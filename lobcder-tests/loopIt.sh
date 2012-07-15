#!/bin/bash
for i in 1 2 3
do
#   python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U vphdemo:vphdemo -K LibiDibi7 delete LOBCDER-REPLICA-vTEST
#   /opt/apache-maven-3.0.4/bin/mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test
  mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test
#   mv  /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/download/scaleUser1.csv /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/download/scaleUser1$time$i.csv
#   mv  /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/upload/scaleUser1.csv /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/upload/scaleUser1$time$i.csv
done

python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U vphdemo:vphdemo -K LibiDibi7 delete LOBCDER-REPLICA-vTEST
