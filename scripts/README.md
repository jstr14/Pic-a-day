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

---

# PicADay Copy to Album Script

Copies photos from a user's personal diary to a shared album **without re-uploading any images**. Only Firestore metadata is written. Photos already present in the album (matched by URL) are skipped automatically.

Useful when you have photos in your personal diary and want to share them with someone via a shared album.

## Requirements

```bash
pip install firebase-admin
```

No image processing library needed — no files are touched.

## Setup

### 1. Get a service account key

Same as above: **Firebase Console → Project Settings → Service Accounts → Generate new private key**.

If you already have `service-account.json` from the upload script, you can reuse it.

### 2. Get your Firebase user ID

**Firebase Console → Authentication → Users** → find your account → copy the **UID** column.

### 3. Get the album ID

**Firebase Console → Firestore → albums collection** → open the album document → copy the **Document ID** from the URL or the document header.

Alternatively, open the app, navigate to the album, and check Firestore for the document whose `name` field matches.

### 4. Create the album first

The album must already exist in the app before running the script. Create it from the Albums screen, then copy its ID as described above.

> The person you want to share with does **not** need to have signed in yet — you can run the script before inviting them. Once they sign in and you invite them from the Members tab, they will immediately see all the copied photos.

## Usage

```bash
python copy_to_album.py \
  --user-id YOUR_FIREBASE_UID \
  --album-id TARGET_ALBUM_ID \
  --service-account /path/to/service-account.json
```

### Options

| Option | Required | Description |
|---|---|---|
| `--user-id` | Yes | Firebase UID of the user whose diary photos will be copied |
| `--album-id` | Yes | Firestore document ID of the target shared album |
| `--service-account` | No | Path to service account JSON (default: `service-account.json`) |

### Example

```bash
python copy_to_album.py \
  --user-id abc123yourUID \
  --album-id xYz789albumID \
  --service-account service-account.json
```

Sample output:

```
User : abc123yourUID  (Ada Lovelace)
Album: Summer Trip  (xYz789albumID)

Loading existing album entries...
Loading personal diary entries...

Copying 47 photo(s) across 12 date(s)...

  2024-07-01:  3 photo(s)
  2024-07-02:  5 photo(s)
  2024-07-04:  8 photo(s)
  ...

========================================
Done.  47 copied,  0 failed.
```

## Notes

- Running the script multiple times is safe — duplicate photos are detected by URL and skipped.
- Photos are tagged with your `uploadedBy` UID and display name, so they appear correctly attributed in the album.
- Your personal diary is never modified.
- The script does not send any invitation — you still need to invite the person from the app once they have signed in.