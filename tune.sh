#!/bin/bash
export outLbl="$date"
pCrcVals=(0.2 0.4)
pChangeSymbCountVals=(0.3 0.4 0.5)
pMVals=(0.3 0.4 0.5)
pHVals=(0.3 0.4 0.5)
tsVals=(0.1 0.15)
for pCrc in "$pCrcVals"; do
    export pCRC=$pCrc
    for pCsc in "$pChangeSymbCountVals"; do
        export pCSC=$pCsc
        for pm in "$pMVals"; do
            export pM=$pm
            for ph in "$pHVals"; do
                export pH=$ph
                for ts in "$tsVals"; do
                    export tS=$ts
                    ./buildGui.sh
                done
            done
        done
    done
done