#!/bin/bash



sqlUser=root
sqlPass=pass
dbName=lobcderDB2
lobUser=user
lobPass=pass


#  

t0=`mysql -u$sqlUser -p$sqlPass -s -N -e "select unix_timestamp(timeStamp) from $dbName.requests_table limit 1"`
mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent from $dbName.requests_table limit 50" > result.csv


tn=0
while IFS=$'\t' read method url timestamp userAgent
do
    echo "curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url  > temp.html"
    curl -iks -u $lobUser:$lobPass -X $method --user-agent '$userAgent' $url > temp.html
    sleepTime=`expr $timestamp - $tn`
    tn=$timestamp
    echo $sleepTime
    
#     sleep $sleepTime
done < result.csv