#!/bin/bash



sqlUser=root
sqlPass=pass
dbName=DDName
lobUser=user
lobPass=pass

basePath=$HOME/Downloads/
CATALINA_HOME=apache-tomcat-6.0.36

t0=`mysql -u$sqlUser -p$sqlPass -s -N -e "select unix_timestamp(timeStamp) from $dbName.requests_table limit 1"`
mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent, contentLen from $dbName.requests_table" > /tmp/result.csv


#prepere files
counter=0
counter1=0
while IFS=$'\t' read method url timestamp userAgent contentLen
do
  if [ $method = 'PUT' ]; then
    let counter1=counter1+1
    echo "Reached "$counter1
    xpath=${url%/*}
    path=`echo $xpath | sed "s/^http:\/\///g"`
    fileName=${url##*/}
    relativePath=${path:27}
    absPath=$basePath/$relativePath
    if [[ ! -f $absPath/$fileName ]]; then
      echo "mkdir -p /tmp/$path"
      mkdir -p /tmp/$path
#       contentLen=$contentLen/2
      echo "dd if=/dev/urandom of=/tmp/$path/$fileName bs=1 count=$contentLen"
#       dd if=/dev/urandom of=/tmp/$path/$fileName bs=1 count=$contentLen  &
#       dd if=/dev/zero of=/tmp/$path/$fileName bs=1 count=$contentLen  &
     touch /tmp/$path/$fileName &
      let counter=counter+1
      if [  $counter -gt 5 ]; then
	echo "Wait for threads. Reached "$counter
	wait
	counter=0
      fi
    fi
  fi
done < /tmp/result.csv

wait

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
# 	echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' -T $path/$fileName $url  > /dev/null"
	 curl -iks -u $lobUser:$lobPass -X $method --user-agent \'"$userAgent"\' -T $path/$fileName $url  > /dev/null
	 ;;
    LOCK)
# 	echo "curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent '$userAgent' $url"
	 token=`curl -iks -u $lobUser:$lobPass -X $method --data @lock.xml --user-agent \'"$userAgent"\' $url  | grep "Lock-Token: <opaquelocktoken:" 2>&1`
	 mkdir -p /tmp/$path
	 echo "Token:  $token" 
	 echo "Token file: /tmp/$path/$fileName.token"
	 echo $token > /tmp/$path/$fileName.token
	 ;;
    UNLOCK)
	token=`cat /tmp/$path/$fileName.token`
# 	 echo "curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent '$userAgent' $url"
	 curl -iks -u $lobUser:$lobPass -X $method --header "$token" --user-agent \'"$userAgent"\' $url
	 ;;
    *) 
# 	echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url  > /dev/null"
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



#Create dumy files from DB:
# mysql -u root -p -s -N -e  "select fileName from lobcderDB2.pdri_table;" > /tmp/files  
#Got to the storage site and run: 
# cat /tmp/files | xargs touch --
# Set the size so it works:
#UPDATE lobcderDB2.ldata_table SET `ldLength` = 0 WHERE datatype = 'logical.file';

#Build successor graph:
# SELECT 
#     f.requestURL AS from_ID,
#     t.requestURL AS to_ID
# FROM
#     requests_table AS f 
# JOIN requests_table AS t
#     ON f.uid = t.uid-1 
# WHERE
#     TIMESTAMPDIFF(SECOND, f.timeStamp, t.timeStamp) < 300
# ;

#or:
# mysql -u root -p -s -N -e  "SELECT f.requestURL AS from_ID, t.methodName AS trans,  t.requestURL AS to_ID FROM lobcderDB2.requests_table AS f JOIN lobcderDB2.requests_table AS t ON f.uid = t.uid-1  WHERE  t.methodName = 'GET';" > /tmp/files


# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/juli.2014-05-13.log  &&  grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/juli.2014-05-13.$CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' $CATALINA_HOME/logs/juli.2014-05-13.log
# grep -c 'Prediction Result:' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Hit' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Miss' $CATALINA_HOME/logs/*.log && grep -c 'Prediction Result: Non' log

# cat catalina.out | grep 'Prediction Result: Hit' | grep -c "GET" && cat catalina.out | grep 'Prediction Result: Miss' | grep -c "GET" && cat catalina.out | grep 'Prediction Result: Non' | grep -c "GET" &&  cat catalina.out | grep 'Prediction Result:' | grep -c "GET"
