#!/bin/bash

sqlUser=root
sqlPass=pass
dbName=DB
replicaLocation=PATH

ls $replicaLocation > fileNamesToReplcate

#echo "CREATE TEMPORARY TABLE tmptable_1 SELECT * FROM pdri_table where" > tmpQuery
echo "SELECT * FROM pdri_table where" > tmpQuery

while read p; do
  echo fileName = \"$p\" >> tmpQuery
  echo or >> tmpQuery
done < fileNamesToReplcate

head -n -1 tmpQuery > query
echo ";" >> query
rm tmpQuery 

mysql -u$sqlUser -p$sqlPass -s -N $dbName < query
#mysql -u$sqlUser -p$sqlPass -s -N -e  "select methodName, requestURL, unix_timestamp(timeStamp)-$t0, userAgent, contentLen from $dbName.requests_table" > /tmp/result.csv