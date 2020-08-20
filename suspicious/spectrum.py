import numpy as np
from my_dict import My_dictionary
from suspicious import Suspicious

class Spectrum :
    """
    """
    def __init__(self, failed, passed, tf, tp) :
        """
        :param failed : array containing ef count for each rule
        :param passed : array containing ep count for each rule
        :param tf : total tests failed
        :param tp : total  tests passed 
        """
        self.failed = failed 
        self.passed = passed 
        self.total_failed = tf
        self.total_passed = tp

    def compute_suspiciousness(self) :
        """
        """
        sus_scores = My_dictionary()
        non_of_rules = self.failed.shape[0]
        for i in range(non_of_rules) :
            scores = []
            ef = self.failed[i]
            ep = self.passed[i]
            scores.append(ef)
            scores.append(ep)
            nf = self.total_failed - ef
            np = self.total_passed - ep
            scores.append(nf)
            scores.append(np)
            suspicious = Suspicious(ef, self.total_failed, ep, self.total_passed)
            tara = suspicious.tarantula()
            scores.append(tara)
            ochi = suspicious.ochai()
            scores.append(ochi)
            jac = suspicious.jaccard()
            scores.append(jac)
            dstar = suspicious.dstar()
            scores.append(dstar)
            # add to dict 
            sus_scores.add(i, scores)
        return sus_scores
