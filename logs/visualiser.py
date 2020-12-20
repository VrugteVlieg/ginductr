import numpy as np
import math
import matplotlib.pyplot as plt
import os
import csv
import sys
import json
import glob
import pandas as pd
import re
import functools
from io import StringIO
#Default version uses _ delimited files cause java printed commas

def stdDevver(n):
    return math.sqrt(n)/30

# used to make the keys readable so they fit in the legend
def shortenKey(k):
    k.split(':')[1][2:4]

def transformKey(k):

    splitKey =  k.split(',')
    params = []
    for i in range(len(k)/2):
        params.append(join())
    





if __name__ == "__main__":
   
    

    #format of filenames
    #{grammar_name}_{metric}_{pop_size}
    # print(fileContent)
    logPath = sys.argv[1]
    with os.scandir(logPath) as configs:
        allResults = {}
        for config in configs:
            print(config.name)
            currPath = os.path.join(logPath, config.name, "**/*.json")
            fileNames  = glob.glob(currPath)
            jsonData = [json.load(open(file)) for file in fileNames]
            df = pd.DataFrame([pd.Series(jd) for jd in jsonData])

            
            numIter = len(df.AVG_SCORE[0])
            print([col for col in df])
            # avgLists = 
            avgDF = pd.DataFrame(
                        [
                            [pd.Series([run[index] for run in df[col]]).mean() for index in range(numIter)] for col in df
                        ], ["avgScore", "maxScore", "scoreDelta"])
            # print(df, "\n", avgDF, "\n\n\n")

            allResults[config.name] = avgDF.transpose()
            # print(pd.Series([1,2,3]).mean())
        
        # newKeys = [re.sub(",")]
        print([key for key in allResults.keys()])
        shortendKeys = [functools.reduce(lambda a,b: str(a) + "," + str(b)
                        ,list(map(lambda x: float(x.replace(',','.'))
                            ,re.split(",?[A-Z]+:", key)[1:])
                            )
                        ) 
                        for key in allResults.keys()]
        print(shortendKeys)
        avgScoreDF = pd.DataFrame([allResults[key].avgScore for key in allResults.keys()], shortendKeys).transpose()
        maxScoreDF = pd.DataFrame([allResults[key].maxScore for key in allResults.keys()], shortendKeys).transpose()
        scoreDeltaDF = pd.DataFrame([allResults[key].scoreDelta for key in allResults.keys()], shortendKeys).transpose()
        # print(avgScoreDF)
        avgScoreDF.plot()
        plt.title("average score over time")
        plt.show()
        maxScoreDF.plot()
        plt.title("Max score over time")
        plt.show()
        scoreDeltaDF.plot()
        plt.title("Score delta over time")
        plt.show()
        # for key in allResults.keys():
        #     res = allResults[key]
        #     print(key, ":\n", res)
        #     print(res.avgScore)
        #     res.plot()
        #     plt.show()
            # res.avgScore.plot(label=key)

        # plt.show()
        # print([allResults[frame] for frame in allResults.keys()])
            
            # print(f for f in os.scandir(entry.path))
    # fileData = json.load(open(sys.argv[1]))
    # print(fileData)
    # print(type(fileData["numGens"]))
    # plt.subplot(111)
    # plt.plot(range(0, fileData["numGens"]), fileData["dataPoints"])
    # plt.legend([fileData["legend"]],)
    # plt.title(fileData["title"])
    # plt.show()