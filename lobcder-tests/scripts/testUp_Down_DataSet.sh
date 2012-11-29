#!/bin/bash

function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net" #"149.156.10.138" 
        SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav"   #"/lobcder-2.0/dav" #"/tomcatWebDAV" #

        PORT="8083"
        URL="http://$HOST_NAME:$PORT/$SERVER_PATH"

        TEST_FILE_NAME=testLargeUpload
        mkdir $HOME/tmp/dataset
        TEST_DATASET_DIR=$HOME/tmp/dataset
        TEST_DATASET_PATH=$TEST_DATASET_DIR/$TEST_FILE_NAME
        
        let TEST_DATASET_SIZE_IN_MB=$TEST_DATASET_SIZE_IN_GB*1024
        TEST_FILE_SIZE_IN_MB=30
        let NUM_OF_FILE=$TEST_DATASET_SIZE_IN_MB/$TEST_FILE_SIZE_IN_MB

        INTERFACE=lo

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
                HOST_NAME="149.156.10.138"
                URL="$HOST_NAME"
        fi

        if [ "$METHOD" = "sftp" ];
        then
                SERVER_PATH="/sftp"
                PORT=22
                HOST_NAME="149.156.10.138"
		#HOST_NAME=elab.lab.uvalight.net
                URL="$HOST_NAME"
                INTERFACE=lo
        fi


        if [ "$METHOD" = "swift" ];
        then
                SERVER_PATH="/auth/v1.0"
                PORT="8443"
                HOST_NAME="149.156.10.131"
                URL="https://$HOST_NAME:$PORT$SERVER_PATH"
                INTERFACE=eth0
        fi

        echo "BASE_DIR $BASE_DIR"
        echo "HOST_NAME $HOST_NAME"
        echo "URL $URL"
	echo "METHOD $METHOD"
}

function initMeasurePathAndFile {
# --------------------Check dir and test files-------------------
        if [ ! -d "$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH" ];
        then
                mkdir -p $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH
        fi

        if [ ! -f  $TEST_DATASET_PATH.0.dat ];
        then
                echo "File $TEST_DATASET_PATH does not exist."
                for ((i = 0 ; i < $NUM_OF_FILE ; i++)); do
                    dd if=/dev/zero of=$TEST_DATASET_PATH.$i.dat bs=30M count=1
                done
        fi

        echo "measures $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH"
        echo "file $TEST_DATASET_PATH"
}


function initMeasureFileAndCadaverScript {
        echo "machine $HOST_NAME" > $HOME/.netrc
        echo "	login $USER_NAME" >> $HOME/.netrc
        echo "	password $PASSWORD" >> $HOME/.netrc
        chmod 600 $HOME/.netrc

        echo open $URL > cadaver.script
        
        BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION.csv

        if [ "$DIRECTION" = "up" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                FILES=$TEST_DATASET_DIR/*
                for f in $FILES
                do
                    echo put $f $f.COPY >> cadaver.script
                done
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
        then
                echo open $URL > cadaver.script
                FILES=$TEST_DATASET_DIR/*
                for f in $FILES
                do
                    echo put $f  >> cadaver.script
                done
                
                #echo mput $TEST_FILE_NAME'*'  >> cadaver.script
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo nmap $TEST_DATASET_DIR/'$1 $1.COPY'  >> cadaver.script
                echo mget $TEST_DATASET_DIR/'*' >> cadaver.script
        elif [ "$DIRECTION" = "down" ] && [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
        then
                echo open $URL > cadaver.script
                echo mget $TEST_FILE_NAME'*' >> cadaver.script
        fi
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
		ftp -i < cadaver.script
	elif [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
	then 
                #cd $TEST_DATASET_DIR
                cadaver < $HOME/workspace/lobcder-tests/scripts/cadaver.script
                #cd $HOME/workspace/lobcder-tests/scripts/
	fi

	if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "swift" ];
        then
		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST"
                python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST
	elif [ "$DIRECTION" = "up" ] && [ "$METHOD" = "swift" ];
	then
		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $TEST_DATASET_DIR"
		python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $TEST_DATASET_DIR
	fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "sftp" ];
        then
                echo "SFTP: -r $USER_NAME@$HOST_NAME:/home/$USER_NAME $TEST_FILE_SERVER_PATH"
                scp -r $USER_NAME@$HOST_NAME:/home/$USER_NAME/dataset  $TEST_DATASET_DIR
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" == "sftp" ];
        then
                echo "SFTP: -r  $TEST_FILE_SERVER_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME"
                scp -r  $TEST_DATASET_DIR $USER_NAME@$HOST_NAME:/home/$USER_NAME 
        fi

       END="$(date +%s)"
       ELAPSED="$(expr $END - $START)"
       let SIZE=$TEST_DATASET_SIZE_IN_MB*1024 #ls -la $TEST_DATASET_PATH | awk '{print $5}'
       echo Elapsed time: $ELAPSED
       #rm cadaver.script
       sleep 1
       kill $BWM_PID
}


function formatOutputAndCleanUp {

# ----------------------- Format output-------------------------
        BWM_FILE_LO_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-$INTERFACE-$TEST_DATASET_SIZE_IN_GB.csv

        echo "Start time" > $BWM_FILE_LO_SERVER_PATH
        echo $START >> $BWM_FILE_LO_SERVER_PATH
        echo "End time" >> $BWM_FILE_LO_SERVER_PATH
        echo $END >> $BWM_FILE_LO_SERVER_PATH
        echo "Elapsed" >> $BWM_FILE_LO_SERVER_PATH
        echo $ELAPSED >> $BWM_FILE_LO_SERVER_PATH
        echo "Size (MB)" >> $BWM_FILE_LO_SERVER_PATH
        echo $TEST_DATASET_SIZE_IN_MB >>  $BWM_FILE_LO_SERVER_PATH
        echo "unix_timestamp;iface_name;bytes_out_$METHOD;bytes_in_$METHOD;bytes_total_$METHOD;packets_out;packets_in;packets_total;errors_out;errors_in" >>  $BWM_FILE_LO_SERVER_PATH
        sed '/total/d' $BWM_FILE_SERVER_PATH >> $BWM_FILE_LO_SERVER_PATH
        rm $BWM_FILE_SERVER_PATH
}



DIRECTION=up
METHOD=$1
USER_NAME=$2
PASSWORD=$3
TEST_DATASET_SIZE_IN_GB=$4
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
start
formatOutputAndCleanUp

SLEEP=30
echo "sleeping for ....$SLEEP"
sleep $SLEEP

DIRECTION=down
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
tart
formatOutputAndCleanUp

find $TEST_DATASET_DIR ! -name "*.dat" -type f -exec rm {} \;
rm $TEST_FILE_NAME*
rm quit 
rm -r $HOME/workspace/lobcder-tests/scripts/home
if [ "$METHOD" = "swift" ];
then
    python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD delete TEST
fi