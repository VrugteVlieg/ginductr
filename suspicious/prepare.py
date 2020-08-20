import json
import numpy as np

class Preparation :
    """
    This class handles preprocessing before suspiciousnes 
    scores computation. Reads in files and contructs 
    matrices depending out test outcome. columns 
    are tests and we have rules on the rows
    """
    def __init__(self, rules, failed, passed) :
        """
        :param rules  : json file containing rules of grammar under test
        :param failed : json file with data from failed tests
        :param passed : json file with data from passed tests
        """
        self.rule_data = self.read_json(rules)
        self.failed_data = self.read_json(failed)
        self.passed_data = self.read_json(passed)

    def read_json(self, jfile) :
        """
        :param jfile : a json file with graph data
        :return data
        """
        temp = open(jfile)
        data = json.load(temp)
        temp.close()
        return data

    def get_rules(self) :
        """
        """
        return self.rule_data

    def construct_matrix(self, outcome) :
        """
        columns are tests
        rows represent rules 
        """
        no_of_rules = len(self.rule_data.items())
        # get no of tests
        if outcome == "passed" :
            graph = self.passed_data
        else :
            graph = self.failed_data
        no_of_tests = len(graph.items())

        matrix = [[0 for k in range(no_of_tests)] for i in range(no_of_rules)]
        for node in graph :
            v1 = int(node)
            for neighbour in graph[node] :
                v2 = int(neighbour)
                matrix[v2][v1] = 1
        return np.array(matrix)

    def basic_counts(self, matrix) :
        """
        """
        counts = np.sum(matrix, axis=1)
        return counts
