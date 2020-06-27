#!/bin/bash
out=0

mvn clean > /dev/null

if [ $? -eq 0 ]; then
    # mvn za.ac.sun.cs:ST4-maven-plugin:1.0-SNAPSHOT:render > /dev/null
    mvn compile > compOut
else
    exit 1
fi


