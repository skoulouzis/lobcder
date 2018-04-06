#!/bin/bash

echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886
apt-get update

export DEBIAN_FRONTEND=noninteractive
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
apt-get -q -y --force-yes  install oracle-java7-installer git maven2 erlang wget makepasswd nmap openvpn

password="pass"
pass=$(perl -e 'print crypt($ARGV[0], "password")' $password)
useradd w -m -p $pass


wget http://apache.mirror.triple-it.nl/tomcat/tomcat-6/v6.0.44/bin/apache-tomcat-6.0.44.tar.gz
tar -xavf apache-tomcat-6.0.44.tar.gz
catalina=apache-tomcat-6.0.44

./$catalina/bin/shutdown.sh
git clone https://github.com/skoulouzis/lobcder.git
cd lobcder/lobcder-worker/
mvn install 
rm -r target/lobcder-worker
mv target/lobcder-worker-?.?-SNAPSHOT target/lobcder-worker
cd 
cp -r lobcder/lobcder-worker/target/lobcder-worker $catalina/webapps/
./$catalina/bin/startup.sh


#sleep 70 
#for i in {1..6}; do ping -c 1 192.168.100.$i; done


#for i in {1..7}; do wget http://localhost:8080/lobcder-worker/2/1; ./$catalina/bin/shutdown.sh; ./$$catalina/bin/startup.sh;  done



