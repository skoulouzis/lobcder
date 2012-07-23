#!/bin/bash
resFolder=$1
# ls measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/upload/swift_10\ _and_40_files_3200k*
for f in `ls $resFolder/*.csv`
do 
  cat $f >> OUT.csv
done 