#!/bin/bash

function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net"
        SERVER_PATH="/tomcatWebDAV" #"/lobcder-2.0-SNAPSHOT/dav"

        PORT="8083"
        URL="http://$HOST_NAME:$PORT/$SERVER_PATH"
        
        TEST_FILE_NAME=testLargeUpload
        TEST_FILE_SERVER_PATH=$HOME/tmp/$TEST_FILE_NAME


        if [ "$METHOD" = "lob" ];
        then
                SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav"
                URL="http://$HOST_NAME:$PORT$SERVER_PATH"
        fi

        if [ "$METHOD" = "javadav" ];
        then
                SERVER_PATH="/tomcatWebDAV"
                URL="http://$HOST_NAME:$PORT$SERVER_PATH"
        fi

        if [ "$METHOD" = "dav" ];
        then
                SERVER_PATH="/webdav"
                PORT="80"
                URL="http://$HOST_NAME:$PORT$SERVER_PATH"
        fi

        if [ "$METHOD" = "ftp" ];
        then
                SERVER_PATH="/ftp"
                PORT=21
                URL="$HOST_NAME"
        fi
        echo "BASE_DIR $BASE_DIR"
        echo "HOST_NAME $HOST_NAME"
        echo "URL $URL"
}

function initMeasurePathAndFile {
# --------------------Check dir and test files-------------------
        if [ ! -d "$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH" ];
        then
                mkdir -p $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH
        fi

        if [ ! -f  $TEST_FILE_SERVER_PATH ];
        then
                echo "File $TEST_FILE_SERVER_PATH does not exist."
                dd if=/dev/zero of=$TEST_FILE_SERVER_PATH bs=1G count=$TEST_FILE_SIZE_IN_GB
                sleep 1
        fi

        echo "measures $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH"
        echo "file $TEST_FILE_SERVER_PATH"
}


function initMeasureFileAndCadaverScript {
        echo "machine $HOST_NAME" > $HOME/.netrc
        echo "	login $USER_NAME" >> $HOME/.netrc
        echo "	password $PASSWORD" >> $HOME/.netrc
        chmod 600 $HOME/.netrc

        echo open $URL > cadaver.script

#        if [ "$METHOD" = "ftp" ];
#        then
#                echo open $HOST_NAME > cadaver.script
#        fi

        if [ "$DIRECTION" = "up" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo put $TEST_FILE_SERVER_PATH $TEST_FILE_SERVER_PATH.COPY >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-up.csv
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" != "ftp" ];
        then
                #echo open $URL > cadaver.script
                echo put $TEST_FILE_SERVER_PATH >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-up.csv
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get $TEST_FILE_SERVER_PATH $TEST_FILE_SERVER_PATH.COPY >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-down.csv
        elif [ "$DIRECTION" = "down" ] && [ "$METHOD" != "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get $TEST_FILE_NAME >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-down.csv
        fi
        echo quit >> cadaver.script
}


function start {
        # ---------------------Start monitoring-----------------------------
        bwm-ng -o csv -I lo -T rate -t 100 >> $BWM_FILE_SERVER_PATH &
        BWM_PID=$!
        sleep 1
        #----------------------start copy-----------------------------------
	START="$(date +%s)"
        if [ "$METHOD" = "ftp" ];
        then
		ftp < cadaver.script
	else
                cadaver < cadaver.script
        fi

       END="$(date +%s)"
       ELAPSED="$(expr $END - $START)"
       SIZE= ls -la $TEST_FILE_SERVER_PATH | awk '{print $5}'
       echo Elapsed time: $ELAPSED
       #rm cadaver.script
       sleep 1
       kill $BWM_PID
}


function formatOutputAndCleanUp {

# ----------------------- Format output-------------------------
        if [ "$DIRECTION" = "up" ];
        then
                BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-up-lo-$TEST_FILE_SIZE.csv
        fi

        if [ "$DIRECTION" = "down" ];
        then
                BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-down-lo-$TEST_FILE_SIZE.csv
        fi
        echo "Start time" > $BWM_FILE_LO_SERVER_PATH
        echo $START >> $BWM_FILE_LO_SERVER_PATH
        echo "End time" >> $BWM_FILE_LO_SERVER_PATH
        echo $END >> $BWM_FILE_LO_SERVER_PATH
        echo "Elapsed" >> $BWM_FILE_LO_SERVER_PATH
        echo $ELAPSED >> $BWM_FILE_LO_SERVER_PATH
        echo "Size" >> $BWM_FILE_LO_SERVER_PATH
        echo $SIZE >>  $BWM_FILE_LO_SERVER_PATH
        echo "unix_timestamp;iface_name;bytes_out_$METHOD;bytes_in_$METHOD;bytes_total_$METHOD;packets_out;packets_in;packets_total;errors_out;errors_in" >>  $BWM_FILE_LO_SERVER_PATH
        sed '/total/d' $BWM_FILE_SERVER_PATH >> $BWM_FILE_LO_SERVER_PATH
        rm $BWM_FILE_SERVER_PATH
        #rm $TEST_FILE_SERVER_PATH
        rm $TEST_FILE_SERVER_PATH.COPY

}



DIRECTION=up
METHOD=$1
USER_NAME=$2
PASSWORD=$3
TEST_FILE_SIZE_IN_GB=$4
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
start
formatOutputAndCleanUp

DIRECTION=down
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
start
formatOutputAndCleanUp
