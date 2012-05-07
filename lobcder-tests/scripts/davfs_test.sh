#!/bin/bash


LOBCDER_URL=http://localhost:8080/lobcder-1.0-SNAPSHOT/
MOUNT_POINT=/media/LOBCDER-TEST



USER_NAME1=$USER
HOST1=localhost
PASS1=secret

#Make mount point 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S mkdir $MOUNT_POINT"

#Make davfs configuration dir 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "mkdir ~/.davfs2/" 
#Copy davfs conf file and secrets file 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS | sudo -S cp /etc/davfs2/davfs2.conf /etc/davfs2/secrets ~/.davfs2/" 

#Change permissions 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS | sudo -S sudo chmod 666 ~/.davfs2/secrets" 

#Add the password
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS | sudo -S echo "$MOUNT_POINT $DAV_USER $DAV_PASS">> ~/.davfs2/secrets "
~/.davfs2/secrets

#Change permissions 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS | sudo -S sudo chmod 600 ~/.davfs2/secrets" 


#Mount it 
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S mount -t davfs  $LOBCDER_URL $MOUNT_POINT -o rw,uid=$USER_NAME"

#USER_NAME1=$USER
#HOST2=host.2.com








#HOST1
#Generate files in $HOME 
#Copy (upload) them in $MOUNT_POINT

#HOST2 
#Process data in $MOUNT_POINT
#Delete 

#HOST1
#?
