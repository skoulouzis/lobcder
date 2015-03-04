## Requirements:
  *Item Java 7 or higher
  *Item git 
  *Item maven 2
  *Item root access on MySQL 5.5 or higher
  *Item Tomcat 6 or 7 (will not work with 8)

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
	1. Step creates the database specified in '-n' in the example  above that is  'lobcderDB'
	2. Step grands privileges to the lobcder user  named 'lobcder' with the password  specified in '-p' in the example  above that is  'lobcderDBPass'
	3. Step clones the code from the git reposetory 
	4. Step compiles the code 
	5. Step modifies the context.xml and web.xml files and sets the database name and password which in the example above is ' lobcderDB' and ' lobcderDBPass' 
	6. Step initializes the database by creating the necessary tables and triggers
	7. Step sets the storage backend locations and credentials according to the storage file set in '-f' 
	8. Step deploys lobcder on tomcat 

After execution is over you'll need to edit the lobcder.properties file. In that file you can find explanations for each property 
