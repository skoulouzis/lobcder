#!/bin/bash

        python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U user -K key download LOBCDER-REPLICA-vTEST

#for f in $(ls $1)
#do
#	blobName=$(echo "$1/$f" | awk '{print substr($1,2)}')
#	python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U user -K key download LOBCDER-REPLICA-vTEST $blobName  
#done
