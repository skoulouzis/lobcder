#!/bin/bash

HOST=http://elab.lab.uvalight.net:8081/lobcder-2.1/dav
FILE_PATH=~/tmp/datasets/file10M.dat

echo open $HOST > cadaver.script
echo put $FILE_PATH >> cadaver.script
echo rm $FILE_PATH >> cadaver.script
cadaver < cadaver.script

