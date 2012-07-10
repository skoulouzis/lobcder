#!/bin/bash
for i in 1 2 3 4 5 6 7 8 9 10 
do
	echo "Welcome $i times"
	/opt/apache-maven-3.0.4/bin/mvn -Dtest=nl.uva.cs.lobcder.tests.LobcderScalabilityTest test
	 mv  /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/download/scaleUser1.csv /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/download/scaleUser1$i.csv
	 mv  /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/upload/scaleUser1.csv /home/skoulouz/workspace/lobcder-tests/measures/elab.lab.uvalight.net/lobcder-1.0-SNAPSHOT/upload/scaleUser1$i.csv
done
