#!/bin/bash

ADB="${ANDROID_HOME}/platform-tools/bin/adb"
AVDMANAGER="${ANDROID_HOME}/tools/bin/avdmanager"
SDKMANAGER="${ANDROID_HOME}/tools/bin/sdkmanager"
EMULATOR="${ANDROID_HOME}/emulator/emulator"

function android_install {
    echo "** Installing SDK package(s):" "$@"
    echo y | ${SDKMANAGER} "$@" > /dev/null
    return $?
}
