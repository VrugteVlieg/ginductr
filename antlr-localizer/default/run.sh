#!/bin/bash

grammar=$1
tests=$2

mvn exec:java -Dexec.args="$grammar $tests" > execOut


touch scores.csv
rm scores.csv

# move generated to suspicious/ to calculate scores
mv *.json suspicious/
(
    cd suspicious/
    python3 main.py grammar.json fail.json pass.json
    mv scores.csv ../
)
