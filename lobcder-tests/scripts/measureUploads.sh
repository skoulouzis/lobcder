#!/bin/bash

# example ./measureUploads.sh ./arch.tar eth0

loopNum=5
dest=$1
srcFilePath=$2
iface=$3
multithread=$4



if [[ -d $srcFilePath ]]; then
  size=`du -sb $srcFilePath | awk '{print $1}'`
else
  size=$(stat -c%s "$srcFilePath")
fi
fileSize=$(echo "($size * 8) / (1000 * 1000)" |bc -l)

for ((i = 0 ; i < $loopNum ; i++)); do
  if [ -n "$multithread" ]; then
    ./uploadFileCurl.sh  $srcFilePath $dest $iface mmm
  else
    ./uploadFileCurl.sh  $srcFilePath $dest $iface
  fi
  mv report.csv report-$fileSize-$i.csv
done

header="fileSize(MBit);speed(MBit/sec)"
echo $header > collectedUpload.csv
sum=0;
for f in `ls report-$fileSize-*.csv`
do
    sed -n 2p $f | awk -F "\"*;\"*" '{ print $7 ";" $8 }' >> collectedUpload.csv
    sed -n 2p $f | awk -F "\"*;\"*" '{ print $8 }' >> speed
done 


stats=`R -q -e "x <- read.csv('speed', header = F); summary(x); sd(x[ , 1])"`
#echo $stats
#> x <- read.csv('speed', header = F); summary(x); sd(x[ , 1]) V1 Min. :50.16 1st Qu.:51.19 Median :52.23 Mean :52.23 3rd Qu.:53.27 Max. :54.30 [1] 2.933017 > 
mean=`echo $stats | awk -F "(\"*:\"*)" '{ print $5}' | awk '{ print $1}'`
stdev=`echo $stats | awk '{ print $26 }'`
rm speed


lineNum=`expr $loopNum + 1`
echo "=AVERAGE(A2:A$lineNum);=AVERAGE(B2:B$lineNum)" >> collectedUpload.csv
echo "=STDEV(A2:A$lineNum);=STDEV(B2:B$lineNum)" >> collectedUpload.csv
#echo " ;$mean" >> collectedUpload.csv
#echo " ;$stdev" >> collectedUpload.csv

header="fileSize(MBit);MeanSpeed(MBit/sec);StdevSpeed(MBit/sec)"
echo $header > stats.csv
echo "$fileSize;$mean;$stdev" >> stats.csv