#!/bin/bash

#example: 
#./uploadFile.sh  /home/$USER/Downloads/files/ http://$USER:pass@h0.wf.vlan400.uvalight.net:8082/lobcder/dav/ wlan0 multithread

# ----------------------------Parse args---------------------------
srcFilePath=$1
destURL=$2
interface=$3
multithread=$4



#----------------------start monitor-----------------------------------
bwm-ng -o csv rate -t 1000 > monitor.csv &
bwmPID=$!
#----------------------start upload-----------------------------------
startNano="$(date +%s%N)"


if [[ -d $srcFilePath ]]; then        
    files=$srcFilePath/*
    if [ -n "$multithread" ]; then
      for f in $files
      do
	curl -T $f $destURL &
      done
      for job in `jobs -p`
      do
	if [ $job -ne $bwmPID ]; then
# 	  echo "wait for $job"
	  wait $job || let "FAIL+=1"
	fi
      done
    else
      for f in $files
      do
	curl -T $f $destURL
      done
    fi
elif [[ -f $srcFilePath ]]; then
    curl -T $srcFilePath $destURL
else
    echo "$srcFilePath is not valid"
    exit 1
fi

sleep 1.5
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



if [[ -d $srcFilePath ]]; then
  size=`du -sb $srcFilePath | awk '{print $1}'`
else
  size=$(stat -c%s "$srcFilePath")
fi

fileSize=$(echo "($size * 8) / (1000.0 * 1000.0)" |bc -l)

speed=$(echo "$fileSize / $elapsedSec" |bc -l)


awk  -v s=$startSec -v e=$elapsedSec -v fs=$fileSize -v sp=$speed -F "\"*;\"*" '{ print $1-s ";" $2 ";" ($3*8)/(1000*1000) ";" ($4*8)/(1000*1000) ";" ($5*8)/(1000*1000) ";" e ";" fs ";" sp}'  tmp.csv >> report.csv
rm monitor.csv tmp.csv