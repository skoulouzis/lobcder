#!/bin/bash


LOBCDER_URL=http://localhost:8080/lobcder-1.0-SNAPSHOT/
MOUNT_POINT=/media/LOBCDER-LOCAL
USER_NAME=$USER


sudo mkdir /media/LOBCDER
sudo mount -t davfs  $LOBCDER_URL $MOUNT_POINT -o rw,uid=$USER_NAME


#HOST1
#Generate files in $HOME 
#Copy (upload) them in $MOUNT_POINT

#HOST2 
#Process data in $MOUNT_POINT
#Delete 

#HOST1
#?
