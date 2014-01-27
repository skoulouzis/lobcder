#!/bin/bash

#example: ./averageDownloads.sh http://USER:PASS@localhost:8080/lobcder/dav/Downloads ~/Downloads/files2/ wlan0

srcURL=$1
dest=file
testData=$2
iface=$3

lstChar=`echo "$srcURL" | tail -c 2`
if [ "$lstChar" = "/" ] ;then
  srcURL=`echo "${srcURL%?}"`
fi

tar -cvf arch.tar $testData
curl -T arch.tar $srcURL/ &

files=$testData/*
for f in $files
do
  curl -T $f $srcURL/dir/ &
done

wait

sleep 20



./measureDownloads.sh $srcURL/arch.tar $dest $iface
mv collectedDownload.csv collectedDownload-singlefile-1.csv
mv stats.csv stats-singlefileDown-1.csv

./measureDownloads.sh $srcURL/dir $dest $iface
mv collectedDownload.csv collectedDownload-multifile-singlethread-1.csv
mv stats.csv stats-multifileDown-singlethread-1.csv

./measureDownloads.sh $srcURL/dir $dest $iface mmmm
mv collectedDownload.csv collectedDownload-multifile-multithread-1.csv
mv stats.csv stats-multifileDown-multithread-1.csv


if [ ! -f "./downloadStats.csv" ]; then
   header="fileSize(MBit);MeanSpeed(MBit/sec)SingleFile;StdevSpeed(MBit/sec)SingleFile;MeanSpeed(MBit/sec)MultiFileSinglethread;StdevSpeed(MBit/sec)MultiFileSinglethread;MeanSpeed(MBit/sec)MultiFileMultithread;StdevSpeed(MBit/sec)MultiFileMultithread;"
    echo $header > downloadStats.csv
fi


ss=`sed -n 2p stats-singlefileDown-1.csv | awk -F "\"*;\"*" '{ print $1 ";" $2 ";" $3}'`
ms=`sed -n 2p stats-multifileDown-singlethread-1.csv | awk -F "\"*;\"*" '{ print $2 ";" $3}'`
mm=`sed -n 2p stats-multifileDown-multithread-1.csv | awk -F "\"*;\"*" '{ print $2 ";" $3}'`

line="$ss;$ms;$mm"
echo $line >> downloadStats.csv

rm arch.tar