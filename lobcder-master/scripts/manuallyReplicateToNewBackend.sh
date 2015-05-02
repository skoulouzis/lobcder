#!/bin/bash

replicaLocation=/home/alogo/Downloads/STORAGE/LOBCDER-REPLICA-SANDBOX

ls $replicaLocation > fileNamesToReplcate

echo "CREATE TEMPORARY TABLE tmptable_1 SELECT * FROM $dbName.pdri_table where" > tmpQuery
#echo "SELECT * FROM pdri_table where" > tmpQuery

while read p; do
  echo fileName = \"$p\" >> tmpQuery
  echo or >> tmpQuery
done < fileNamesToReplcate

head -n -1 tmpQuery > query
echo ";" >> query
rm tmpQuery 

#mysql -h localhost  -u$sqlUser -p$sqlPass -s lobcderDB2 < query
#mysql -u$sqlUser -p$sqlPass -s -N -e "ALTER TABLE $dbName.tmptable_1 drop pdriId;"
#mysql -u$sqlUser -p$sqlPass -s -N -e  $dbName "UPDATE $dbName.tmptable_1 SET storageSiteRef = $storageSiteID;"
#mysql -u$sqlUser -p$sqlPass -s -N -e $dbName "INSERT INTO $dbName.pdri_table SELECT 0,$dbName.tmptable_1.* FROM $dbName.tmptable_1;"
#mysql -u$sqlUser -p$sqlPass -s -N -e  $dbName "DROP TEMPORARY TABLE IF EXISTS $dbName.tmptable_1;"