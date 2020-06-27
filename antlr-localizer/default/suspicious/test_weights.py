import numpy as np

class WeightedSpectrum :
    """
    This an implementation of simple weighted spectrum.
    
    The idea is straight forward,for each entity e replace the 
    counts in the formula, by the total weight of tests 
    that execute an  entity e.
    """
    def __init__(self, rules, failed, passed) :
        """
        """
        self.rules_graph = rules
        self.failed_graph = failed
        self.passed_graph = passed 

    def out_degree(self, node, graph) :
        """
        """
        return len(graph[node])

    def failed_tests_weights(self) :
        """
        """
        no_of_rules = len(self.rules_graph.items())
        no_of_tests = len(self.failed_graph.items())
        matrix = [[0 for k in range(no_of_tests)] for i in range(no_of_rules)]
        total_weight = 0
        for node in self.failed_graph :
            v1 = int(node)
            for neighbour in self.failed_graph[node] :
                v2 = int(neighbour)
                weight = 1/self.out_degree(node, self.failed_graph)
                matrix[v2][v1] = weight
                total_weight = total_weight + weight
        return total_weight, np.array(matrix)

    def passed_tests_weights(self) :
        """
        All passed test assigned the same weight.
        Fixed for each test is 1/no_of_tests
        """
        no_of_rules = len(self.rules_graph.items())
        no_of_tests = len(self.passed_graph.items())
        matrix = [[0 for k in range(no_of_tests)] for i in range(no_of_rules)]
        total_weight = 0
        for node in self.passed_graph:
            v1 = int(node)
            for neighbour in self.passed_graph[node]:
                v2 = int(neighbour)
                weight = 1 / no_of_tests
                total_weight = 1/self.out_degree(node, self.passed_graph)
                matrix[v2][v1] = weight
        return total_weight, np.array(matrix)
