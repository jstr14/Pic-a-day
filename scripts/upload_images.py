#!/usr/bin/env python3
"""
PicADay bulk image uploader.

Replicates what the Android app does when a user picks a photo:
  1. Compress image to max 1080px, JPEG quality 80, fix EXIF rotation.
  2. Extract EXIF date (YYYY-MM-DD), time (HH:mm), lat, lon.
  3. Upload compressed image to Firebase Storage: users/{uid}/photos/{fileName}
  4. Merge photo entry into Firestore: users/{uid}/entries/{date}

Requirements:
    pip install firebase-admin Pillow

Usage:
    python upload_images.py \\
        --images-dir /path/to/photos \\
        --user-id YOUR_FIREBASE_UID \\
        --service-account /path/to/service-account.json

    # If images have no EXIF date, supply one explicitly:
    python upload_images.py ... --date 2024-07-15
"""

import argparse
import io
import sys
import time
import uuid
from datetime import datetime
from pathlib import Path

from PIL import Image, ExifTags

import firebase_admin
from firebase_admin import credentials, storage, firestore
from google.cloud.firestore_v1 import ArrayUnion

# ---------------------------------------------------------------------------
# Constants — match the Android ImageHelper exactly
# ---------------------------------------------------------------------------
MAX_SIZE = 1080
JPEG_QUALITY = 80
SUPPORTED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".webp"}

# EXIF tag IDs
_TAG_MAP = {name: tag for tag, name in ExifTags.TAGS.items()}
ORIENTATION_TAG = _TAG_MAP["Orientation"]
DATETIME_ORIGINAL_TAG = _TAG_MAP["DateTimeOriginal"]
DATETIME_TAG = _TAG_MAP["DateTime"]
GPS_TAG = _TAG_MAP["GPSInfo"]


# ---------------------------------------------------------------------------
# Image processing
# ---------------------------------------------------------------------------

def _fix_orientation(img: Image.Image, raw_exif: dict) -> Image.Image:
    """Rotate image to match EXIF orientation — same as Android rotateImageIfRequired."""
    orientation = raw_exif.get(ORIENTATION_TAG)
    # EXIF values: 3=180°, 6=90° CW (needs 270° rotation), 8=270° CW (needs 90° rotation)
    rotations = {3: 180, 6: 270, 8: 90}
    degrees = rotations.get(orientation)
    if degrees:
        img = img.rotate(degrees, expand=True)
    return img


def compress_image(image_path: Path) -> tuple[bytes, dict]:
    """
    Compress image matching Android ImageHelper.compressImage():
      - Fix rotation from EXIF orientation
      - Scale to max 1080px on longest side
      - JPEG quality 80

    Returns (compressed_bytes, raw_exif_dict).
    """
    with Image.open(image_path) as img:
        try:
            raw_exif = img._getexif() or {}
        except (AttributeError, Exception):
            raw_exif = {}

        img = img.convert("RGB")
        img = _fix_orientation(img, raw_exif)

        w, h = img.size
        if max(w, h) > MAX_SIZE:
            scale = MAX_SIZE / max(w, h)
            img = img.resize((int(w * scale), int(h * scale)), Image.LANCZOS)

        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=JPEG_QUALITY)
        return buf.getvalue(), raw_exif


# ---------------------------------------------------------------------------
# EXIF metadata extraction
# ---------------------------------------------------------------------------

def _gps_to_degrees(value) -> float:
    """Convert EXIF GPS IFDRational tuple to decimal degrees."""
    d, m, s = value
    return float(d) + float(m) / 60 + float(s) / 3600


def extract_exif_metadata(raw_exif: dict) -> tuple[str | None, str | None, float | None, float | None]:
    """
    Match Android ProcessImageUseCase.extractExifData().
    Returns (date "YYYY-MM-DD", time "HH:mm", lat, lon).
    """
    date_str = time_str = lat = lon = None

    # Date / time — prefer DateTimeOriginal, fall back to DateTime
    dt_raw = raw_exif.get(DATETIME_ORIGINAL_TAG) or raw_exif.get(DATETIME_TAG)
    if dt_raw:
        try:
            dt = datetime.strptime(dt_raw, "%Y:%m:%d %H:%M:%S")
            date_str = dt.strftime("%Y-%m-%d")
            time_str = dt.strftime("%H:%M")
        except ValueError:
            pass

    # GPS
    gps_raw = raw_exif.get(GPS_TAG)
    if gps_raw:
        gps = {ExifTags.GPSTAGS.get(t, t): v for t, v in gps_raw.items()}
        try:
            lat = _gps_to_degrees(gps["GPSLatitude"])
            if gps.get("GPSLatitudeRef", "N") != "N":
                lat = -lat
        except (KeyError, TypeError, ZeroDivisionError):
            lat = None
        try:
            lon = _gps_to_degrees(gps["GPSLongitude"])
            if gps.get("GPSLongitudeRef", "E") != "E":
                lon = -lon
        except (KeyError, TypeError, ZeroDivisionError):
            lon = None

    return date_str, time_str, lat, lon


# ---------------------------------------------------------------------------
# Firebase upload
# ---------------------------------------------------------------------------

