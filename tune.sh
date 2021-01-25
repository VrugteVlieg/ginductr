#!/bin/bash
export outLbl=$(date)
testSuites=(0 1 2 3 4)
runCount=(0 1 2 3 4)
pMVals=(0.3 0.5)
pHVals=(0.3 0.5)
tsVals=(0.1 0.15)
trap "exit" INT
# for pCrc in "${pCrcVals[@]}"; do
#     export pCRC=$pCrc
#     for pCsc in "${pChangeSymbCountVals[@]}"; do
#         export pCSC=$pCsc
#         for pm in "${pMVals[@]}"; do
#             export pM=$pm
#             for ph in "${pHVals[@]}"; do
#                 export pH=$ph
#                 for ts in "${tsVals[@]}"; do
#                     export tS=$ts
for s in "${testSuites[@]}"; do
        export suite=$s
    for r in "${runCount[@]}"; do
        export currRun=$r
        ./buildGui.sh
    done
done
#                     ./buildGui.sh
#                 done
#             done
#         done
#     done
# done
# ./buildGui.sh