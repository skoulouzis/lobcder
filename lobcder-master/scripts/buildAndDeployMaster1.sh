#!/bin/bash

echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu precise main" | tee -a /etc/apt/sources.list
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys EEA14886
apt-get update
apt-get upgrade

export DEBIAN_FRONTEND=noninteractive
echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections

apt-get -q -y --force-yes  install mysql-server mysql-client oracle-java7-installer git maven2 erlang wget makepasswd nmap bwm-ng

cd 


wget http://apache.mirror.triple-it.nl/tomcat/tomcat-6/v6.0.44/bin/apache-tomcat-6.0.44.tar.gz
tar -xavf apache-tomcat-6.0.44.tar.gz
catalina=apache-tomcat-6.0.44

wget https://raw.githubusercontent.com/skoulouzis/lobcder/dev/lobcder-master/scripts/deployMaster.sh
chmod +x deployMaster.sh
wget https://raw.githubusercontent.com/skoulouzis/lobcder/dev/lobcder-master/scripts/storageFile
./deployMaster.sh -n lobcderDB -p pass -u root -f storageFile -l lobcderAdmin -s lobcderAdmin -c ./$catalina
