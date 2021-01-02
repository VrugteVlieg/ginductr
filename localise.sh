#!/bin/bash

touch scores.csv
rm scores.csv

# move generated to suspicious/ to calculate scores
mv *.json suspicious/
(
    cd suspicious/
    python3 main.py grammar.json fail.json pass.json
    mv scores.csv ../
)
