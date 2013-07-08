#!/bin/bash

USER=`/usr/bin/whoami`
if [ ${USER} != "ldpdapp" ]
then
  echo "ERROR: This script MUST be run as user ldpdapp!"
  exit
fi

HOSTNAME=`/bin/hostname`
if [ ${HOSTNAME} != "bronte.cul.columbia.edu" ]
then
  echo "ERROR: This script MUST be run on bronte.cul.columbia.edu.  If you want to run it somewhere else, please verify that the version of ruby that you want to use is located at /usr/local/bin/ruby and that all required gems are available for the user ldpdapp.  If you're certain that everything is correct, feel free to update this script (including the hostname check for bronte.cul.columbia.edu)."
  exit
fi

LIVE_SITE_SCREENSHOT_DIR='/cul/cul0/lito/vmounts/bronte/opt/nginx/html/hrwa_images/website_screenshots'
BASEDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

COMMAND=$1

if [[ "$COMMAND" == "run" ]]; then
    echo 'Starting...'
elif [[ "$COMMAND" == "force" ]]; then
    echo 'Starting...'
    echo 'Forcing refresh of derivatives...'
else
    echo 'Usage: refresh_hrwa_derivatives.sh [run|force]'
    exit
fi

echo 'Starting HRWA image derivative refresh check...'
echo "BASEDIR: $BASEDIR"
echo "LIVE_SITE_SCREENSHOT_DIR: $LIVE_SITE_SCREENSHOT_DIR"

if [[ -f $BASEDIR/website_screenshots_last_modified ]]; then
    LAST_KNOWN_WEBSITE_SCREENSHOTS_MODIFIED_STRING=`cat $BASEDIR/website_screenshots_last_modified | sed -e 's/^ *//g' -e 's/ *$//g'`
else
    LAST_KNOWN_WEBSITE_SCREENSHOTS_MODIFIED_STRING='0' # No need to create this file now.  It will be created after the entire script runs.
fi

WEBSITE_SCREENSHOTS_ACTUALLY_LAST_MODIFIED=`stat -c %y $BASEDIR/../website_screenshots`

if [[ "$COMMAND" == 'force' || "$LAST_KNOWN_WEBSITE_SCREENSHOTS_MODIFIED_STRING" != "$WEBSITE_SCREENSHOTS_ACTUALLY_LAST_MODIFIED" ]]; then
    
    echo 'Original screenshots directory has been modified!  Updating derivative screenshots...'

    # Delete old derivatives if they exist
    rm -rf $BASEDIR/derivatives/640px/
    rm -rf $BASEDIR/derivatives/140px/

    # Note: Need to call specific version of ruby in cron (because we can't rely on a value from /usr/bin/env)
    # Let's make sure that ruby is available, and where we expect it
    if [[ ! -f /usr/local/bin/ruby ]]; then
        echo 'Error: No ruby found at /usr/local/bin/ruby'
        echo 'Exiting.'
    fi
    /usr/local/bin/ruby $BASEDIR/../image_derivative_generator/generate_image_derivatives.rb $BASEDIR/../website_screenshots/ $BASEDIR/derivatives/640px/ 640 jpeg
    /usr/local/bin/ruby $BASEDIR/../image_derivative_generator/generate_image_derivatives.rb $BASEDIR/../website_screenshots/ $BASEDIR/derivatives/140px/ 140 jpeg

    #Now that we've made the new derivatives, let's replace the ones on the live site

    echo 'Copying derivatives to the live site...'

    # Make sure that we actually have something to copy (in case something goes wrong on the ruby side)
    if [[ ! -d "$BASEDIR/derivatives/640px" || ! -d "$BASEDIR/derivatives/140px" ]]; then
        echo 'Error: No derivatives to copy.'
        echo 'Exiting.'
    fi

    # Delete old 640px-previous and 140px-previous derivatives if they exist, since the current derivatives are about to becomes the XXXpx-previous ones
    rm -rf $LIVE_SITE_SCREENSHOT_DIR/640px-previous
    rm -rf $LIVE_SITE_SCREENSHOT_DIR/140px-previous

    cp -pr $BASEDIR/derivatives/640px $LIVE_SITE_SCREENSHOT_DIR/640px-new
    cp -pr $BASEDIR/derivatives/140px $LIVE_SITE_SCREENSHOT_DIR/140px-new
    mv $LIVE_SITE_SCREENSHOT_DIR/640px $LIVE_SITE_SCREENSHOT_DIR/640px-previous
    mv $LIVE_SITE_SCREENSHOT_DIR/140px $LIVE_SITE_SCREENSHOT_DIR/140px-previous 
    mv $LIVE_SITE_SCREENSHOT_DIR/640px-new $LIVE_SITE_SCREENSHOT_DIR/640px
    mv $LIVE_SITE_SCREENSHOT_DIR/140px-new $LIVE_SITE_SCREENSHOT_DIR/140px

    echo $WEBSITE_SCREENSHOTS_ACTUALLY_LAST_MODIFIED > $BASEDIR/website_screenshots_last_modified
else

    echo 'Screenshots directory has NOT been modified.  No need to update screenshots.'
    
fi

echo 'HRWA screenshot refresh complete!'