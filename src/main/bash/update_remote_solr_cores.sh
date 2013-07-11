#!/bin/bash

# This script is used to transfer solr cores from the local server
# (currently: always fillmore.cul.columbia.edu) to other remote
# servers with the same solr setup.
#
# Expectations:
# - Local and remote servers have same tomcat path (and that /opt/tomcat/bin
#   contains startup.sh and shutdown.sh)
# - Local and remote servers are running the same version of Solr (4.2 as of
#   this writing), and that their solr core directories are located at a path
#   similar /opt/solr-4.2/hrwa-fsf and /opt/solr-4.2/hrwa-asf
# - The same user (user "tomcat", in this case) is able to ssh from one server to
#   the other without having to type a password (using public/private keys) AND
#   the tomcat app is run by this user AND this user has write access to the
#   solr directories AND this user can start and stop the tomcat app.
# - Curl is available on both machines.
#

USER=`/usr/bin/whoami`
if [ ${USER} != "tomcat" ]
then
  echo "ERROR: This script MUST be run as user tomcat!"
  exit
fi

HOSTNAME=`/bin/hostname`
if [ ${HOSTNAME} != "fillmore.cul.columbia.edu" ]
then
  echo "ERROR: This script MUST be run from fillmore.cul.columbia.edu!"
  exit
fi

###############################################################
# FUNCTIONS
###############################################################
function tomcatProcessCount {
    echo `ps aux | grep '/opt/tomca[t]' | grep 'java' | wc -l`
}
###############################################################

if [[ "$1" == '' || "$1" == '--help' ]]; then
    echo 'Usage: update_remote_server_cores.sh <remote server url>'
    exit 0
fi

if ping -c1 $1 > /dev/null; then
    REMOTE_SERVER=$1
else
    echo "Error: Unable to connect to remote server: $1"
    exit 1
fi

LOCAL_SERVER=`/bin/hostname`

echo "Local server: $LOCAL_SERVER"
echo "Remote server: $REMOTE_SERVER"

### SHUT DOWN LOCAL TOMCAT ###

#Verify that tomcat is running:

if [[ $(tomcatProcessCount) != '1' ]]; then
    echo 'Expected local tomcat to be running, but did not find expected tomcat process from `ps aux` output. Exiting.'
    exit 1
fi

echo 'Local tomcat is running.'

# Shutdown Tomcat
echo 'Shutting down local tomcat...'

/opt/tomcat/bin/shutdown.sh

sleep 10

if [[ $(tomcatProcessCount) != '0' ]]; then
    echo 'Tomcat is still running, but we want it to stop.  Waiting for 10 more seconds...'
    sleep 10
    if [[ $(tomcatProcessCount) != '0' ]]; then
        echo 'Tomcat is still running after 15 seconds.  This could indicate a problem.  Exiting.'
        exit 1
    fi
fi

echo 'Tomcat has been shut down successfully.'

# COPY CORES OVER

echo "Copying FSF solr core from $LOCAL_SERVER to $REMOTE_SERVER..."
scp -pr /opt/solr-4.2/hrwa-fsf $REMOTE_SERVER:/opt/solr-4.2/new-hrwa-fsf
echo 'FSF core copy complete.'

echo "Copying ASF solr core from $LOCAL_SERVER to $REMOTE_SERVER..."
scp -pr /opt/solr-4.2/hrwa-asf $REMOTE_SERVER:/opt/solr-4.2/new-hrwa-asf
echo 'ASF core copy complete.'

### START UP LOCAL TOMCAT AGAIN ##

echo 'Starting up local tomcat once again...'
/opt/tomcat/bin/startup.sh
sleep 5
echo 'Local tomcat has started'

### SHUT DOWN REMOTE TOMCAT ###

echo 'Shutting down remote tomcat...'
ssh $REMOTE_SERVER '/opt/tomcat/bin/shutdown.sh'
sleep 10 #wait 15 seconds to let tomcat shut down

echo 'Is remote tomcat running?'
REMOTE_TOMCAT_PROCESS_COUNT=$(ssh $REMOTE_SERVER 'echo `ps aux | grep '/opt/tomca[t]' | grep 'java' | wc -l`')
if [[ "$REMOTE_TOMCAT_PROCESS_COUNT" == '1' ]]; then
    echo 'Yes'
else
    echo 'No'
fi

echo 'Swapping new cores on remote server...'

# SWAP IN NEW CORES
ssh $REMOTE_SERVER 'mv /opt/solr-4.2/hrwa-fsf /opt/solr-4.2/old-hrwa-fsf; mv /opt/solr-4.2/new-hrwa-fsf /opt/solr-4.2/hrwa-fsf; rm -rf /opt/solr-4.2/old-hrwa-fsf'
ssh $REMOTE_SERVER 'mv /opt/solr-4.2/hrwa-asf /opt/solr-4.2/old-hrwa-asf; mv /opt/solr-4.2/new-hrwa-asf /opt/solr-4.2/hrwa-asf; rm -rf /opt/solr-4.2/old-hrwa-asf'

echo 'Done swapping cores.'

# RESTART REMOTE TOMCAT
echo 'Restarting remote tomcat...'
ssh $REMOTE_SERVER '/opt/tomcat/bin/startup.sh'
sleep 10 #wait 10 seconds to allow remote tomcat to start up

echo 'Is remote tomcat running?'
REMOTE_TOMCAT_PROCESS_COUNT=$(ssh $REMOTE_SERVER 'echo `ps aux | grep '/opt/tomca[t]' | grep 'java' | wc -l`')
if [[ "$REMOTE_TOMCAT_PROCESS_COUNT" == '1' ]]; then
    echo 'Yes'
else
    echo 'No'
fi

echo 'Core update complete!'