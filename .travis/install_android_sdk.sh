#!/bin/bash
# Installs current Android SDK for Travis CI

SDK_URL="https://dl.google.com/android/repository/sdk-tools-linux-4333796.zip"
SDK_SHA256="92ffee5a1d98d856634e8b71132e8a95d96c83a63fde1099be3d86df3106def9"

SDK_CACHE_DIR="${HOME}/.cache/android-sdk"
SDK_ZIP="${SDK_CACHE_DIR}/sdk-tools.zip"
SDKMANAGER="${ANDROID_HOME}/tools/bin/sdkmanager"

mkdir -p "${SDK_CACHE_DIR}"

if ! echo "${SDK_SHA256}  ${SDK_ZIP}" | sha256sum -c
then
    # Need to download again...
    rm -f "${SDK_ZIP}"
    curl -o "${SDK_ZIP}" "${SDK_URL}"

    if ! echo "${SDK_SHA256}  ${SDK_ZIP}" | sha256sum -c
    then
        # download checksum fail
        echo "Download failed, checksum mismatch!"
        exit 1
    fi
fi

mkdir "${ANDROID_HOME}"
unzip -qq -n "${SDK_ZIP}" -d "${ANDROID_HOME}"

# Install/update Android SDK components
echo y | ${SDKMANAGER} "platform-tools" > /dev/null
echo y | ${SDKMANAGER} "build-tools;${ANDROID_BUILD_TOOLS}" > /dev/null
echo y | ${SDKMANAGER} "platforms;android-${ANDROID_API}" > /dev/null

echo y | ${SDKMANAGER} "extras;android;m2repository" > /dev/null
echo y | ${SDKMANAGER} "extras;google;m2repository" > /dev/null
