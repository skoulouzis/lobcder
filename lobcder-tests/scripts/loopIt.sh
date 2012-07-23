#!/bin/bash
for i in 1 2 
do
	#python2.6  /home/skoulouz/Documents/scripts/swift -A https://149.156.10.131:8443/auth/v1.0 -U username -K key delete LOBCDER-REPLICA-vTEST
	/opt/apache-maven-3.0.4/bin/mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test
	#mvn -Dtest=nl.uva.cs.lobcder.tests.PerformanceTest test
done
