# PicADay Image Upload Script

Bulk-uploads images from your laptop to Firebase, replicating exactly what the app does when a user picks a photo:

1. Compresses the image to max 1080px on the longest side, JPEG quality 80, fixing EXIF rotation.
2. Extracts date, time, latitude, and longitude from EXIF metadata.
3. Uploads the compressed image to Firebase Storage at `users/{uid}/photos/{fileName}`.
4. Creates or updates the Firestore entry at `users/{uid}/entries/{date}`.

## Requirements

```bash
pip install firebase-admin Pillow
```

Supported image formats: `.jpg`, `.jpeg`, `.png`, `.webp`

## Setup

### 1. Get a service account key

Go to **Firebase Console → Project Settings → Service Accounts → Generate new private key** and save the downloaded file as `service-account.json` inside the `scripts/` folder (or anywhere you prefer).

> Keep this file private — do not commit it to version control.

### 2. Get your Firebase user ID

Go to **Firebase Console → Authentication**, find your account, and copy the **UID**.

## Usage

```bash
python upload_images.py \
  --images-dir /path/to/photos \
  --user-id YOUR_FIREBASE_UID \
  --service-account /path/to/service-account.json
```

### Options

| Option | Required | Description |
|---|---|---|
| `--images-dir` | Yes | Folder containing images to upload |
| `--user-id` | Yes | Firebase UID of the target user account |
| `--service-account` | No | Path to service account JSON (default: `service-account.json`) |
| `--date` | No | Override date for all images as `YYYY-MM-DD`. Use when images have no EXIF date |

### Examples

**Phone photos with EXIF dates:**
```bash
python upload_images.py \
  --images-dir ~/Desktop/vacation_photos \
  --user-id abc123yourUID
```

**Screenshots or downloads without EXIF dates:**
```bash
python upload_images.py \
  --images-dir ~/Desktop/screenshots \
  --user-id abc123yourUID \
  --date 2024-07-15
```

**Custom service account path:**
```bash
python upload_images.py \
  --images-dir ~/Desktop/photos \
  --user-id abc123yourUID \
  --service-account ~/keys/picaday-service-account.json
```