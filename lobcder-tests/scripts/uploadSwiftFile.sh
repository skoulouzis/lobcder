#!/bin/bash

echo $1
for f in $(ls $1)
do

	python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U user -K key upload LOBCDER-REPLICA-vTEST $1/$f 
#  cat $1/$f
  
done
