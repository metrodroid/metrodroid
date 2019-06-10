#!/bin/bash

function check_exists {
    if ! [[ -e "$1" ]]
    then
        echo "** ERROR: Missing expected file: $1"
        echo ""
        echo "The CI process has failed to install all required packages."
        echo ""
        echo "This can be caused by transient network issues. If it persistently"
        echo "fails, then either the packages are broken or there is something"
        echo "wrong with the CI configuration."
        echo ""
        echo "CI will terminate now with an error!"
        exit 1
    fi
}

function android_install {
    echo "** Installing SDK package(s):" "$@"
    echo y | ${SDKMANAGER} "$@" > /dev/null
    return $?
}
