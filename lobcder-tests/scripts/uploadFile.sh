#!/bin/bash

#example: 
#./uploadFile.sh  /home/$USER/Downloads/files/ http://$USER:pass@h0.wf.vlan400.uvalight.net:8082/lobcder/dav/ wlan0 multithread

# ----------------------------Parse args---------------------------
srcFilePath=$1
destURL=$2
interface=$3
multithread=$4


# extract the protocol
proto="`echo $destURL | grep '://' | sed -e's,^\(.*://\).*,\1,g'`"
# remove the protocol
url=`echo $destURL| sed -e s,$proto,,g`

# extract the user and password (if any)
userpass="`echo $url | grep @ | cut -d@ -f1`"
pass=`echo $userpass | grep : | cut -d: -f2`
if [ -n "$pass" ]; then
    user=`echo $userpass | grep : | cut -d: -f1`
else
    user=$userpass
fi

# extract the host -- updated
hostport=`echo $url | sed -e s,$userpass@,,g | cut -d/ -f1`
port=`echo $hostport | grep : | cut -d: -f2`
if [ -n "$port" ]; then
    host=`echo $hostport | grep : | cut -d: -f1`
else
    host=$hostport
fi

# extract the path (if any)
path="`echo $url | grep / | cut -d/ -f2-`"


echo "machine $host" > $HOME/.netrc
echo "	login $user" >> $HOME/.netrc
echo "	password $pass" >> $HOME/.netrc
chmod 600 $HOME/.netrc


# --------------------------check if we upload file or directory ---------------------
if [[ -d $srcFilePath ]]; then
    #echo "$srcFilePath is a directory"
        
    files=$srcFilePath/*
    if [ -n "$multithread" ]; then
    #echo "it's multithread"
      i=0
      for f in $files
      do
	  i=`expr $i + 1` 
	  echo open $destURL > cadaver$i.script
	  echo put $f  >> cadaver$i.script
	  echo quit >>  cadaver$i.script
      done
    else
      echo open $destURL > cadaver.script
      for f in $files
      do
	  echo put $f  >> cadaver.script
      done
      echo quit >> cadaver.script
    fi
    
elif [[ -f $srcFilePath ]]; then
    #echo "$srcFilePath is a file"
    echo open $destURL > cadaver.script
    echo put $srcFilePath >> cadaver.script
    echo quit >> cadaver.script
else
    echo "$srcFilePath is not valid"
    exit 1
fi


#----------------------start monitor-----------------------------------
bwm-ng -o csv rate -t 1000 > monitor.csv &
bwmPID=$!
#----------------------start upload-----------------------------------
#start="$(date +%s)"
startNano="$(date +%s%N)"


if [ -n "$multithread" ] && [[ -d $srcFilePath ]] ; then
  #echo "it's multithread"
  i=0
  for f in $files
  do
    i=`expr $i + 1` 
    /usr/bin/time --format="%e" cadaver < cadaver$i.script >/dev/null &
    #time cadaver < cadaver$i.script &
    cadaverPID[$i]=$!
  done
  

  for job in `jobs -p`
  do
    if [ $job -ne $bwmPID ]; then
        wait $job || let "FAIL+=1"
    fi
  done
else
   /usr/bin/time --format="%e" cadaver < cadaver.script >/dev/null
  #time cadaver < cadaver.script
fi


elapsed="$(($(date +%s%N)-$startNano))"
elapsedMil=$(echo "$elapsed / 1000000.0" |bc -l)
#elapsedMil="$(($elapsed/1000000))"
elapsedSec=$(echo "$elapsed / 1000000000.0" |bc -l)
#echo "$elapsed  1000000000.0" | awk '{printf "%f", $1 / $2}'
#elapsedSec="$(($elapsed/1000000000))"
#end="$(date +%s)"

startMil=$(echo "$startNano/ 1000000.0" |bc -l)
#elapsedMil="$(($elapsed/1000000))"
startSec=$(echo "$startNano / 1000000000.0" |bc -l)

sleep 1.5
kill $bwmPID
#elapsed="$(expr $end - $start)"




#-----------------------Build report------------------------------------
# StartTime(sec);EndTime(sec);Elapsed(sec);
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
rm monitor.csv tmp.csv cadaver*.script