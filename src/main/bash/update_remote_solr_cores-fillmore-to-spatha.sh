#!/bin/bash

# This script is used to transfer solr cores from the local server
# (currently: always fillmore.cul.columbia.edu) to other remote
# server (currently: always spatha.cul.columbia.edu)
#
# Expectations:
# - Local and remote servers are running the same version of Solr (4.2 as of
#   this writing)
# - The same user (user "ldpdapp", in this case) is able to ssh from one server to
#   the other without having to type a password (using public/private keys) AND
#   the tomcat app is run by this user AND this user has write access to the
#   solr directories AND this user can start and stop the solr cores.
# - Curl is available on both machines.
#

USER=`/usr/bin/whoami`
if [ "$USER" != "ldpdapp" ]
then
  echo "ERROR: This script MUST be run as user ldpdapp!"
  exit
fi

HOSTNAME=`/bin/hostname`
if [ "$HOSTNAME" != "fillmore.cul.columbia.edu" ]
then
  echo "ERROR: This script MUST be run from fillmore.cul.columbia.edu!"
  exit
fi

LOCAL_SERVER=`/bin/hostname`
LOCAL_SERVER_FILESYSTEM_PATH_TO_SOLR='/opt/solr-4.2'
LOCAL_SERVER_URI_PATH_TO_SOLR='/solr-4.2'
LOCAL_SERVER_SOLR_PORT='8080'

REMOTE_SERVER='spatha.cul.columbia.edu'
REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR='/opt/solr-4.2-hrwa'
REMOTE_SERVER_URI_PATH_TO_SOLR='/solr-4.2-hrwa'
REMOTE_SERVER_SOLR_PORT='8081'

ASF_CORE_NAME='hrwa-asf'
FSF_CORE_NAME='hrwa-fsf'

if ping -c1 $REMOTE_SERVER > /dev/null; then
    echo "Test passed: Able to connect to remote server: $REMOTE_SERVER"
else
    echo "Error: Unable to connect to remote server: $REMOTE_SERVER"
    exit 1
fi

if [ "$REMOTE_SERVER" == "$LOCAL_SERVER" ]
then
  echo "ERROR: The local and remote servers cannot be the same."
  exit
fi

echo "Local server: $LOCAL_SERVER"
echo "Remote server: $REMOTE_SERVER"

### UNLOAD LOCAL HRWA CORES ###

echo 'Unloading local HRWA cores...'
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=UNLOAD&core=$FSF_CORE_NAME"
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=UNLOAD&core=$ASF_CORE_NAME"
sleep 20 # wait for current requests to complete
echo 'Done unloading local HRWA cores.'

# COPY CORES OVER

echo "Copying FSF solr core from $LOCAL_SERVER to $REMOTE_SERVER..."
scp -pr $LOCAL_SERVER_FILESYSTEM_PATH_TO_SOLR/$FSF_CORE_NAME $REMOTE_SERVER:$REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/new-$FSF_CORE_NAME
echo 'FSF core copy complete.'

echo "Copying ASF solr core from $LOCAL_SERVER to $REMOTE_SERVER..."
scp -pr $LOCAL_SERVER_FILESYSTEM_PATH_TO_SOLR/$ASF_CORE_NAME $REMOTE_SERVER:$REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/new-$ASF_CORE_NAME
echo 'ASF core copy complete.'

### RE-LOAD LOCAL HRWA CORES ##

echo 'Re-adding and reloading local HRWA solr cores...'
# Behind the scenes, this is actually a CREATE and then RELOAD.  Note: RELOAD is recommended for Solr 4.2, but required in Solr 4.3.
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=CREATE&name=$FSF_CORE_NAME&instanceDir=$FSF_CORE_NAME&loadOnStartup=true&transient=false"
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=RELOAD&core=$FSF_CORE_NAME"
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=CREATE&name=$ASF_CORE_NAME&instanceDir=$ASF_CORE_NAME/&loadOnStartup=true&transient=false"
curl "http://$LOCAL_SERVER:$LOCAL_SERVER_SOLR_PORT$LOCAL_SERVER_URI_PATH_TO_SOLR/admin/cores?action=RELOAD&core=$ASF_CORE_NAME"
echo 'Local HRWA solr cores have been re-added and reloaded.'

### UNLOAD REMOTE HRWA CORES ###

echo 'Unloading remote HRWA cores...'
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=UNLOAD&core=$FSF_CORE_NAME"
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=UNLOAD&core=$ASF_CORE_NAME"
sleep 20 # wait for current requests to complete
echo 'Done unloading remote HRWA cores.'

echo 'Swapping new cores on remote server...'

# SWAP IN NEW CORES, first deleting any remnants of the last transfer (backup core files)

ssh $REMOTE_SERVER "rm -rf $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/old-$FSF_CORE_NAME; mv $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/$FSF_CORE_NAME $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/old-$FSF_CORE_NAME; mv $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/new-$FSF_CORE_NAME $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/$FSF_CORE_NAME"
ssh $REMOTE_SERVER "rm -rf $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/old-$ASF_CORE_NAME; mv $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/$ASF_CORE_NAME $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/old-$ASF_CORE_NAME; mv $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/new-$ASF_CORE_NAME $REMOTE_SERVER_FILESYSTEM_PATH_TO_SOLR/$ASF_CORE_NAME"

echo 'Done swapping core directories on the filesystem.'

echo 'Re-adding and reloading remote HRWA solr cores...'
# Behind the scenes, this is actually a CREATE and then RELOAD.  Note: RELOAD is recommended for Solr 4.2, but required in Solr 4.3.
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=CREATE&name=$FSF_CORE_NAME&instanceDir=$FSF_CORE_NAME&loadOnStartup=true&transient=false"
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=RELOAD&core=$FSF_CORE_NAME"
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=CREATE&name=$ASF_CORE_NAME&instanceDir=$ASF_CORE_NAME&loadOnStartup=true&transient=false"
curl "http://$REMOTE_SERVER:$REMOTE_SERVER_SOLR_PORT$REMOTE_SERVER_URI_PATH_TO_SOLR/admin/cores?action=RELOAD&core=$ASF_CORE_NAME"
echo 'Remote HRWA solr cores have been re-added and reloaded.'

# Trigger solr cache refresh by visiting the search URLs (using very long timeouts because the first search can take a while)
curl --connect-timeout 300 --max-time 300 "http://hrwa.cul.columbia.edu/search?utf8=%E2%9C%93&search_type=find_site&q="
curl --connect-timeout 300 --max-time 300 "http://hrwa.cul.columbia.edu/search?utf8=%E2%9C%93&search_type=archive&q=" # Generally, the archive search is the only one that takes a while the first time

echo 'Core update complete!'