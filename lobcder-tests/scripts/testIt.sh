#!/bin/bash

# replace test.proprties
# mv etc/test.proprties etc/test.proprties.old

TEST_URL="webdav.test.url=http://localhost:8083/lobcder-2.0-SNAPSHOT/dav"
TEST_UNAME1="webdav.test.username1=demo1"
TEST_PASS1="webdav.test.password1=d3moPasswd"

TEST_UNAME2="webdav.test.username2=p-med"
TEST_PASS2="webdav.test.password2=hev9ngs43w"

TEST_SCALE_NAMES="lobcder.scale.usernames=scaleUser1,scaleUser2,scaleUser3,scaleUser4,scaleUser5,scaleUser6,scaleUser7,scaleUser8,scaleUser9,scaleUser10,scaleUser11,scaleUser12,scaleUser13,scaleUser14,scaleUser15,scaleUser16,scaleUser17,scaleUser18,scaleUser19,scaleUser20,scaleUser21,scaleUser22,scaleUser23,scaleUser24"
TEST_SCALE_PASS="lobcder.scale.password=passwd"

TEST_BACKEND_ENDPOINT="backend.endpoint=swift://149.156.10.131:8443/auth/v1.0/"
TEST_BACKEND_UNAME="backend.username=user"
TEST_BACKEND_PASS="backend.password=key"

TEST_LARE_UP="test.large.upload=true"
TEST_LARGE_DOWN="test.large.download=true"
TEST_SET_UP="test.dataset.upload=false"
TEST_SET_DOWN="test.dataset.download=false"

echo $TEST_URL > etc/test.proprties

echo $TEST_UNAME1 >> etc/test.proprties
echo $TEST_PASS1 >> etc/test.proprties

echo $TEST_UNAME2 >> etc/test.proprties
echo $TEST_PASS2 >> etc/test.proprties

echo $TEST_SCALE_NAMES >> etc/test.proprties
echo $TEST_SCALE_PASS >> etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> etc/test.proprties
echo $TEST_BACKEND_UNAME >> etc/test.proprties
echo $TEST_SET_DOWN >> etc/test.proprties

echo $TEST_LARE_UP >> etc/test.proprties
echo $TEST_LARGE_DOWN >> etc/test.proprties
echo $TEST_SET_UP >> etc/test.proprties
echo $TEST_SET_DOWN >> etc/test.proprties


JAVA_HOME=/opt/java/jdk /home/skoulouz/.netbeans/7.0/maven/bin/mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test


TEST_URL="webdav.test.url=http://localhost:8083/lobcder-2.0-SNAPSHOT/dav"
TEST_UNAME1="webdav.test.username1=demo1"
TEST_PASS1="webdav.test.password1=d3moPasswd"

TEST_UNAME2="webdav.test.username2=p-med"
TEST_PASS2="webdav.test.password2=hev9ngs43w"

TEST_SCALE_NAMES="lobcder.scale.usernames=scaleUser1,scaleUser2,scaleUser3,scaleUser4,scaleUser5,scaleUser6,scaleUser7,scaleUser8,scaleUser9,scaleUser10,scaleUser11,scaleUser12,scaleUser13,scaleUser14,scaleUser15,scaleUser16,scaleUser17,scaleUser18,scaleUser19,scaleUser20,scaleUser21,scaleUser22,scaleUser23,scaleUser24"
TEST_SCALE_PASS="lobcder.scale.password=passwd"

TEST_BACKEND_ENDPOINT="backend.endpoint=swift://149.156.10.131:8443/auth/v1.0/"
TEST_BACKEND_UNAME="backend.username=user"
TEST_BACKEND_PASS="backend.password=key"

TEST_LARE_UP="test.large.upload=false"
TEST_LARGE_DOWN="test.large.download=false"
TEST_SET_UP="test.dataset.upload=true"
TEST_SET_DOWN="test.dataset.download=true"

echo $TEST_URL > etc/test.proprties

echo $TEST_UNAME1 >> etc/test.proprties
echo $TEST_PASS1 >> etc/test.proprties

echo $TEST_UNAME2 >> etc/test.proprties
echo $TEST_PASS2 >> etc/test.proprties

echo $TEST_SCALE_NAMES >> etc/test.proprties
echo $TEST_SCALE_PASS >> etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> etc/test.proprties
echo $TEST_BACKEND_UNAME >> etc/test.proprties
echo $TEST_SET_DOWN >> etc/test.proprties

echo $TEST_LARE_UP >> etc/test.proprties
echo $TEST_LARGE_DOWN >> etc/test.proprties
echo $TEST_SET_UP >> etc/test.proprties
echo $TEST_SET_DOWN >> etc/test.proprties


for i in 1 2 3 4 5 
do
	#python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U username -K key delete LOBCDER-REPLICA-vTEST
	JAVA_HOME=/opt/java/jdk /home/skoulouz/.netbeans/7.0/maven/bin/mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test
	#mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test
done



scp -r /home/skoulouz/measures/localhost elab:/home/skoulouz/workspace/lobcder-tests/measures


