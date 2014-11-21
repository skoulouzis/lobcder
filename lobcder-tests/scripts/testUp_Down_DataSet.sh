#!/bin/bash

function initVariables {
        BASE_DIR=$HOME/workspace/lobcder-tests
        HOST_NAME="elab.lab.uvalight.net" #"149.156.10.138" 
        SERVER_PATH="/tomcatWebDAV"   #"/lobcder-2.0/dav" #"/tomcatWebDAV" #

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
 		#for ((i = 0 ; i < 5 ; i++)); do
                    dd if=/dev/zero of=$TEST_DATASET_PATH.$i.dat bs="$TEST_FILE_SIZE_IN_MB"'M' count=1
  		    #dd if=/dev/zero of=$TEST_DATASET_PATH.$i.dat bs=2M count=1
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
        
        BWM_FILE_SERVER_NAME=bwm-$DIRECTION.csv
        BWM_FILE_SERVER_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_SERVER_NAME

        if [ "$DIRECTION" = "up" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                FILES=$TEST_DATASET_DIR/*
                echo prompt >> cadaver.script
                #echo mput $TEST_DATASET_DIR/* >> cadaver.script
                for f in $FILES
                do
                    COUNT=`expr $COUNT + 1` 
                    #echo put $f $f.COPY >> cadaver.script
                    echo put $f dataset/test_"$COUNT" >> cadaver.script
                done
        elif [ "$DIRECTION" = "up" ] && [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
        then
                echo open $URL > cadaver.script
                FILES=$TEST_DATASET_DIR/*
                for f in $FILES
                do
                    echo put $f  >> cadaver.script
                done
                
        fi

        if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "ftp" ];
        then
                echo open $URL > cadaver.script
                echo nmap $TEST_DATASET_DIR/'$1 $1.COPY'  >> cadaver.script
                #echo mget $TEST_DATASET_DIR/'*' >> cadaver.script
                echo cd dataset >> cadaver.script 
                echo 'mget test*' >> cadaver.script 
        fi
        if [ "$DIRECTION" = "down" ];
        then
	   if [ "$METHOD" = "lob" ] || [ "$METHOD" = "dav" ] || [ "$METHOD" = "javadav" ];
	   then
                echo open $URL > cadaver.script
                echo mget $TEST_FILE_NAME'*' >> cadaver.script
	    fi
        fi
        echo quit >> cadaver.script

#         echo "BWM_FILE_PATH $BWM_FILE_SERVER_PATH"
#         echo "-------------------"
        cat cadaver.script
}


function start {
        # ---------------------Start monitoring-----------------------------
	bwm-ng -o csv -T rate -t 1000 > $BWM_FILE_SERVER_PATH &
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
                 time cadaver < $HOME/workspace/lobcder-tests/scripts/cadaver.script
                 #cd $HOME/workspace/lobcder-tests/scripts/
 	fi
 
 	if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "swift" ];
         then
#  		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST"
#                  python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST
		FILES=$TEST_DATASET_DIR/*
		for f in $FILES
                do
		  NAME=`basename $f`
# 		  echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST home/skoulouz/tmp/dataset/$NAME"
 		  python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD download TEST home/skoulouz/tmp/dataset/$NAME
                done
 	elif [ "$DIRECTION" = "up" ] && [ "$METHOD" = "swift" ];
 	then
#  		echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $TEST_DATASET_DIR"
#  		python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $TEST_DATASET_DIR
		FILES=$TEST_DATASET_DIR/*
		for f in $FILES
                do
# 		  echo "SWIFT: python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $f"
		  python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD upload TEST $f
                done
 	fi
 
         if [ "$DIRECTION" = "down" ] && [ "$METHOD" = "sftp" ];
         then
#                  echo "SFTP: -r $USER_NAME@$HOST_NAME:/home/$USER_NAME $TEST_FILE_SERVER_PATH"
                 scp -r $USER_NAME@$HOST_NAME:/home/$USER_NAME/dataset  $TEST_DATASET_DIR
         elif [ "$DIRECTION" = "up" ] && [ "$METHOD" == "sftp" ];
         then
#                  echo "SFTP: -r  $TEST_FILE_SERVER_PATH $USER_NAME@$HOST_NAME:/home/$USER_NAME"
                 scp -r  $TEST_DATASET_DIR $USER_NAME@$HOST_NAME:/home/$USER_NAME 
         fi

       END="$(date +%s)"
       ELAPSED="$(expr $END - $START)"
       SIZE=`du -sb $TEST_DATASET_DIR | awk '{print $1}'` #$TEST_DATASET_SIZE_IN_MB*1024 #ls -la $TEST_DATASET_PATH | awk '{print $5}'
       echo Elapsed time: $ELAPSED
       #rm cadaver.script
       sleep 1
       kill $BWM_PID
}


function formatOutputAndCleanUp {

# ----------------------- Format output-------------------------
        BWM_FILE_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-$TEST_DATASET_SIZE_IN_GB.csv

        BWM_FILE_LO_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-lo-$TEST_DATASET_SIZE_IN_GB.csv
        BWM_FILE_LO_Rx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-lo-Rx-$TEST_DATASET_SIZE_IN_GB.csv
        BWM_FILE_LO_Tx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-lo-Tx-$TEST_DATASET_SIZE_IN_GB.csv

        BWM_FILE_ETH0_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/bwm-$DIRECTION-dataset-eth0-$TEST_DATASET_SIZE_IN_GB.csv

        BWM_FILE_ETH0_Tx_NAME=bwm-$DIRECTION-dataset-eth0-Tx-$TEST_DATASET_SIZE_IN_GB.csv
        BWM_FILE_ETH0_Tx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_ETH0_Tx_NAME
        BWM_FILE_ETH0_Rx_NAME=bwm-$DIRECTION-dataset-eth0-Rx-$TEST_DATASET_SIZE_IN_GB.csv
        BWM_FILE_ETH0_Rx_PATH=$BASE_DIR/measures/$HOST_NAME/$SERVER_PATH/$BWM_FILE_ETH0_Rx_NAME

        echo "Start time" > $BWM_FILE_PATH
        echo $START >> $BWM_FILE_PATH
        echo "End time" >> $BWM_FILE_PATH
        echo $END >> $BWM_FILE_PATH
        echo "Elapsed" >> $BWM_FILE_PATH
        echo $ELAPSED >> $BWM_FILE_PATH
        echo "Elapsed" $ELAPSED
        echo "Size (MBit)" >> $BWM_FILE_PATH
        TEST_FILE_SIZE_IN_MBITS=$(echo "($SIZE * 8)   / (1000.0 * 1000.0)" |bc -l)
        echo $TEST_FILE_SIZE_IN_MBITS >> $BWM_FILE_PATH
        echo "Speed (MBit/sec)" >> $BWM_FILE_PATH
        SPEED=$(echo "$TEST_FILE_SIZE_IN_MBITS  / $ELAPSED" |bc -l)
        echo $SPEED >> $BWM_FILE_PATH
        echo "Speed (MBit/sec): $SPEED"


        echo "unix_timestamp;iface_name;MBits_out_"$METHOD"_lo;MBits_in_"$METHOD"_lo;bytes_total_"$METHOD"_lo;packets_out;packets_in;packets_total;errors_out;errors_in" > $BWM_FILE_LO_PATH
	cp $BWM_FILE_SERVER_PATH  $BWM_FILE_SERVER_NAME
	awk  -v s=$START -F "\"*;\"*" '{ print $1-s ";" $2 ";" ($3*8) / (1000*1000) ";" ($4*8) / (1000*1000) ";" ($5*8) / (1000*1000) ";" ($6*8) / (1000*1000) ";" $7 ";" $9 ";" $10}'  $BWM_FILE_SERVER_NAME > tmp
	mv tmp $BWM_FILE_SERVER_PATH
	
        cat $BWM_FILE_SERVER_PATH | grep lo >> $BWM_FILE_LO_PATH

	
        echo "unix_timestamp;iface_name;MBits_out_"$METHOD"_eth0;MBits_in_"$METHOD"_eth0;MBits_total_"$METHOD"_eth0;packets_out;packets_in;packets_total;errors_out;errors_in" > $BWM_FILE_ETH0_PATH
        cat $BWM_FILE_SERVER_PATH | grep 'eth0' | sed '/peth/d' >> $BWM_FILE_ETH0_PATH
        
        if [ "$DIRECTION" = "up" ] ;
        then
            awk -F "\"*;\"*" '{print $1 ";" $4}' $BWM_FILE_LO_PATH > $BWM_FILE_LO_Rx_PATH
            awk -F "\"*;\"*" '{print $3}' $BWM_FILE_ETH0_PATH >> $BWM_FILE_ETH0_Tx_PATH
            cp $BWM_FILE_ETH0_Tx_PATH .
            awk -F "\"*;\"*" '{ OFS=";"} {getline add < "'$BWM_FILE_ETH0_Tx_NAME'"} {print $0,add}' $BWM_FILE_LO_Rx_PATH >> $BWM_FILE_PATH
            mv $BWM_FILE_ETH0_Tx_NAME $BWM_FILE_ETH0_Tx_PATH
        fi
        
        if [ "$DIRECTION" = "down" ] ;
        then
            awk -F "\"*;\"*" '{print $1 ";" $3}' $BWM_FILE_LO_PATH > $BWM_FILE_LO_Tx_PATH
            awk -F "\"*;\"*" '{print $4}' $BWM_FILE_ETH0_PATH >> $BWM_FILE_ETH0_Rx_PATH
            cp $BWM_FILE_ETH0_Rx_PATH .
            awk -F "\"*;\"*" '{ OFS=";"} {getline add < "'$BWM_FILE_ETH0_Rx_NAME'"} {print $0,add}' $BWM_FILE_LO_Tx_PATH >> $BWM_FILE_PATH
            mv $BWM_FILE_ETH0_Rx_NAME $BWM_FILE_ETH0_Rx_PATH
        fi
        DATE=`date +%s`
        mv $BWM_FILE_PATH $BWM_FILE_PATH-$DATE.csv
        rm ./*.csv
        rm $BWM_FILE_LO_PATH $BWM_FILE_LO_Rx_PATH $BWM_FILE_LO_Tx_PATH $BWM_FILE_ETH0_PATH $BWM_FILE_ETH0_Tx_PATH $BWM_FILE_ETH0_Rx_PATH

}



DIRECTION=up
METHOD=$1
USER_NAME=$2
PASSWORD=$3
TEST_DATASET_SIZE_IN_GB=$4

if [ "$METHOD" = "sftp" ] || [ "$METHOD" = "ftp" ];
then 
  ssh $USER_NAME@$HOST_NAME 'rm -r dataset/*'
  ssh $USER_NAME@$HOST_NAME 'rm -r testLargeUpload*'
fi

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

find $TEST_DATASET_DIR ! -name "*.dat" -type f -exec rm {} \;
rm $TEST_FILE_NAME*
rm quit 
rm -r $HOME/workspace/lobcder-tests/scripts/home
rm -r $HOME/tmp/home
if [ "$METHOD" = "swift" ];
then
    python2.6  /home/$USER/Documents/scripts/swift -A $URL -U $USER_NAME -K $PASSWORD delete TEST
fi

rm -r  ~/tmp/dataset/*
if [ "$METHOD" = "sftp" ] || [ "$METHOD" = "ftp" ];
then 
  ssh $USER_NAME@$HOST_NAME 'rm -r dataset/*'
  ssh $USER_NAME@$HOST_NAME 'rm -r testLargeUpload*'
fi
