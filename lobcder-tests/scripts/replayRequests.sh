#!/bin/bash



sqlUser=root
sqlPass=pass
dbName=lobcderDB2
lobUser=user
lobPass=pass

basePath=$HOME/Downloads/
servers/apache-tomcat-6.0.36=CATALINA_HOME

t0=`mysql -u$sqlUser -p$sqlPass -s -N -e "select unix_timestamp(timeStamp) from $dbName.requests_table limit 1"`
mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent, contentLen from $dbName.requests_table" > /tmp/result.csv


#prepere files 
while IFS=$'\t' read method url timestamp userAgent contentLen
do
  if [ $method = 'PUT' ]; then  
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    fileName=${url##*/}
    relativePath=${path:27}
    absPath=$basePath/$relativePath
    if [[ ! -f $absPath/$fileName ]]; then
      echo "mkdir -p $path"
      mkdir -p $path
      echo "dd if=/dev/urandom of=/tmp/$path/$fileName bs=1 count=$contentLen"
      dd if=/dev/urandom of=/tmp/$path/$fileName bs=1 count=$contentLen
    fi
    
  fi
done < /tmp/result.csv

numOfReq=`wc -l < /tmp/result.csv`


tn=0
while IFS=$'\t' read method url timestamp userAgent contentLen
do
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
	echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' -T $path/$fileName $url  > /dev/null"
	 curl -iks -u $lobUser:$lobPass -X $method --user-agent \'"$userAgent"\' -T $path/$fileName $url  > /dev/null
	 ;;
    LOCK)
	echo "curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent '$userAgent' $url"
	 token=`curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent \'"$userAgent"\' $url  | grep "Lock-Token: <opaquelocktoken:" 2>&1`
	 mkdir -p /tmp/$path
	 echo "Token:  $token" 
	 echo "Token file: /tmp/$path/$fileName.token"
	 echo $token > /tmp/$path/$fileName.token
	 ;;
    UNLOCK)
	token=`cat /tmp/$path/$fileName.token`
	 echo "curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent '$userAgent' $url"
	 curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent \'"$userAgent"\' $url
	 ;;
    *) echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url  > /dev/null"
	curl -iks -u $lobUser:$lobPass -X $method --user-agent \'"$userAgent"\' $url > /dev/null
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
done < /tmp/result.csv



# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/juli.2014-05-13.log  &&  grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/juli.2014-05-13.$CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' $CATALINA_HOME/logs/juli.2014-05-13.log
# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' log
