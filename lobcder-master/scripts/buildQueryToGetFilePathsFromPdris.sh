#!/bin/bash

echo "select distinct(ldata_table.uid) from pdri_table JOIN ldata_table ON ldata_table.pdriGroupRef = pdri_table.pdriGroupRef where " > tmpQuery

while read p; do
  echo pdri_table.fileName = \"$p\" >> tmpQuery
  echo or >> tmpQuery
done < $1

head -n -1 tmpQuery > query
echo ";" >> query
rm tmpQuery 