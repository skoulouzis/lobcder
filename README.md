## Requirements:
  * Java 7 or higher
  * git 
  * maven 2
  * root access on MySQL 5.5 or higher
  * Tomcat 6 or 7 (will not work with 8)

## Deployment
To deploy the LOBCDER master simply run the deployMaster.sh script. The arguments for the script are:

   -n	Database name. This will hold the logical file system
   
   -p	Database password for the lobcder service user. This is the user that will make the queries for file names, storage backend passwords, replication, etc.
   
   -u	mysql username. The root DB name 
   
   -a	mysql password. The root password 
   
   -l	lobcder admin username
   
   -s	lobcder admin password
   
   -f   Storage site description file. Format: <URI> <USERNAME> <PASSWORD> . 
	Available implmentations: sftp://host:PORT/, swift(ssl)://host:PORT/, webdav(ssl)://host:PORT/, file://host:PORT/, 
	
   -c	Path where tomcat is. The equivelat of $CATALINA_HOME 

## Example
An example running the script is: 
./deployMaster.sh -n lobcderDB -p lobcderDBPass -u root -a mysqlRootPass -f storageFile -l admin -s admin -c ./apache-tomcat-7.0.59

An example for the storage file:
webdavssl://user@test.webdav.org/dav/ user pass
sftp://user@itcsubmit.wustl.edu/ user pass

## Description
This script takes the following steps:
1. Creates the database specified in '-n' in the example  above that is  'lobcderDB'

2. Grands privileges to the lobcder user  named 'lobcder' with the password  specified in '-p' in the example  above that is  'lobcderDBPass'

3. Clones the code from the git reposetory 

4. Compiles the code 

5. Modifies the context.xml and web.xml files and sets the database name and password which in the example above is ' lobcderDB' and ' lobcderDBPass' 

6. Initializes the database by creating the necessary tables and trigger

7. Sets the storage backend locations and credentials according to the storage file set in '-f'

8. Deploys lobcder on tomcat 

After execution is over you'll need to edit the lobcder.properties file. In that file you can find explanations for each property 

## Header
