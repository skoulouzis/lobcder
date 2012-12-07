#!/bin/bash

function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net" #"149.156.10.138" 
        SERVER_PATH="/lobcder-2.0-SNAPSHOT/dav" #"/tomcatWebDAV" #
        
        PORT="8083"
        URL="http://$HOST_NAME:$PORT/$SERVER_PATH"
        
        TEST_FILE_NAME=testLargeUpload
        TEST_FILE_PATH=$HOME/tmp/$TEST_FILE_NAME

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

        if [ ! -f  $TEST_FILE_PATH ];
        then
                echo "File $TEST_FILE_PATH does not exist."
                let COUNT=$TEST_FILE_SIZE_IN_GB*10
                dd if=/dev/zero of=$TEST_FILE_PATH bs=100M count=$COUNT
                #dd if=/dev/zero of=$TEST_FILE_PATH bs=5M count=$COUNT
                sleep 1
        fi

        echo "measures $BASE_DIR/measures/$HOST_NAME/$SERVER_PATH"
        echo "file $TEST_FILE_PATH"
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
    
        BWM_FILE_NAME=bwm-$DIRECTION.csv
        BWM_FILE_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_NAME
        if [ "$DIRECTION" = "up" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                #echo put $TEST_FILE_PATH $TEST_FILE_PATH.COPY >> cadaver.script
                echo put $TEST_FILE_PATH TEST_FILE_COPY >> cadaver.script
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" != "ftp" ];
        then
                #echo open $URL > cadaver.script
                echo put $TEST_FILE_PATH >> cadaver.script
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get TEST_FILE_COPY $TEST_FILE_NAME >> cadaver.script
        elif [ "$DIRECTION" = "down" ] && [ "$METHOD" != "ftp" ];
        then
                echo open $URL > cadaver.script
                echo get $TEST_FILE_NAME $TEST_FILE_NAME >> cadaver.script
        fi
        echo quit >> cadaver.script
        echo quit >> cadaver.script
        echo "BWM_FILE_PATH $BWM_FILE_PATH"
}


function start {
        # ---------------------Start monitoring-----------------------------
        bwm-ng -o csv rate -t 1000 > $BWM_FILE_PATH &
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
		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload LOBCDER-REPLICA-v2.0 $TEST_FILE_PATH"
		python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload LOBCDER-REPLICA-v2.0 $TEST_FILE_PATH
        fi

        if  [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
        then
                cadaver < cadaver.script
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "sftp" ];
        then
                echo "SFTP: -r $USER_NAME@$HOST_NAME:/home/$USER_NAME $TEST_FILE_PATH"
                scp -r $USER_NAME@$HOST_NAME:/home/$USER_NAME/$TEST_FILE_NAME  $TEST_FILE_PATH 
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" == "sftp" ];
        then
                echo "SFTP: -r  $TEST_FILE_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME"
                scp -r  $TEST_FILE_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME 
        fi

       END="$(date +%s)"
       ELAPSED="$(expr $END - $START)"
       SIZE= ls -la $TEST_FILE_PATH | awk '{print $5}'
       echo Elapsed time: $ELAPSED
       #rm cadaver.script
       sleep 1
       kill $BWM_PID
}


