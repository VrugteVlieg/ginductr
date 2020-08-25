import numpy as np
import math
import matplotlib.pyplot as plt
import os
import csv
import sys
import json
from io import StringIO
#Default version uses _ delimited files cause java printed commas

def stdDevver(n):
    return math.sqrt(n)/30


if __name__ == "__main__":
   
    

    #format of filenames
    #{grammar_name}_{metric}_{pop_size}
    # print(fileContent)
    fileData = json.load(open(sys.argv[1]))
    print(fileData)
    print(type(fileData["numGens"]))
    plt.subplot(111)
    plt.plot(range(0, fileData["numGens"]), fileData["dataPoints"])
    plt.legend([fileData["legend"]],)
    plt.title(fileData["title"])
    plt.show()