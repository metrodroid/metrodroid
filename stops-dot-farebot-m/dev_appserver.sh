#!/bin/sh
# dev_appserver.py wrapper for gcloud
SCRIPT="dev_appserver.py"
GCLOUD="`which gcloud`"

if [ "x$GCLOUD" = "x" ] ; then
	echo 'gcloud is not in your PATH.  Cannot continue.'
	exit 1
fi

# Now locate the root of gcloud's installation
GCLOUD_PATH="`dirname $(dirname ${GCLOUD})`"

# Now look for script
SCRIPT_PATH="${GCLOUD_PATH}/platform/google_appengine/${SCRIPT}"

# Check that this is a real executable
if [ ! -x "${SCRIPT_PATH}" ] ; then
	echo "${SCRIPT} seems to be missing. Make sure you have gcloud App Engine components available."
	echo "Cannot continue."
	exit 1
fi

${SCRIPT_PATH} $@ app.yaml