function formatOutputAndCleanUp {
# ----------------------- Format output-------------------------
        BWM_FILE_NAME_FINAL=bwm-$DIRECTION-$TEST_FILE_SIZE_IN_GB.csv
        BWM_FILE_PATH_FINAL=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_NAME_FINAL
        

        BWM_FILE_LO_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-lo-$TEST_FILE_SIZE_IN_GB.csv
        BWM_FILE_LO_Rx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-lo-Rx-$TEST_FILE_SIZE_IN_GB.csv
        BWM_FILE_LO_Tx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-lo-Tx-$TEST_FILE_SIZE_IN_GB.csv

        BWM_FILE_ETH0_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-eth0-$TEST_FILE_SIZE_IN_GB.csv

        BWM_FILE_ETH0_Tx_NAME=bwm-$DIRECTION-eth0-Tx-$TEST_FILE_SIZE_IN_GB.csv
        BWM_FILE_ETH0_Tx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_ETH0_Tx_NAME
        BWM_FILE_ETH0_Rx_NAME=bwm-$DIRECTION-eth0-Rx-$TEST_FILE_SIZE_IN_GB.csv
        BWM_FILE_ETH0_Rx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_ETH0_Rx_NAME

        echo "Start time" > $BWM_FILE_PATH_FINAL
        echo $START >> $BWM_FILE_PATH_FINAL
        echo "End time" >> $BWM_FILE_PATH_FINAL
        echo $END >> $BWM_FILE_PATH_FINAL
        echo "Elapsed" >> $BWM_FILE_PATH_FINAL
        echo $ELAPSED >> $BWM_FILE_PATH_FINAL
        echo "Elapsed" $ELAPSED
        echo "Size (MB)" >> $BWM_FILE_PATH_FINAL
        TEST_FILE_SIZE_IN_MB=$(echo "$TEST_FILE_SIZE_IN_GB * 1024.0" |bc -l)
        echo $TEST_FILE_SIZE_IN_MB >> $BWM_FILE_PATH_FINAL
        echo "Speed (MB/sec)" >> $BWM_FILE_PATH_FINAL
        SPEED=$(echo "$TEST_FILE_SIZE_IN_MB / $ELAPSED" |bc -l)
        echo $SPEED >> $BWM_FILE_PATH_FINAL
        echo "Speed (MB/sec): $SPEED"


        echo "unix_timestamp;iface_name;Mbytes_out_"$METHOD"_lo;Mbytes_in_"$METHOD"_lo;bytes_total_"$METHOD"_lo;packets_out;packets_in;packets_total;errors_out;errors_in" > $BWM_FILE_LO_PATH


	cp $BWM_FILE_PATH  $BWM_FILE_NAME
	awk  -v s=$START -F "\"*;\"*" '{ print $1-s ";" $2 ";" $3/(1024*1024) ";" $4/(1024*1024) ";" $5/(1024*1024) ";" $6 ";" $7 ";" $9 ";" $10}' $BWM_FILE_NAME > tmp
	mv tmp $BWM_FILE_PATH

        
        cat $BWM_FILE_PATH | grep lo >> $BWM_FILE_LO_PATH


        echo "unix_timestamp;iface_name;Mbytes_out_"$METHOD"_eth0;Mbytes_in_"$METHOD"_eth0;Mbytes_total_"$METHOD"_eth0;packets_out;packets_in;packets_total;errors_out;errors_in" > $BWM_FILE_ETH0_PATH
        cat $BWM_FILE_PATH | grep 'eth0' | sed '/peth/d' >> $BWM_FILE_ETH0_PATH
        
        if [ "$DIRECTION" = "up" ] ;
        then
            awk -F "\"*;\"*" '{print $1 ";" $4}' $BWM_FILE_LO_PATH > $BWM_FILE_LO_Rx_PATH
            awk -F "\"*;\"*" '{print $3}' $BWM_FILE_ETH0_PATH >> $BWM_FILE_ETH0_Tx_PATH
            cp $BWM_FILE_ETH0_Tx_PATH $BWM_FILE_ETH0_Tx_NAME
            awk -F "\"*;\"*" '{ OFS=";"} {getline add < "'$BWM_FILE_ETH0_Tx_NAME'"} {print $0,add}' $BWM_FILE_LO_Rx_PATH >> $BWM_FILE_PATH_FINAL
            mv $BWM_FILE_ETH0_Tx_NAME $BWM_FILE_ETH0_Tx_PATH
        fi
        
        if [ "$DIRECTION" = "down" ] ;
        then
            awk -F "\"*;\"*" '{print $1 ";" $3}' $BWM_FILE_LO_PATH > $BWM_FILE_LO_Tx_PATH
            awk -F "\"*;\"*" '{print $4}' $BWM_FILE_ETH0_PATH >> $BWM_FILE_ETH0_Rx_PATH
            cp $BWM_FILE_ETH0_Rx_PATH .
            awk -F "\"*;\"*" '{ OFS=";"} {getline add < "'$BWM_FILE_ETH0_Rx_NAME'"} {print $0,add}' $BWM_FILE_LO_Tx_PATH >> $BWM_FILE_PATH_FINAL
            mv $BWM_FILE_ETH0_Rx_NAME $BWM_FILE_ETH0_Rx_PATH
        fi
        DATE=`date +%s`
        mv $BWM_FILE_PATH_FINAL $BWM_FILE_PATH_FINAL-$DATE.csv
        rm ./*.csv
        rm $BWM_FILE_LO_PATH $BWM_FILE_LO_Rx_PATH $BWM_FILE_LO_Tx_PATH $BWM_FILE_ETH0_PATH $BWM_FILE_ETH0_Tx_PATH $BWM_FILE_ETH0_Rx_PATH
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

SLEEP=3
echo "sleeping for ....$SLEEP"
sleep $SLEEP

DIRECTION=down
initVariables
initMeasurePathAndFile
initMeasureFileAndCadaverScript
start
formatOutputAndCleanUp


rm $TEST_FILE_PATH
rm cadaver.script
rm $TEST_FILE_PATH.COPY
rm quit 
rm -r home/$USER/tmp/$TEST_FILE_NAME
if [ "$METHOD" = "swift" ];
then
    python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD delete LOBCDER-REPLICA-v2.0 $TEST_FILE_PATH
fi
