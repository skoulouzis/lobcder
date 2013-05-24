#!/bin/bash

HOST=http://elab.lab.uvalight.net:8081/lobcder-2.1/dav
FILENAME=file10M.dat
FILE_PATH=~/tmp/datasets/$FILENAME

echo open $HOST > cadaver.script
echo put $FILE_PATH >> cadaver.script
echo rm $FILENAME >> cadaver.script
cadaver < cadaver.script

