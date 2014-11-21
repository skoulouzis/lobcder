#!/bin/bash


for (( c=1; c<=256; c=c*2 ))
do
  $HOME/servers/apache-tomcat-6.0.36/bin/shutdown.sh
  sleep 10
  killall -9 java 
  $HOME/servers/apache-tomcat-6.0.36/bin/startup.sh
  sleep 20
  ls $HOME/.tsung/tsung_$cUsers_600k.xml
#   tsung  -f $HOME/.tsung/tsung_$cUsers_600k.xml -i 1 start 
done