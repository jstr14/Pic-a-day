#!/usr/bin/env python3
"""
PicADay – copy personal diary photos to a shared album.

Reads all entries from  users/{userId}/entries
and writes them into    albums/{albumId}/entries

No images are re-uploaded; only Firestore metadata is copied.
Photos already present in the album (matched by URL) are skipped.

Requirements:
    pip install firebase-admin

Usage:
    python copy_to_album.py \
        --user-id  YOUR_FIREBASE_UID \
        --album-id TARGET_ALBUM_ID \
        --service-account /path/to/service-account.json
"""

import argparse
import sys
from collections import defaultdict

import firebase_admin
from firebase_admin import credentials, firestore
from google.cloud.firestore_v1 import ArrayUnion


def main():
    parser = argparse.ArgumentParser(
        description="Copy personal diary photos to a PicADay shared album."
    )
    parser.add_argument(
        "--user-id", required=True,
        help="Firebase UID of the source user.",
    )
    parser.add_argument(
        "--album-id", required=True,
        help="Firestore document ID of the target album.",
    )
    parser.add_argument(
        "--service-account", default="service-account.json",
        help="Path to Firebase service account JSON (default: service-account.json).",
    )
    args = parser.parse_args()

    try:
        cred = credentials.Certificate(args.service_account)
    except Exception as e:
        print(f"ERROR loading service account: {e}")
        sys.exit(1)

    firebase_admin.initialize_app(cred)
    db = firestore.client()

    user_id = args.user_id
    album_id = args.album_id

    # ------------------------------------------------------------------
    # 1. Resolve user display name
    # ------------------------------------------------------------------
    user_doc = db.collection("users").document(user_id).get()
    if user_doc.exists:
        user_data = user_doc.to_dict() or {}
        display_name = user_data.get("displayName") or ""
    else:
        display_name = ""
        print(f"WARNING: No users/{user_id} document found. 'uploadedByName' will be empty.")

    print(f"User : {user_id}" + (f"  ({display_name})" if display_name else ""))

    # ------------------------------------------------------------------
    # 2. Verify album exists
    # ------------------------------------------------------------------
    album_doc = db.collection("albums").document(album_id).get()
    if not album_doc.exists:
        print(f"ERROR: Album '{album_id}' does not exist.")
        sys.exit(1)

    album_name = (album_doc.to_dict() or {}).get("name", album_id)
    print(f"Album: {album_name}  ({album_id})")

    # ------------------------------------------------------------------
    # 3. Load existing album entries → build set of known URLs per date
    #    (used to skip photos that are already in the album)
    # ------------------------------------------------------------------
    print("\nLoading existing album entries...")
    existing_urls: dict[str, set[str]] = defaultdict(set)
    for doc in db.collection("albums").document(album_id).collection("entries").stream():
        for photo in (doc.to_dict() or {}).get("photos", []):
            url = photo.get("url")
            if url:
                existing_urls[doc.id].add(url)

    # ------------------------------------------------------------------
    # 4. Read personal diary entries and collect photos to copy
    # ------------------------------------------------------------------
    print("Loading personal diary entries...")
    new_photos_by_date: dict[str, list[dict]] = defaultdict(list)

    for doc in db.collection("users").document(user_id).collection("entries").stream():
        data = doc.to_dict() or {}
        date_str = data.get("date") or doc.id
        known_urls = existing_urls.get(date_str, set())

        for photo in data.get("photos", []):
            url = photo.get("url")
            if not url or url in known_urls:
                continue  # skip missing or already-present photos

            entry: dict = {"url": url, "uploadedBy": user_id}
            if display_name:
                entry["uploadedByName"] = display_name
            if photo.get("time"):
                entry["time"] = photo["time"]
            if photo.get("lat") is not None:
                entry["lat"] = photo["lat"]
            if photo.get("lon") is not None:
                entry["lon"] = photo["lon"]

            new_photos_by_date[date_str].append(entry)

    # ------------------------------------------------------------------
    # 5. Write to album entries
    # ------------------------------------------------------------------
    if not new_photos_by_date:
        print("\nNothing to copy — all photos are already in the album.")
        return

    total = sum(len(v) for v in new_photos_by_date.values())
    print(f"\nCopying {total} photo(s) across {len(new_photos_by_date)} date(s)...\n")

    copied = failed = 0
    for date_str in sorted(new_photos_by_date):
        photos = new_photos_by_date[date_str]
        year_month = date_str[:7]  # "YYYY-MM"
        print(f"  {date_str}:  {len(photos)} photo(s)")
        try:
            db.collection("albums").document(album_id) \
              .collection("entries").document(date_str) \
              .set(
                  {
                      "date": date_str,
                      "yearMonth": year_month,
                      "photos": ArrayUnion(photos),
                  },
                  merge=True,
              )
            copied += len(photos)
        except Exception as e:
            print(f"    ERROR: {e}")
            failed += len(photos)

    print(f"\n{'=' * 40}")
    print(f"Done.  {copied} copied,  {failed} failed.")


if __name__ == "__main__":
    main()