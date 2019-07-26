#!/bin/bash
# Installs emulators on Travis CI
# Assumes install_android_sdk.sh has already run.
source ./.travis/utils.sh

EMULATOR_TARGET="system-images;android-${EMULATOR_API};${EMULATOR_FLAVOUR};${EMULATOR_ARCH}"
EMULATOR_DIR="${ANDROID_SDK_ROOT}/system-images/android-${EMULATOR_API}/${EMULATOR_FLAVOUR}/${EMULATOR_ARCH}"

if [[ -z "${EMULATOR_API}" ]]
then
    echo "** Emulator not required, skipping step."
    exit 0
fi

android_install \
    "emulator" \
    "${EMULATOR_TARGET}"

check_exists "$ADB"
check_exists "$AVDMANAGER"

echo "** Starting adb-server..."
$ADB start-server

echo "** AVDs targets:"
$AVDMANAGER list

echo "** Platform files:"
ls -laR "${ANDROID_SDK_ROOT}/system-images"

check_exists "$EMULATOR"
check_exists "$EMULATOR_DIR"
check_exists "$EMULATOR_DIR/system.img"

if ! [[ -e "${EMULATOR_DIR}/kernel-ranchu" || -e "${EMULATOR_DIR}/kernel-ranchu-64" ]]
then
    echo "** ERROR: Missing kernel-ranchu(-64) for this target!"
    echo ""
    echo "This system image will not work on Emulator v29+. This is a bug in"
    echo "Android emulator system images."
    echo ""
    echo "More details: https://issuetracker.google.com/issues/134845202"
    echo ""
    echo "CI will now terminate with an error!"
    exit 1
fi

echo "** Creating AVD for Android ${EMULATOR_API} on ${EMULATOR_ARCH}..."
# Do you wish to create a custom hardware profile? [no]
echo "no" | ${AVDMANAGER} create avd -n "emu" -k "${EMULATOR_TARGET}" -f || exit 1
