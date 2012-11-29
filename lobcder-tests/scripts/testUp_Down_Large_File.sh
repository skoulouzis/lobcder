#!/bin/bash

function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net" #"149.156.10.138" 
        SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav" #"/tomcatWebDAV" #
        
        PORT="8083"
        URL="http://$HOST_NAME:$PORT/$SERVER_PATH"
        
        TEST_FILE_NAME=testLargeUpload
        TEST_FILE_SERVER_PATH=$HOME/tmp/$TEST_FILE_NAME

        INTERFACE=lo


        if [ "$METHOD" = "lob" ];
        then
                SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav"   #"/lobcder-2.0/dav"
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

        if [ "$METHOD" = "swift" ];
        then
                SERVER_PATH="/auth/v1.0"
                PORT="8443"
                HOST_NAME="149.156.10.131"
                URL="https://$HOST_NAME:$PORT$SERVER_PATH"
                INTERFACE=eth0
        fi

        if [ "$METHOD" = "ftp" ];
        then
                SERVER_PATH="/ftp"
                PORT=21
                HOST_NAME="149.156.10.138"
                URL="$HOST_NAME"
        fi

        if [ "$METHOD" = "sftp" ];
        then
                SERVER_PATH="/sftp"
                PORT=22
                HOST_NAME="149.156.10.138"
                URL="$HOST_NAME"
                INTERFACE=eth0
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
                let COUNT=$TEST_FILE_SIZE_IN_GB*10
                dd if=/dev/zero of=$TEST_FILE_SERVER_PATH bs=100M count=$COUNT
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
        
        BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION.csv
        if [ "$DIRECTION" = "up" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo put $TEST_FILE_SERVER_PATH $TEST_FILE_SERVER_PATH.COPY >> cadaver.script
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" != "ftp" ];
        then
                #echo open $URL > cadaver.script
                echo put $TEST_FILE_SERVER_PATH >> cadaver.script
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get $TEST_FILE_SERVER_PATH $TEST_FILE_SERVER_PATH.COPY $TEST_FILE_NAME >> cadaver.script
        elif [ "$DIRECTION" = "down" ] && [ "$METHOD" != "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get $TEST_FILE_NAME $TEST_FILE_NAME >> cadaver.script
        fi
        echo quit >> cadaver.script
        echo quit >> cadaver.script
        echo "BWM_FILE_SERVER_PATH $BWM_FILE_SERVER_PATH"
}


function start {
        # ---------------------Start monitoring-----------------------------
        bwm-ng -o csv -I $INTERFACE -T rate -t 100 >> $BWM_FILE_SERVER_PATH &
        BWM_PID=$!
        sleep 1
        #----------------------start copy-----------------------------------
	START="$(date +%s)"
        if [ "$METHOD" = "ftp" ];
        then
		ftp < cadaver.script
        elif [ "$DIRECTION" = "down" ] && [ "$METHOD" = "swift" ];
        then
		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download LOBCDER-REPLICA-v2.0 home/$USER/tmp/$TEST_FILE_NAME"
                python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download LOBCDER-REPLICA-v2.0 home/$USER/tmp/$TEST_FILE_NAME
	elif [ "$DIRECTION" = "up" ] && [ "$METHOD" = "swift" ];
	then
		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload LOBCDER-REPLICA-v2.0 $TEST_FILE_SERVER_PATH"
		python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload LOBCDER-REPLICA-v2.0 $TEST_FILE_SERVER_PATH
        fi

        if  [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
        then
                cadaver < cadaver.script
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "sftp" ];
        then
                echo "SFTP: -r $USER_NAME@$HOST_NAME:/home/$USER_NAME $TEST_FILE_SERVER_PATH"
                scp -r $USER_NAME@$HOST_NAME:/home/$USER_NAME/$TEST_FILE_NAME  $TEST_FILE_SERVER_PATH 
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" == "sftp" ];
        then
                echo "SFTP: -r  $TEST_FILE_SERVER_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME"
                scp -r  $TEST_FILE_SERVER_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME 
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
        BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-$INTERFACE-$TEST_FILE_SIZE_IN_GB.csv
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

SLEEP=180
echo "sleeping for ....$SLEEP"
sleep $SLEEP

DIRECTION=down
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
start
formatOutputAndCleanUp


rm $TEST_FILE_SERVER_PATH
rm cadaver.script
rm $TEST_FILE_SERVER_PATH.COPY
rm quit 
rm -r home/$USER/tmp/$TEST_FILE_NAME
if [ "$METHOD" = "swift" ];
then
    python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD delete LOBCDER-REPLICA-v2.0 $TEST_FILE_SERVER_PATH
fi
