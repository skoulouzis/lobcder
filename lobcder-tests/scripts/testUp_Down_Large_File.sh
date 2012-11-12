#!/bin/bash

# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/shutdown.sh
# $HOME/servers/apache-tomcat-6.0.35-TEST/bin/startup.sh
# sleep 5


function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net"
        SERVER_PATH="/tomcatWebDAV" #"/lobcder-2.0-SNAPSHOT/dav"

        PORT="8083"
        URL="http://$HOST_NAME:$PORT$SERVER_PATH"

        TEST_FILE_NAME=testLargeUpload
        TEST_FILE_SERVER_PATH=$HOME/tmp/$TEST_FILE_NAME
        let TEST_FILE_SIZE=1024*1024*1000


        if [ "$1" = "lob" ];
        then
                SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav"
        fi

        if [ "$1" = "dav" ];
        then
                SERVER_PATH="/tomcatWebDAV"
        fi
        if [ "$1" = "ftp" ];
        then
                SERVER_PATH="/ftp"
                PORT=21
                URL="ftp://$HOST_NAME$SERVER_PATH"
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
                dd if=/dev/zero of=$TEST_FILE_SERVER_PATH bs=$TEST_FILE_SIZE count=1
        fi

        echo "SERVER_PATH $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH"
}


function initMeasureFileAndCadaverScript {
#	echo "ARGS: 1: $1 2: $2 3: $3 4 $4"
	rm -f $HOME/.netrc
        echo "machine $HOST_NAME" > $HOME/.netrc
        echo "	login $3" >> $HOME/.netrc
        echo "	password $4" >> $HOME/.netrc

        echo open $URL > cadaver.script

        if [ "$2" = "ftp" ];
        then
                #echo open $HOST_NAME > cadaver.script
		rm -f $HOME/.netrc
        fi


        if [ "$1" = "up" ];
        then
                echo put $TEST_FILE_SERVER_PATH >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-up.csv
                echo "UP!"
        fi

        if [ "$1" = "down" ];
        then
                echo get $TEST_FILE_NAME >> cadaver.script
                BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-down.csv
                echo "DOWN!"
        fi
        echo quit >> cadaver.script
        echo "BWM_FILE $BWM_FILE_SERVER_PATH"
}


function start {
        CMD="cadaver < cadaver.script"
        if [ "$1" = "ftp" ];
        then
		CMD="ftp << cadaver.script"
	fi

echo "Command $CMD"
echo "bwm-ng path $BWM_FILE_SERVER_PATH"
# ---------------------Start monitoring-----------------------------
bwm-ng -o csv -I lo -T rate -t 100 >> $BWM_FILE_SERVER_PATH &
       BWM_PID=$!
       sleep 1
#----------------------start copy-----------------------------------
	START="$(date +%s)"
	#$CMD                    #cadaver < cadaver.script       

ftp -inv $HOST_NAME <<ENDFTP
user $2 $3
put $TEST_FILE_SERVER_PATH $SERVER_PATH
bye
ENDFTP



       END="$(date +%s)"
       ELAPSED="$(expr $END - $START)"
       SIZE= ls -la $TEST_FILE_SERVER_PATH | awk '{print $5}'
       echo Elapsed time: $ELAPSED
       rm cadaver.script
       sleep 1
       kill $BWM_PID
}


function formatOutput {

# ----------------------- Format output-------------------------
        if [ "$1" = "up" ];
        then
                BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-up-lo.csv
        fi

        if [ "$1" = "down" ];
        then
                BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-down-lo.csv
        fi
        echo "Start time" > $BWM_FILE_LO_SERVER_PATH
        echo $START >> $BWM_FILE_LO_SERVER_PATH
        echo "End time" >> $BWM_FILE_LO_SERVER_PATH
        echo $END >> $BWM_FILE_LO_SERVER_PATH
        echo "Elapsed" >> $BWM_FILE_LO_SERVER_PATH
        echo $ELAPSED >> $BWM_FILE_LO_SERVER_PATH
        echo "Size" >> $BWM_FILE_LO_SERVER_PATH
        echo $SIZE >>  $BWM_FILE_LO_SERVER_PATH
        echo "unix_timestamp;iface_name;bytes_out;bytes_in;bytes_total;packets_out;packets_in;packets_total;errors_out;errors_in" >>  $BWM_FILE_LO_SERVER_PATH
        sed '/total/d' $BWM_FILE_SERVER_PATH >> $BWM_FILE_LO_SERVER_PATH
        rm $BWM_FILE_SERVER_PATH

}



#----------------FTP---------------------
DIRECTION=up
initVariables $1
initMeasurePathAndFile
initMeasureFileAndCadaverScript $DIRECTION $1 $2 $3
start $1 $2 $3
# formatOutput $DIRECTION

