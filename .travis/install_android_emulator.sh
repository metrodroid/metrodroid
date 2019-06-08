#!/bin/bash
# Installs emulators on Travis CI
# Assumes install_android_sdk.sh has already run.

SDK_DIR="${HOME}/android-sdk"
ADB="${SDK_DIR}/platform-tools/bin/adb"
AVDMANAGER="${SDK_DIR}/tools/bin/avdmanager"
SDKMANAGER="${SDK_DIR}/tools/bin/sdkmanager"
EMULATOR="${SDK_DIR}/emulator/emulator"

EMULATOR_TARGET="system-images;android-${EMULATOR_API};default;${EMULATOR_ARCH}"

echo y | ${SDKMANAGER} "emulator" > /dev/null
echo y | ${SDKMANAGER} "platforms;android-${EMULATOR_API}" > /dev/null
echo y | ${SDKMANAGER} "${EMULATOR_TARGET}" > /dev/null

echo "AVDs targets:"
${AVDMANAGER} list target

echo "Creating AVD for Android ${EMULATOR_API} on ${EMULATOR_ARCH}..."
${AVDMANAGER} create avd -n emu -k "${EMULATOR_TARGET}" -f

echo "Starting emulator in background..."
${EMULATOR} -avd emu -no-skin -no-window &

echo "Waiting for emulator..."
./.travis/android-wait-for-emulator

${ADB} shell input keyevent 82 &
