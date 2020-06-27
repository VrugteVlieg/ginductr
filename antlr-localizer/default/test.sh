#!/bin/bash
cd antlr-localizer/default

./build.sh
result=$?
echo $result >> compOut
if [ $result -eq 0 ]; then
    echo  "codeGenSuccess"
    ./run.sh src/main/antlr4/za/ac/sun/cs/localizer/UUT.g4 tests/UUT
    rm target/generated-sources/antlr4/za/ac/sun/cs/localizer/*
else
    echo "codeGenFailed"    
fi

