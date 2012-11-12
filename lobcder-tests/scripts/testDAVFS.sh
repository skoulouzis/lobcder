#!/bin/bash

# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/shutdown.sh
# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/startup.sh
# sleep 5

BASE_DIR=$HOME/workspace/lobcder-tests

WEB_DAV_HOST="elab.lab.uvalight.net"
WEB_DAV_PATH="/lobcder-2.0-SNAPSHOT/dav"
WEB_DAV_URL="http://$WEB_DAV_HOST:8083$WEB_DAV_PATH"

TEST_FILE_PATH=$HOME/tmp/testLargeUpload
let TEST_FILE_SIZE=1024*1024*2000

# --------------------Check dir and test files-------------------
WEB_DAV_MOUNT_POINT=`mount | grep elab | awk '{print $3;}'`
if [ -z $WEB_DAV_MOUNT_POINT ]; 
then
	echo "webdav is no mounted"
	exit -1
fi

if [ ! -d "$BASE_DIR/measures/$WEB_DAV_HOST/$WEB_DAV_PATH" ]; 
then
	mkdir -p $BASE_DIR/measures/$WEB_DAV_HOST/$WEB_DAV_PATH
fi

if [ ! -f  $TEST_FILE_PATH ];
then
	echo "File $TEST_FILE_PATH does not exist."
	dd if=/dev/zero of=$TEST_FILE_PATH bs=$TEST_FILE_SIZE count=1
fi


# ---------------------Add header -----------------------------
BWM_FILE_PATH=$BASE_DIR/measures/$WEB_DAV_HOST/$WEB_DAV_PATH/bwm-up.csv
echo "unix_timestamp;iface_name;bytes_out;bytes_in;bytes_total;packets_out;packets_in;packets_total;errors_out;errors_in" > $BWM_FILE_PATH
# ---------------------Start monitoring-----------------------------
bwm-ng -o csv -I lo -T rate >> $BWM_FILE_PATH &
BWM_PID=$!

#----------------------start copy-----------------------------------
START="$(date +%s)"
cp $TEST_FILE_PATH $WEB_DAV_MOUNT_POINT
END="$(date +%s)"
ELAPSED="$(expr $END - $START)"
echo Elapsed time: $ELAPSED


sleep 1
kill $BWM_PID

BWM_FILE_LO_PATH=$BASE_DIR/measures/$WEB_DAV_HOST/$WEB_DAV_PATH/bwm-up-lo.csv
echo "unix_timestamp;iface_name;bytes_out;bytes_in;bytes_total;packets_out;packets_in;packets_total;errors_out;errors_in" >  $BWM_FILE_LO_PATH
sed '/total/d' $BWM_FILE_PATH >> $BWM_FILE_LO_PATH
rm $BWM_FILE_PATH



