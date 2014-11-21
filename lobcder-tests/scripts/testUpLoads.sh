#!/bin/bash

host=https://elab.lab.uvalight.net:8081/lobcder/dav/
filename=Tx.out    
filePath=/home/alogo/Downloads/logs/$filename       #~/tmp/datasets/$FILENAME
username=alogo
password=hondos

# echo open $HOST > cadaver.script
# echo put $FILE_PATH >> cadaver.script
# echo rm $FILENAME >> cadaver.script
# cadaver < cadaver.script


curl -u $username:$password -k -T $filePath $host/$filename