#!/bin/bash

# example ./measureUploads.sh ./arch.tar eth0

loopNum=5
srcURL=$1  #http://$USER:PASS@$HOST:8082/lobcder/dav/dir
dest=$2
iface=$3
multithread=$4


lstChar=`echo "$srcURL" | tail -c 2`
if [ "$lstChar" = "/" ] ;then
  srcURL=`echo "${srcURL%?}"`
fi

for ((i = 0 ; i < $loopNum ; i++)); do
  if [ -n "$multithread" ]; then
    ./downloadFile.sh $srcURL $dest $iface mmm
  else
    ./downloadFile.sh $srcURL $dest $iface
  fi
  mv report.csv report-$i.csv
done


if [[ -d $dest ]]; then
  size=`du -sb $dest | awk '{print $1}'`
else
  size=$(stat -c%s "$dest")
fi

fileSize=$(echo "($size * 8) / (1000 * 1000)" |bc -l)


header="fileSize(MBit);speed(MBit/sec)"
echo $header > collectedDownload.csv
sum=0;
for f in `ls report-*.csv`
do
    sed -n 2p $f | awk -F "\"*;\"*" '{ print $7 ";" $8 }' >> collectedDownload.csv
    sed -n 2p $f | awk -F "\"*;\"*" '{ print $8 }' >> speed
done 


stats=`R -q -e "x <- read.csv('speed', header = F); summary(x); sd(x[ , 1])"`
#echo $stats
#> x <- read.csv('speed', header = F); summary(x); sd(x[ , 1]) V1 Min. :50.16 1st Qu.:51.19 Median :52.23 Mean :52.23 3rd Qu.:53.27 Max. :54.30 [1] 2.933017 > 
mean=`echo $stats | awk -F "(\"*:\"*)" '{ print $5}' | awk '{ print $1}'`
stdev=`echo $stats | awk '{ print $26 }'`
rm speed


lineNum=`expr $loopNum + 1`
echo "=AVERAGE(A2:A$lineNum);=AVERAGE(B2:B$lineNum)" >> collectedDownload.csv
echo "=STDEV(A2:A$lineNum);=STDEV(B2:B$lineNum)" >> collectedDownload.csv
#echo " ;$mean" >> collectedDownload.csv
#echo " ;$stdev" >> collectedDownload.csv

header="fileSize(MBit);MeanSpeed(MBit/sec);StdevSpeed(MBit/sec)"
echo $header > stats.csv
echo "$fileSize;$mean;$stdev" >> stats.csv

rm -r $dest