#!/bin/bash



sqlUser=root
sqlPass=pass
dbName=lobcderDB2
lobUser=user
lobPass=pass


#  

t0=`mysql -u$sqlUser -p$sqlPass -s -N -e "select unix_timestamp(timeStamp) from $dbName.requests_table limit 1"`
mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent, contentLen from $dbName.requests_table" > result.csv


#prepere files 
while IFS=$'\t' read method url timestamp userAgent contentLen
do
  if [ $method = 'PUT' ]; then
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    echo "mkdir -p $path"
    mkdir -p $path
    fileName=${url##*/}
    dd if=/dev/urandom of=$path/$fileName bs=1 count=$contentLen
  fi
done < result.csv


tn=0
while IFS=$'\t' read method url timestamp userAgent contentLen
do
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    fileName=${url##*/}
    case "$method" in
    PUT) echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' -T $path/$fileName $url  > temp.html"
	 curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' -T $path/$fileName $url  > temp.html
	 ;;
    LOCK) echo "curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent '$userAgent' $url"
	 token=`curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent '$userAgent' $url  | grep "Lock-Token: <opaquelocktoken:" 2>&1`
	 mkdir -P $path
	 echo $token > $path/$fileName.token
	 ;;
    UNLOCK) token=`cat $path/$fileName.token`
	 echo "curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent '$userAgent' $url"
	 curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent '$userAgent' $url
	 ;;
    *) 	echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url  > temp.html"
	curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url > temp.html
	;;
    esac
    
    sleepTime=`expr $timestamp - $tn`
    tn=$timestamp
    echo $sleepTime
    
#     sleep $sleepTime
done < result.csv