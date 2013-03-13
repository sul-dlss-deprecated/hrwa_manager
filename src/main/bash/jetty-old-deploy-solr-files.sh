#! /bin/bash
SERVER=fillmore.cul.columbia.edu
SOLR_USER=litojetty
SOLR_FSF_CONF_DIR=/opt/solr-4.1/hrwa-fsf/conf/
SOLR_ASF_CONF_DIR=/opt/solr-4.1/hrwa-asf/conf/

BASEDIR=$(dirname $0)

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

scp -p $BASEDIR/../resources/solrconf/fsf/solr4.1-fsf.schema.xml $SOLR_USER@$SERVER:$SOLR_FSF_CONF_DIR/schema.xml
scp -p $BASEDIR/../resources/solrconf/fsf/solr4.1-fsf.solrconfig.xml $SOLR_USER@$SERVER:$SOLR_FSF_CONF_DIR/solrconfig.xml
scp -p $BASEDIR/../resources/solrconf/asf/solr4.1-asf.schema.xml $SOLR_USER@$SERVER:$SOLR_ASF_CONF_DIR/schema.xml
scp -p $BASEDIR/../resources/solrconf/asf/solr4.1-asf.solrconfig.xml $SOLR_USER@$SERVER:$SOLR_ASF_CONF_DIR/solrconfig.xml

#Restart jetty as self (requires password)
echo 'To make these change live, ssh into $SERVER and run: sudo /sbin/service litojetty restart'