def upload_image(
    image_path: Path,
    user_id: str,
    bucket,
    db,
    date_override: str | None,
) -> bool:
    print(f"  Processing: {image_path.name}")

    # Compress
    try:
        compressed_bytes, raw_exif = compress_image(image_path)
    except Exception as e:
        print(f"  ERROR compressing: {e}")
        return False

    original_kb = image_path.stat().st_size // 1024
    compressed_kb = len(compressed_bytes) // 1024
    print(f"  Size: {original_kb}KB → {compressed_kb}KB")

    # Metadata
    date_str, time_str, lat, lon = extract_exif_metadata(raw_exif)
    if date_override:
        date_str = date_override

    if not date_str:
        print("  SKIP: No date in EXIF. Pass --date YYYY-MM-DD to override.")
        return False

    year_month = date_str[:7]  # "YYYY-MM"

    # --- Upload to Firebase Storage ---
    # Path: users/{userId}/photos/{fileName}  (same as Android FirebaseStorageRepository)
    file_name = f"photo_{int(time.time() * 1000)}_{uuid.uuid4()}.jpg"
    blob_path = f"users/{user_id}/photos/{file_name}"

    try:
        blob = bucket.blob(blob_path)

        # Assign a Firebase download token so the URL works identically to
        # URLs generated by the Android Firebase SDK.
        download_token = str(uuid.uuid4())
        blob.metadata = {"firebaseStorageDownloadTokens": download_token}

        blob.upload_from_string(compressed_bytes, content_type="image/jpeg")
        blob.patch()  # persist the metadata / token

        encoded_path = blob_path.replace("/", "%2F")
        download_url = (
            f"https://firebasestorage.googleapis.com/v0/b/{bucket.name}"
            f"/o/{encoded_path}?alt=media&token={download_token}"
        )
    except Exception as e:
        print(f"  ERROR uploading to Storage: {e}")
        return False

    print(f"  Storage: {blob_path}")

    # --- Write to Firestore ---
    # users/{userId}/entries/{date}  (same as Android FirebaseImageRepositoryImpl.addPhotoToDate)
    photo_map: dict = {"url": download_url}
    if time_str:
        photo_map["time"] = time_str
    if lat is not None:
        photo_map["lat"] = lat
    if lon is not None:
        photo_map["lon"] = lon

    doc_ref = (
        db.collection("users")
        .document(user_id)
        .collection("entries")
        .document(date_str)
    )
    try:
        doc_ref.set(
            {
                "date": date_str,
                "yearMonth": year_month,
                "photos": ArrayUnion([photo_map]),
            },
            merge=True,
        )
    except Exception as e:
        print(f"  ERROR writing to Firestore: {e}")
        return False

    meta_parts = [f"date={date_str}"]
    if time_str:
        meta_parts.append(f"time={time_str}")
    if lat is not None:
        meta_parts.append(f"lat={lat:.5f}, lon={lon:.5f}")
    print(f"  Firestore: {', '.join(meta_parts)}")
    return True


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="Bulk-upload images to PicADay Firebase (Storage + Firestore)."
    )
    parser.add_argument(
        "--images-dir", required=True,
        help="Folder containing images to upload.",
    )
    parser.add_argument(
        "--user-id", required=True,
        help="Firebase UID of the target user account.",
    )
    parser.add_argument(
        "--service-account", default="service-account.json",
        help="Path to Firebase service account JSON (default: service-account.json).",
    )
    parser.add_argument(
        "--date", default=None,
        help="Override date for all images as YYYY-MM-DD. Use when images lack EXIF dates.",
    )
    args = parser.parse_args()

    images_dir = Path(args.images_dir)
    if not images_dir.is_dir():
        print(f"ERROR: '{images_dir}' is not a directory.")
        sys.exit(1)

    service_account_path = Path(args.service_account)
    if not service_account_path.exists():
        print(f"ERROR: Service account file not found: {service_account_path}")
        print("Get it from Firebase Console > Project Settings > Service Accounts > Generate new private key")
        sys.exit(1)

    # Init Firebase Admin SDK
    cred = credentials.Certificate(str(service_account_path))
    firebase_admin.initialize_app(cred, {
        "storageBucket": "pic-a-day-a869f.firebasestorage.app"
    })
    bucket = storage.bucket()
    db = firestore.client()

    # Collect images (sorted by name for predictable order)
    image_files = sorted([
        f for f in images_dir.iterdir()
        if f.is_file() and f.suffix.lower() in SUPPORTED_EXTENSIONS and not f.name.startswith("._")
    ])

    if not image_files:
        print(f"No supported images found in '{images_dir}'. Supported: {', '.join(SUPPORTED_EXTENSIONS)}")
        sys.exit(0)

    print(f"Found {len(image_files)} image(s) → uploading for user '{args.user_id}'\n")

    success = failed = 0
    for image_path in image_files:
        ok = upload_image(image_path, args.user_id, bucket, db, date_override=args.date)
        if ok:
            success += 1
        else:
            failed += 1
        print()

    print(f"{'='*40}")
    print(f"Done.  {success} uploaded,  {failed} failed.")


if __name__ == "__main__":
    main()