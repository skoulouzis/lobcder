#!/bin/bash

# replace test.proprties
# mv etc/test.proprties etc/test.proprties.old


$HOME/servers/apache-tomcat-6.0.35-TEST/bin/shutdown.sh
$HOME/servers/apache-tomcat-6.0.35-TEST/bin/startup.sh
sleep 5
BASE_DIR=$HOME/workspace/lobcder-tests

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

echo $TEST_URL > $BASE_DIR/etc/test.proprties

echo $TEST_UNAME1 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS1 >> $BASE_DIR/etc/test.proprties

echo $TEST_UNAME2 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS2 >> $BASE_DIR/etc/test.proprties

echo $TEST_SCALE_NAMES >> $BASE_DIR/etc/test.proprties
echo $TEST_SCALE_PASS >> $BASE_DIR/etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> $BASE_DIR/etc/test.proprties
echo $TEST_BACKEND_UNAME >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties

echo $TEST_LARE_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_LARGE_DOWN >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties


bwm-ng -o csv -I lo -T rate >> $BASE_DIR/measures/localhost/lobcder-2.0-SNAPSHOT/bm_lobcder_mistos.csv &
BWM_PID=$!

JAVA_HOME=/opt/java/jdk $HOME/.netbeans/7.0/maven/bin/mvn -f $BASE_DIR/pom.xml -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test

sleep 5
kill $BWM_PID


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

echo $TEST_URL > $BASE_DIR/etc/test.proprties

echo $TEST_UNAME1 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS1 >> $BASE_DIR/etc/test.proprties

echo $TEST_UNAME2 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS2 >> $BASE_DIR/etc/test.proprties

echo $TEST_SCALE_NAMES >> $BASE_DIR/etc/test.proprties
echo $TEST_SCALE_PASS >> $BASE_DIR/etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> $BASE_DIR/etc/test.proprties
echo $TEST_BACKEND_UNAME >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties

echo $TEST_LARE_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_LARGE_DOWN >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties


for i in 1 2 3 4 5 
do
	JAVA_HOME=/opt/java/jdk $HOME/.netbeans/7.0/maven/bin/mvn -f $BASE_DIR//pom.xml -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test
done






# -----------------------------WEBDAV------------------
TEST_URL="webdav.test.url=http://localhost:8083/tomcatWebDAV"
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

echo $TEST_URL > $BASE_DIR/etc/test.proprties

echo $TEST_UNAME1 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS1 >> $BASE_DIR/etc/test.proprties

echo $TEST_UNAME2 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS2 >> $BASE_DIR/etc/test.proprties

echo $TEST_SCALE_NAMES >> $BASE_DIR/etc/test.proprties
echo $TEST_SCALE_PASS >> $BASE_DIR/etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> $BASE_DIR/etc/test.proprties
echo $TEST_BACKEND_UNAME >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties

echo $TEST_LARE_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_LARGE_DOWN >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties


bwm-ng -o csv -I lo -T rate >> $BASE_DIR/measures/localhost/tomcatWebDAV/bm_weddav_mistos.csv &
BWM_PID=$!

JAVA_HOME=/opt/java/jdk $HOME/.netbeans/7.0/maven/bin/mvn -f $BASE_DIR//pom.xml -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test

sleep 5
kill $BWM_PID


TEST_URL="webdav.test.url=http://localhost:8083//tomcatWebDAV"
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

echo $TEST_URL > $BASE_DIR/etc/test.proprties

echo $TEST_UNAME1 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS1 >> $BASE_DIR/etc/test.proprties

echo $TEST_UNAME2 >> $BASE_DIR/etc/test.proprties
echo $TEST_PASS2 >> $BASE_DIR/etc/test.proprties

echo $TEST_SCALE_NAMES >> $BASE_DIR/etc/test.proprties
echo $TEST_SCALE_PASS >> $BASE_DIR/etc/test.proprties

echo $TEST_BACKEND_ENDPOINT >> $BASE_DIR/etc/test.proprties
echo $TEST_BACKEND_UNAME >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties

echo $TEST_LARE_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_LARGE_DOWN >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_UP >> $BASE_DIR/etc/test.proprties
echo $TEST_SET_DOWN >> $BASE_DIR/etc/test.proprties


for i in 1 2 3 4 5 
do
    JAVA_HOME=/opt/java/jdk $HOME/.netbeans/7.0/maven/bin/mvn -f $BASE_DIR//pom.xml -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test-compile surefire:test
done




#---------------------THE END---------------------------
scp -r $HOME/measures/localhost elab:$BASE_DIR//measures
$HOME/servers/apache-tomcat-6.0.35-TEST/bin/shutdown.sh

