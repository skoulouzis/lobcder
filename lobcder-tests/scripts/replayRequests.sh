#!/bin/bash


sqlUser=root
sqlPass=rootPass
dbName=dbName
lobUser=admin
lobPass=admin

basePath=$HOME/Downloads/
CATALINA_HOME=servers/apache-tomcat-6.0.36

t0=`mysql -u$sqlUser -p$sqlPass -s -N -e "select unix_timestamp(timeStamp) from $dbName.requests_table limit 1"`
mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent, contentLen from $dbName.requests_table" > /tmp/result.csv


numOfReq=`wc -l < /tmp/result.csv`
#prepere files 
while IFS=$'\t' read method url timestamp userAgent contentLen
do
#   if [ $method = 'GET' ] || [ $method = 'PUT' ] ; then  
  if [ $method = 'PUT' ] ; then  
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    fileName=${url##*/}
    relativePath=${path:27}
    absPath=$basePath/$relativePath
    if [[ ! -f $absPath/$fileName ]]; then
      echo "mkdir -p $absPath"
      mkdir -p $absPath
      echo "dd if=/dev/urandom of=$absPath/$fileName bs=1 count=$(($contentLen/10))"
      dd if=/dev/urandom of=$absPath/$fileName bs=1 count=$(($contentLen/10))
    fi
  fi
     counter=$(( $counter + 1 ))
     tmp=$((100 *  $counter))
     progress=$(($tmp/$numOfReq))
     echo "File: $counter / $numOfReq  Progress: $progress %"
done < /tmp/result.csv




tn=0
counter=0
echo "Method,ContentLen,Elapsed" > report.csv
while IFS=$'\t' read method url timestamp userAgent contentLen
do
    start=$(date +%s%N)
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    fileName=${url##*/}
    relativePath=${path:27}
    absPath=$basePath/$relativePath
     if [ -e $absPath/$fileName ]; then
      path=$absPath
    else
      path=/tmp/$path
     fi
    case "$method" in
    PUT)
	echo "curl -iks -L -u $lobUser:$lobPass -X $method --user-agent '$userAgent' -T $path/$fileName $url  > /dev/null"
	 curl -iks -L -u $lobUser:$lobPass -X $method --user-agent \'"$userAgent"\' -T $path/$fileName $url  > /dev/null
	 ;;
    LOCK)
	echo "curl -iks -L -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent '$userAgent' $url"
	 token=`curl -iks -L -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent \'"$userAgent"\' $url  | grep "Lock-Token: <opaquelocktoken:" 2>&1`
	 mkdir -p /tmp/$path
	 echo "Token:  $token" 
	 echo "Token file: /tmp/$path/$fileName.token"
	 echo $token > /tmp/$path/$fileName.token
	 ;;
    UNLOCK)
	token=`cat /tmp/$path/$fileName.token`
	 echo "curl -iks -L -u $lobUser:$lobPass -X $method --header "$token" --user-agent '$userAgent' $url"
	 curl -iks -L -u $lobUser:$lobPass -X $method --header "$token" --user-agent \'"$userAgent"\' $url
	 ;;
    *) echo "curl -iks -L -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url  > /dev/null"
	curl -iks -L -u $lobUser:$lobPass -X $method --user-agent \'"$userAgent"\' $url > /dev/null
	;;
    esac
    sleepTime=`expr $timestamp - $tn`
    tn=$timestamp
#     echo $sleepTime
    
#      sleep $sleepTime
     counter=$(( $counter + 1 ))
     tmp=$((100 *  $counter))
     progress=$(($tmp / $numOfReq))
     echo "Request: $counter / $numOfReq  Progress: $progress %"
     
     elasped=$((($(date +%s%N) - $start)/1000000))     
     echo "$method,$contentLen,$elasped" >> report.csv
     
done < /tmp/result.csv



# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/juli.2014-05-13.log  &&  grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/juli.2014-05-13.$CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' $CATALINA_HOME/logs/juli.2014-05-13.log
# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' log
