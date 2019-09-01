#!/bin/bash
# Run test on metrodroid-cli
# This is intended to only check that our CLI still works. Complete card handling tests are in
# commonTest.

# Exit on any non-zero return, verbose output
set -ev
TEST_OUTPUT_DIR=$(mktemp -d)

# Runs Metrodroid, immediately exiting if it fails.
function check_metrodroid {
    ./jvmcli/build/install/jvmcli-shadow/bin/metrodroid-cli "$@" || exit 1
}

# Checks that we got something looking like help output.
function check_help {
    check_metrodroid "$@" | grep -q "Commands:"
}

# Checking help output
check_help
check_help -h
check_help --help

# Supported card count
check_metrodroid supported | grep -ic "card name"


# Test opal-transit-litter.xml
F="./src/commonTest/assets/opal/opal-transit-litter.xml"

# Should get no output here
UNRECOGNIZED="$(check_metrodroid unrecognized $F)"
[[ -z "${UNRECOGNIZED}" ]]

# Check identify
IDENTIFY_FN="${TEST_OUTPUT_DIR}/identify"
check_metrodroid identify "$F" > "$IDENTIFY_FN"

# Show output for debug
cat "$IDENTIFY_FN"

grep -q "Opal" "$IDENTIFY_FN"
grep -q "3085 2200 7856 2242" "$IDENTIFY_FN"

# Check parse
PARSE_FN="${TEST_OUTPUT_DIR}/parse"
check_metrodroid parse "$F" > "$PARSE_FN"

# Show output for debug
cat "$PARSE_FN"

grep -q "Opal" "$PARSE_FN"
grep -q "3085 2200 7856 2242" "$PARSE_FN"
grep -q "Journey completed (distance fare)" "$PARSE_FN"

rm -fr "${TEST_OUTPUT_DIR}"

# Finished!
