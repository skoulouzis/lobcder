#!/bin/bash

N=1

for i in 10 40 160 640
do
  dataSet=/tmp/testDatasets/$i/
  #echo $dataSet
  
  fileSize=$(ls -lh $dataSet/file0.dat | awk '{print($5)}')
  uploadResFile=$HOME/workspace/lobcder-tests/measures/elab.lab.uvalight.net/swift_script/upload/swift_"$i"_"$fileSize"

  downloadResFile=$HOME/workspace/lobcder-tests/measures/elab.lab.uvalight.net/swift_script/download/swift_"$i"_"$fileSize"

  for (( c=0; c<=$N; c++ ))
  do
	  #/usr/bin/time -f '%e' -o up  ./uploadSwiftFile.sh $dataSet
	  #cat up >> $uploadResFile 
	  /usr/bin/time -f '%e' -o down  ./downloadSwiftFile.sh $dataSet
	  cat down >> $downloadResFile 
  done

done
