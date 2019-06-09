#!/bin/bash
# Installs emulators on Travis CI
# Assumes install_android_sdk.sh has already run.
source ./.travis/utils.sh

EMULATOR_TARGET="system-images;android-${EMULATOR_API};default;${EMULATOR_ARCH}"
EMULATOR_DIR="${ANDROID_SDK_ROOT}/system-images/android-${EMULATOR_API}/default/${EMULATOR_ARCH}"

android_install \
    "platform-tools" \
    "emulator" \
    "${EMULATOR_TARGET}"

echo "** Starting adb-server..."
$ADB start-server

echo "** AVDs targets:"
${AVDMANAGER} list

echo "** Possibly fixing broken images..."
if ! [[ -e "${EMULATOR_DIR}/kernel-ranchu" ]]
then
    EMULATOR_ARGS="-engine classic"
else
    EMULATOR_ARGS=""
fi

echo "** Platform files:"
ls -laR "${ANDROID_SDK_ROOT}/system-images"

echo "** Creating AVD for Android ${EMULATOR_API} on ${EMULATOR_ARCH}..."
# Do you wish to create a custom hardware profile? [no]
echo "no" | ${AVDMANAGER} create avd -n emu -k "${EMULATOR_TARGET}" -f

echo "** Starting emulator in background..."
${EMULATOR} -avd emu -no-skin -no-window ${EMULATOR_ARGS} &

echo "** Waiting for emulator..."
./.travis/android-wait-for-emulator

${ADB} shell input keyevent 82 &
