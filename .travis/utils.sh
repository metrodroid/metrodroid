#!/bin/bash

ADB="${ANDROID_SDK_ROOT}/platform-tools/adb"
AVDMANAGER="${ANDROID_SDK_ROOT}/tools/bin/avdmanager"
SDKMANAGER="${ANDROID_SDK_ROOT}/tools/bin/sdkmanager"
EMULATOR="${ANDROID_SDK_ROOT}/emulator/emulator"

function android_install {
    echo "** Installing SDK package(s):" "$@"
    echo y | ${SDKMANAGER} "$@" > /dev/null
    return $?
}
