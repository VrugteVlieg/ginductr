#!/bin/bash
mvn package -f "/home/jaco/Code/honsProj/pom.xml" 

$JAVA_HOME/bin/java -cp target/hons-1.0-SNAPSHOT.jar:./antlr-4.8-complete.jar stb.App grcd 4