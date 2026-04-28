#!/bin/bash
# Reads FIREBASE_TOKEN from local.properties and uploads the release to Firebase App Distribution.

FIREBASE_TOKEN=$(grep -m1 '^FIREBASE_TOKEN=' local.properties | cut -d'=' -f2-)

if [ -z "$FIREBASE_TOKEN" ]; then
    echo "ERROR: FIREBASE_TOKEN not found in local.properties"
    echo "Run 'firebase login:ci' and paste the token into local.properties"
    exit 1
fi

export FIREBASE_TOKEN

./gradlew :app:assembleRelease :app:appDistributionUploadRelease