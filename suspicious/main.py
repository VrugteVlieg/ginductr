import numpy as np
from prepare import Preparation
from spectrum import Spectrum
import sys

def main(grammar, failed_tests, passed_tests) :
    """
    """
    prepare = Preparation(grammar,failed_tests, passed_tests)
    # get the represention of rules and tests
    rules = prepare.get_rules()
    failed = prepare.construct_matrix("failed")
    total_failed = failed.shape[1]
    passed = prepare.construct_matrix("passed")
    total_passed = passed.shape[1]
    # print(total_passed)
    failed_counts = prepare.basic_counts(failed)
    passed_counts = prepare.basic_counts(passed)
    # print(passed_counts)
    spectrum = Spectrum(failed_counts, passed_counts, total_failed, total_passed)
    scores = spectrum.compute_suspiciousness()
    spit_csv(rules, scores)

def spit_csv(rules, scores) :
    """
    :param rules  : contains rule names and their indices 
    :param scores : contains suspicious scores for each rule 
    """
    with open("scores.csv", "a") as file1:
        writeToStdOut = True
        if(writeToStdOut):
            # print("Rule,ef,ep,nf,np,Tarantula,Ochiai,Jaccard,DStar")
            for rule in scores :
                line = rules[str(rule)]
                for score in scores[rule] :
                    line = line + "," + str(score) 
                print(line)
        else:
            file1.write("Rule,ef,ep,nf,np,Tarantula,Ochiai,Jaccard,DStar\n")
            for rule in scores :
                line = rules[str(rule)]
                for score in scores[rule] :
                    line = line + "," + str(score) 
                file1.write(line+"\n")

if __name__ == "__main__" :
    grammar_data = sys.argv[1]
    failed_test_data = sys.argv[2]
    passed_test_data = sys.argv[3]
    main(grammar_data, failed_test_data, passed_test_data)
