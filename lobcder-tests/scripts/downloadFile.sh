#!/bin/bash

#example: 
#./downloadFile.sh http://$USER:pass@$HOST:8082/lobcder/dav/ /home/$USER/Downloads/files/ wlan0 multithread

# ----------------------------Parse args---------------------------
srcURL=$1
destPath=$2
interface=$3
multithread=$4

lstChar=`echo "$srcURL" | tail -c 2`
if [ "$lstChar" = "/" ] ;then
  srcURL=`echo "${srcURL%?}"`
fi

proto="`echo $srcURL| grep '://' | sed -e's,^\(.*://\).*,\1,g'`"
url=`echo $srcURL | sed -e s,$proto,,g`
path="`echo $url | grep / | cut -d/ -f2-`"
name=`basename $path`

curl -is -X PROPFIND $srcURL > tmp1
grep -oP '(?<=<d:displayname>)(.*?)(?=</d:displayname>)' tmp1 | grep -oP "^((?!$name).)*$" > tmp2

#----------------------start monitor-----------------------------------
bwm-ng -o csv rate -t 1000 > monitor.csv &
bwmPID=$!
#----------------------start upload-----------------------------------
startNano="$(date +%s%N)"

if [ -n "$multithread" ] && [[ -s tmp2 ]] ; then
  mkdir $destPath
  while read p; do
#     echo "-L --request GET $srcURL/$p -o $destPath/$p 2>/dev/null &"
    curl -L --request GET $srcURL/$p -o $destPath/$p 2>/dev/null &
  done < tmp2
  for job in `jobs -p`
  do
  if [ $job -ne $bwmPID ]; then
    wait $job || let "FAIL+=1"
  fi
  done
  
elif [ -s tmp2 ] ; then
  mkdir $destPath
  while read p; do
#     echo "-L --request GET $srcURL/$p -o $destPath/$p 2>/dev/null"
    curl -L --request GET $srcURL/$p -o $destPath/$p 2>/dev/null
  done < tmp2
else
  curl -L --request GET $srcURL -o $destPath 2>/dev/null
fi


sleep 2
kill $bwmPID

elapsed="$(($(date +%s%N)-$startNano))"
elapsedMil=$(echo "$elapsed / 1000000.0" |bc -l)
elapsedSec=$(echo "$elapsed / 1000000000.0" |bc -l)

startMil=$(echo "$startNano/ 1000000.0" |bc -l)
startSec=$(echo "$startNano / 1000000000.0" |bc -l)


#-----------------------Build report------------------------------------
header="Time(sec);ifaceName;MBitsOut;MBitsIn;MBitsTotal;elapsed(sec);fileSize(MBit);speed(MBit/sec)"

grep $interface monitor.csv > tmp.csv
echo $header > report.csv



if [[ -d $destPath ]]; then
  size=`du -sb $destPath | awk '{print $1}'`
else
  size=$(stat -c%s "$destPath")
fi

fileSize=$(echo "($size * 8) / (1000.0 * 1000.0)" |bc -l)

speed=$(echo "$fileSize / $elapsedSec" |bc -l)


awk  -v s=$startSec -v e=$elapsedSec -v fs=$fileSize -v sp=$speed -F "\"*;\"*" '{ print $1-s ";" $2 ";" ($3*8)/(1000*1000) ";" ($4*8)/(1000*1000) ";" ($5*8)/(1000*1000) ";" e ";" fs ";" sp}'  tmp.csv >> report.csv
rm monitor.csv tmp.csv tmp1 tmp2