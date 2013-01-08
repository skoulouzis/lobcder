#!/bin/bash
resFolder=$1

# resFolder=/home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-2.0-SNAPSHOT/dav/

for f in `ls $resFolder/bwm-*-*.csv-*.csv`
do
  fname=$(basename $f ".csv")
#   echo $fname
#   echo $fname "\n"
#   echo $fname | awk -F "\"*-\"*" '{printf $3 "*" $3 "*" $4}' 
  var=`echo $fname | awk -F "\"*-\"*" '{printf $3}'`
#   echo $var
  if [ "$var" = "dataset" ] ;
  then 
    tmp=`echo $fname | awk -F "\"*-\"*" '{printf $2 "-" $3 "-" $4}'`
  else
    tmp=`echo $fname | awk -F "\"*-\"*" '{printf $2 "-" $3 "-"}'`
  fi
  header=$(basename $tmp ".csv")
#   echo $header
  sed '10q;d' $f >> $header.tmp
  sed 's/$/;/g' $header.tmp > $header-1.tmp
  sed 's/;;;/;/g' $header-1.tmp > $header-2.tmp
  sed 's/;;/;/g' $header-2.tmp > $header-3.tmp
  awk -F "\"*;\"*" '{print $1}' $header-3.tmp > $header-4.tmp
  mv $header-4.tmp $header.tmp
  rm $header-1.tmp $header-2.tmp  $header-3.tmp $header-4.tmp

done


for f in `ls *.tmp`
do
   echo $f
  fname=$(basename $f ".csv-.tmp")
#   echo $fname
  h=$h$fname";"
  h2=$h2' <(cut -d, -f1 '$f')'	
done

echo $h > out.csv
# echo $h2
cmd='paste -d, '$h2
eval ${cmd} >> out.csv

mv out.csv $resFolder/
 

# echo -e $allHeadr >> FILE.csv
# echo -e $var2  >> FILE.csv

# tr ' ' '\n' < FILE.csv > FILE2.csv

rm *.tmp