#!/bin/bash
# Argument = -t test -r server -p password -v

lobGitDir=lobcder
version=v2.5

usage()
{
cat << EOF
usage: $0 options

This script deploys lobcder. It first creates a data base and the necessary tables, 
the clones the project compiles it and copies it to tomcat's webapps

OPTIONS:
   -h	Show this message
   -n	Database name. This will hold the logical file system
   -p	Database password for the lobcder user. The lobcder is the user that will make the queries 
   -u	mysql username. Root if possible
   -a	mysql password
   -l	lobcder admin username
   -s	lobcder admin password
   -f   Storage site description file. Format: <URI> <USERNAME> <PASSWORD>. 
	Available implmentations: sftp://host:PORT/, swift(ssl)://host:PORT/, webdav(ssl)://host:PORT/, file://host:PORT/, 
   -c	Path where tomcat is. The equivelat of \$CATALINA_HOME 

Example: ./deploy.sh  -n lobcderDB -p lobPass -u root -a mysqlPass -f storageFile -l admin -s admin -c ./apache-tomcat-6.0.36/
EOF
}

dbName=
dbPasswd=
sqlUser=
sqlPass=
lobAdmin=
lobPass=
ssFile=
catalinaLocation=
while getopts “hn:p:u:a:l:s:f:c:v” OPTION
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
         l)
             lobAdmin=$OPTARG
             ;;     
         s)
             lobPass=$OPTARG
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

if [[ -z $dbName ]] || [[ -z $dbPasswd ]] || [[ -z $sqlUser ]] || [[ -z $ssFile  ]] || [[ -z $catalinaLocation  ]] || [[ -z $lobAdmin  ]] || [[ -z $lobPass ]]
then
     usage
     exit 1
fi



#  ------------------------- Init DB------------------------------
if [[ -z $sqlPass ]] 
then 
mysql -u$sqlUser -s -N -e  "create database $dbName;"
mysql -u$sqlUser -s -N -e  "GRANT ALL PRIVILEGES on $dbName.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"
mysql -u$sqlUser -s -N -e  "GRANT SUPER ON *.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"
else 
mysql -u$sqlUser -p$sqlPass -s -N -e  "create database $dbName;"
mysql -u$sqlUser -p$sqlPass -s -N -e  "GRANT ALL PRIVILEGES on $dbName.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"
mysql -u$sqlUser -p$sqlPass -s -N -e  "GRANT SUPER ON *.* to lobcder@localhost IDENTIFIED by '$dbPasswd';"
fi




if [ -d "$lobGitDir" ]; then
  cd $lobGitDir git pull
else
    git clone https://github.com/skoulouzis/lobcder.git $lobGitDir
    #git checkout $version
fi



cd $lobGitDir/lobcder-master
mvn install 
rm -r target/lobcder
mv target/lobcder-master-?.? target/lobcder
rm -r target/lobcder/manage*.jsp
cd ../../


# jdbcDefult=url=\"jdbc:mysql:\/\/localhost:3306\/lobcderDB2\?zeroDateTimeBehavior=convertToNull
jdbcDefult=`grep "url=" $lobGitDir/lobcder-master/target/lobcder/META-INF/context.xml`
jdbcDefultPass=`grep "password=" $lobGitDir/lobcder-master/target/lobcder/META-INF/context.xml`

jdbcDBName=url=\"jdbc:mysql:\/\/localhost:3306\/$dbName\?zeroDateTimeBehavior=convertToNull\"
jdbcPass="password=\"$dbPasswd\""

sed -i "s#<res-ref-name>jdbc\/lobcderDB2<\/res-ref-name>#<res-ref-name>jdbc\/$dbName<\/res-ref-name>#g" $lobGitDir/lobcder-master/target/lobcder/WEB-INF/web.xml
sed -i "s#$jdbcDefult#$jdbcDBName#g" $lobGitDir/lobcder-master/target/lobcder/META-INF/context.xml
sed -i "s#$jdbcDefultPass#$jdbcPass#g" $lobGitDir/lobcder-master/target/lobcder/META-INF/context.xml


#  --------------------- Build tables trigers and storage sites  --------------------
mysql --user=lobcder --password=$dbPasswd $dbName < $lobGitDir/lobcder-master/target/lobcder/WEB-INF/classes/init.sql
while read uri username pass
do
  echo Adding $username":"$pass"@"$uri
  if [[ -z $sqlPass ]] 
  then 
    mysql -u$sqlUser -p$sqlPass -s -N -e  "INSERT INTO  $dbName.credential_table(username, password) VALUES ('$username', '$pass');"
  else
    mysql -u$sqlUser -s -N -e "INSERT INTO  $dbName.credential_table(username, password) VALUES ('$username', '$pass');"
  fi
  SET @credRef = LAST_INSERT_ID();
  INSERT INTO $dbName.storage_site_table(resourceUri, credentialRef, currentNum, currentSize, quotaNum, quotaSize, isCache) VALUES('$uri', @credRef, -1, -1, -1, -1, FALSE);"
done < $ssFile


  if [[ -z $sqlPass ]] 
    then
        mysql -u$sqlUser -s -N -e  "INSERT INTO $dbName.auth_usernames_table(token, uname) VALUES ('$lobAdmin', '$lobPass');"
    else
        mysql -u$sqlUser -p$sqlPass -s -N -e  "INSERT INTO $dbName.auth_usernames_table(token, uname) VALUES ('$lobAdmin', '$lobPass');"
    fi

SET @authUserNamesRef = LAST_INSERT_ID();
INSERT INTO $dbName.auth_roles_tables(roleName, unameRef) VALUES  ('admin',     @authUserNamesRef),
                                                          ('other',     @authUserNamesRef),
                                                          ('megarole',  @authUserNamesRef);"


cp -r $lobGitDir/lobcder-master/target/lobcder $catalinaLocation/webapps/


echo "-------------------------------------------------------"
echo "If everything went OK look at $catalinaLocation/webapps/lobcder/WEB-INF/classes/lobcder.properties" 