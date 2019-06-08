#!/bin/bash
# Installs emulators on Travis CI
# Assumes install_android_sdk.sh has already run.
source ./.travis/utils.sh

EMULATOR_TARGET="system-images;android-${EMULATOR_API};default;${EMULATOR_ARCH}"

android_install \
    "emulator" \
    "platforms;android-${EMULATOR_API}" \
    "${EMULATOR_TARGET}"

echo "** AVDs targets:"
${AVDMANAGER} list

echo "** Creating AVD for Android ${EMULATOR_API} on ${EMULATOR_ARCH}..."
# Do you wish to create a custom hardware profile? [no]
echo "no" | ${AVDMANAGER} create avd -n emu -k "${EMULATOR_TARGET}" -f

echo "** Starting emulator in background..."
${EMULATOR} -avd emu -no-skin -no-window &

echo "** Waiting for emulator..."
./.travis/android-wait-for-emulator

${ADB} shell input keyevent 82 &
