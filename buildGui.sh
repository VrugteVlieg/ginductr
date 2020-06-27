#!/bin/bash
mvn clean
MAVEN_OPTS="-cp target/hons-1.0-SNAPSHOT.jar:./antlr-4.8-complete.jar"
mvn javafx:run