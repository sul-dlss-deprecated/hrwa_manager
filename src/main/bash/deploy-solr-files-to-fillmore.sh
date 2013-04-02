#! /bin/bash
SERVER=fillmore.cul.columbia.edu
SOLR_USER=tomcat
SOLR_FSF_CONF_DIR=/opt/solr-4.2/hrwa-fsf/conf/
SOLR_ASF_CONF_DIR=/opt/solr-4.2/hrwa-asf/conf/

BASEDIR=$(dirname $0)

echo "Deploying Solr ASF/FSF solrconf/schema files to: $SERVER"

#Rename existing files to name.xml.previous
# Move files only if they exist --> if [ -a file ]; then echo 'move file'; fi
REMOTE_MOVE_COMMANDS_TO_RUN="cd $SOLR_FSF_CONF_DIR"
REMOTE_MOVE_COMMANDS_TO_RUN="$REMOTE_MOVE_COMMANDS_TO_RUN; if [ -a ./schema.xml ]; then mv ./schema.xml ./schema.xml.previous; fi"
REMOTE_MOVE_COMMANDS_TO_RUN="$REMOTE_MOVE_COMMANDS_TO_RUN; if [ -a ./solrconfig.xml ]; then mv ./solrconfig.xml ./schema.xml.previous; fi"
REMOTE_MOVE_COMMANDS_TO_RUN="cd $SOLR_ASF_CONF_DIR"
REMOTE_MOVE_COMMANDS_TO_RUN="$REMOTE_MOVE_COMMANDS_TO_RUN; if [ -a ./schema.xml ]; then mv ./schema.xml ./schema.xml.previous; fi"
REMOTE_MOVE_COMMANDS_TO_RUN="$REMOTE_MOVE_COMMANDS_TO_RUN; if [ -a ./solrconfig.xml ]; then mv ./solrconfig.xml ./schema.xml.previous; fi"

ssh $SOLR_USER@$SERVER $REMOTE_MOVE_COMMANDS_TO_RUN

#Push latest schema and solrconfig files to the server

scp -p $BASEDIR/../../../solrconf/fsf/solr4.2-fsf.schema.xml $SOLR_USER@$SERVER:$SOLR_FSF_CONF_DIR/schema.xml
scp -p $BASEDIR/../../../solrconf/fsf/solr4.2-fsf.solrconfig.xml $SOLR_USER@$SERVER:$SOLR_FSF_CONF_DIR/solrconfig.xml
scp -p $BASEDIR/../../../solrconf/asf/solr4.2-asf.schema.xml $SOLR_USER@$SERVER:$SOLR_ASF_CONF_DIR/schema.xml
scp -p $BASEDIR/../../../solrconf/asf/solr4.2-asf.solrconfig.xml $SOLR_USER@$SERVER:$SOLR_ASF_CONF_DIR/solrconfig.xml

#Restart tomcat
RESTART_TOMCAT_COMMAND="echo 'Shutting down tomcat...';/opt/tomcat/bin/shutdown.sh;echo 'Waiting 5 seconds to allow shutdown to complete...';sleep 5;echo 'Restarting tomcat...';/opt/tomcat/bin/startup.sh"
ssh $SOLR_USER@$SERVER $RESTART_TOMCAT_COMMAND
