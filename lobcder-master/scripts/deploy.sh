#!/bin/bash
# Argument = -t test -r server -p password -v

usage()
{
cat << EOF
usage: $0 options

This script run the test1 or test2 over a machine.

OPTIONS:
   -h	Show this message
   -n	Database name. This will hold the logical file system
   -p	Database password for the lobcder user. The lobcder is the user that will make the queries 
   -u	mysql username. Root if possible
   -a	mysql password
   -f   Storage site description file. Format: <URI> <USERNAME> <PASSWORD>. 
	Available implmentations: sftp://host:PORT/, swift(ssl)://host:PORT/, webdav(ssl)://host:PORT/, file://host:PORT/, 
   -c	Path where tomcat is. The equivelat of \$CATALINA_HOME 
EOF
}

dbName=
dbPasswd=
sqlUser=
sqlPass=
ssFile=
while getopts “hn:p:u:a:f:c:v” OPTION
do
     case $OPTION in
         h)
             usage
             exit 1
             ;;
         n)
             dbName=$OPTARG
             ;;
         p)
             dbPasswd=$OPTARG
             ;;
         u)
             sqlUser=$OPTARG
             ;;           
         a)
             sqlPass=$OPTARG
             ;;                 
         f)
             ssFile=$OPTARG
             ;;
         c)
             catalinaLocation=$OPTARG
             ;;             
         ?)
             usage
             exit
             ;;
     esac
done

if [[ -z $dbName ]] || [[ -z $dbPasswd ]] || [[ -z $sqlUser ]] || [[ -z $sqlPass ]] || [[ -z $ssFile  ]] || [[ -z $catalinaLocation  ]]
then
     usage
     exit 1
fi



#  ------------------------- Init DB------------------------------
mysql -u$sqlUser -p$sqlPass -s -N -e  "create database $dbName;"
mysql -u$sqlUser -p$sqlPass -s -N -e  "GRANT ALL PRIVILEGES on $dbName.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"
mysql -u$sqlUser -p$sqlPass -s -N -e  "GRANT SUPER ON *.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"



git clone https://github.com/skoulouzis/lobcder.git
cd lobcder/lobcder-master
mvn install 
rm -r target/lobcder
mv target/lobcder-master-2.4 target/lobcder
rm -r target/lobcder/manage*.jsp
cd ../../
sed -i "s#<res-ref-name>jdbc\/lobcderDB2<\/res-ref-name>#<res-ref-name>jdbc\/$dbName<\/res-ref-name>#g" lobcder/lobcder-master/target/lobcder/WEB-INF/web.xml
sed -i "s#url=\"jdbc:mysql:\/\/localhost:3306\/lobcderDB2\"#url=\"jdbc:mysql:\/\/localhost:3306\/$dbName\"#g" lobcder/lobcder-master/target/lobcder/META-INF/context.xml
sed -i "s#password=\"RoomC3156\"#password=\"$dbPasswd\"#g" lobcder/lobcder-master/target/lobcder/META-INF/context.xml


#  --------------------- Build tables trigers and storage sites  --------------------
mysql --user=lobcder --password=$dbPasswd $dbName < lobcder/lobcder-master/target/lobcder/WEB-INF/classes/init.sql
# while read uri username pass
# do
#   echo Adding $username":"$pass"@"$uri
#   mysql -u$sqlUser -p$sqlPass -s -N -e  "INSERT INTO  $dbName.credential_table(username, password) VALUES ('$username', '$pass'); 
#   SET @credRef = LAST_INSERT_ID();
#   INSERT INTO $dbName.storage_site_table(resourceUri, credentialRef, currentNum, currentSize, quotaNum, quotaSize, isCache) VALUES('$uri', @credRef, -1, -1, -1, -1, TRUE);"
# done < $ssFile
# 
# 
# 
# cp -r lobcder/lobcder-master/target/lobcder $catalinaLocation/webapps/