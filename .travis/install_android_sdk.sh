#!/bin/bash
# Installs current Android SDK for Travis CI.
source ./.travis/utils.sh

SDK_CACHE_DIR="${HOME}/.cache/android-sdk"
SDK_ZIP="${SDK_CACHE_DIR}/sdk-tools.zip"

if [[ "${TARGET}" != "android" ]]
then
    echo "** Android SDK not required, skipping step."
    exit 0
fi

function download_sdk {
    mkdir -p "${SDK_CACHE_DIR}"

    # NOTE: We deliberately don't actually put this in the Travis cache.
    # Many Travis CI nodes are on GCE anyway, so it takes about 2 seconds to
    # download the SDK from dl.google.com.
    #
    # This is useful for local testing, outside of GCE, where the environment
    # is recycled.
    echo "** Checking for existing ${SDK_ZIP} (SHA256 = ${ANDROID_SDK_SHA256})..."
    if ! echo "${ANDROID_SDK_SHA256}  ${SDK_ZIP}" | sha256sum -c
    then
        echo "** Not found, downloading again..."
        rm -f "${SDK_ZIP}"
        curl -o "${SDK_ZIP}" "${ANDROID_SDK_URL}"

        if ! echo "${ANDROID_SDK_SHA256}  ${SDK_ZIP}" | sha256sum -c
        then
            # download checksum fail
            echo "** Download failed, checksum mismatch!"
            exit 1
        fi
    fi

    mkdir -p "${ANDROID_SDK_ROOT}"
    unzip -qq -n "${SDK_ZIP}" -d "${ANDROID_SDK_ROOT}"
    return $?
}

# Fix up missing repository configuration
mkdir -p $HOME/.android
touch $HOME/.android/repositories.cfg

# Check to see if we already have a working SDK installation, before trying to
# download a new one.
android_install "platform-tools" || download_sdk

# Install required Android SDK components
android_install \
    "platform-tools" \
    "build-tools;${ANDROID_BUILD_TOOLS}" \
    "platforms;android-${ANDROID_API}" \
    "extras;android;m2repository" \
    "extras;google;m2repository"
