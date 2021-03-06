#!/bin/bash

#example: ./averageUploads.sh http://USER:PASS@localhost:8080/lobcder/dav/Downloads ../testData/ wlan0

dest=$1
srcPath=$2
iface=$3

tar -cvf arch.tar $srcPath

./measureUploads.sh $dest ./arch.tar $iface
mv collectedUpload.csv collectedUpload-singlefile-1.csv
mv stats.csv stats-singlefile-1.csv

./measureUploads.sh $dest $srcPath $iface
mv collectedUpload.csv collectedUpload-multifile-singlethread-1.csv
mv stats.csv stats-multifile-singlethread-1.csv

./measureUploads.sh $dest $srcPath $iface mmm
mv collectedUpload.csv collectedUpload-multifile-multithread-1.csv
mv stats.csv stats-multifile-multifile-multithread-1.csv


if [ ! -f "./uploadStats.csv" ]; then
   header="fileSize(MBit);MeanSpeed(MBit/sec)SingleFile;StdevSpeed(MBit/sec)SingleFile;MeanSpeed(MBit/sec)MultiFileSinglethread;StdevSpeed(MBit/sec)MultiFileSinglethread;MeanSpeed(MBit/sec)MultiFileMultithread;StdevSpeed(MBit/sec)MultiFileMultithread;"
    echo $header > uploadStats.csv
fi


ss=`sed -n 2p stats-singlefile-1.csv | awk -F "\"*;\"*" '{ print $1 ";" $2 ";" $3}'`
ms=`sed -n 2p stats-multifile-singlethread-1.csv | awk -F "\"*;\"*" '{ print $2 ";" $3}'`
mm=`sed -n 2p stats-multifile-multifile-multithread-1.csv | awk -F "\"*;\"*" '{ print $2 ";" $3}'`

line="$ss;$ms;$mm"
echo $line >> uploadStats.csv

rm arch.tar