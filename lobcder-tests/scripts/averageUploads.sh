#!/bin/bash

srcPath=$1
iface=$2

tar -cvf arch.tar $1

./measureUploads.sh ./arch.tar $iface
mv collectedUpload.csv collectedUpload-singlefile-1.csv
mv stats.csv stats-singlefile-1.csv

./measureUploads.sh $srcPath $iface
mv collectedUpload.csv collectedUpload-multifile-singlethread-1.csv
mv stats.csv stats-multifile-singlethread-1.csv

./measureUploads.sh $srcPath $iface mmm
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