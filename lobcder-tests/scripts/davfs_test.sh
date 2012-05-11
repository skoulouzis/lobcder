#!/bin/bash


LOBCDER_URL=http://elab.lab.uvalight.net:8080/lobcder-1.0-SNAPSHOT
MOUNT_POINT=/media/LOBCDER-TEST
# MOUNT_POINT=/media/webdav.elab/skoulouz/
DAV_USER=davuser
DAV_PASS=dav_secret
DIRNAME=lobcderTest
TMP_TEST_DIR=/tmp/$DIRNAME


USER_NAME1=skoulouz
HOST1=localhost
PASS1=hondos
#------------------------------for client 1 -------------------------------------
#Make mount point 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S mkdir $MOUNT_POINT"

# #Make davfs configuration dir 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "mkdir ~/.davfs2/" 
# #Copy davfs conf file
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "cp /etc/davfs2/davfs2.conf ~/.davfs2/" 
# 
# #Copy davfs conf file and secrets file 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S cp /etc/davfs2/davfs2.conf /etc/davfs2/secrets ~/.davfs2/" 
# 
# #Change permissions 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S sudo chmod 666 ~/.davfs2/secrets" 
# 
# #Add the password
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S echo "$MOUNT_POINT $DAV_USER $DAV_PASS">> ~/.davfs2/secrets "
# ~/.davfs2/secrets
# 
# #Change permissions & owner
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S chmod 600 ~/.davfs2/secrets" 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S chown $USER_NAME1 ~/.davfs2/secrets"

#Mount it 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $DAV_PASS | sudo mount -t davfs -o usdername=$DAV_USER $LOBCDER_URL $MOUNT_POINT -o rw,uid=$USER_NAME1"


# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "ls $MOUNT_POINT"

#unmount it 
# sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "echo $PASS1 | sudo -S umount $MOUNT_POINT"

      

#HOST1
#Generate files in $HOME 
#Copy (upload) them in $MOUNT_POINT
echo "Make the dir"
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "mkdir $MOUNT_POINT/$DIRNAME"

START_TIME=$(date +%s)
#Download some data
sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "wget https://www.biomedtown.org/biomed_town/vphshare/reception/public_repository/FactsFP7_VPH-Share_1v2.pdf -O $MOUNT_POINT/$DIRNAME/ShareFacts.pdf"
END_TIME=$(date +%s)
echo "----------------->$HOST1: Prestage time: $((END_TIME - START_TIME)) secs."


echo "Copy them in the mount folder. Something like process"
START_TIME=$(date +%s)
for ((a=1; a <= 10 ; a++))
do
  sshpass -p "$PASS1" ssh $USER_NAME1@$HOST1 "cp $MOUNT_POINT/$DIRNAME/ShareFacts.pdf $MOUNT_POINT/$DIRNAME/ShareFacts$a.pdf"
done
END_TIME=$(date +%s)
echo "----------------->$HOST1 Process time: $((END_TIME - START_TIME)) secs."

#HOST2 
#Process data in $MOUNT_POINT
#Delete 
USER_NAME2=skoulouzis
HOST2=149.156.10.138
PASS2=pass@Amstel

echo "Process pdf"
START_TIME=$(date +%s)
sshpass -p "$PASS2" ssh $USER_NAME2@$HOST2 "gs -q -dNOPAUSE -sDEVICE=pdfwrite -sOUTPUTFILE=$MOUNT_POINT/$DIRNAME/combinedpdf.pdf -dBATCH $MOUNT_POINT/$DIRNAME/*.pdf"
END_TIME=$(date +%s)
echo "----------------->$HOST2 Process time: $((END_TIME - START_TIME)) secs."

echo "Clean up"
sshpass -p "$PASS2" ssh $USER_NAME2@$HOST2 "rm -r $MOUNT_POINT/$DIRNAME"

#HOST1
#?
