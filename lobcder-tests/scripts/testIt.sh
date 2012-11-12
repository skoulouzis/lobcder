#!/bin/bash

# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/shutdown.sh
# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/startup.sh
# sleep 5
BASE_DIR=$HOME/workspace/lobcder-tests

TEST_URL="webdav.test.url=http://localhost:8083/lobcder-2.0-SNAPSHOT/dav"
TEST_UNAME1="webdav.test.username1=demo1"
TEST_PASS1="webdav.test.password1=d3moPasswd"


bwm-ng -o csv -I lo -T rate >> $BASE_DIR/measures/localhost/lobcder-2.0-SNAPSHOT/$TEST_URL &
BWM_PID=$!



sleep 2
kill $BWM_PID

sed '/total/d' $BASE_DIR/measures/localhost/lobcder-2.0-SNAPSHOT/bm_lobcder_mistos.csv > $BASE_DIR/measures/localhost/lobcder-2.0-SNAPSHOT/bm_




