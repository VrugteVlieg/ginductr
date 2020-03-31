package stb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class App {
    
    static HashMap<String,Double> scores = new HashMap<>();
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            // GrammarReader myReader = new GrammarReader("./grammars/arithmetic.g4");
            // runTests(myReader);   
            LinkedList<GrammarReader> myGrammars = GrammarGenerator.generatePopulation(1);
            for (int i = 0; i < 5; i++) {
                myGrammars.forEach(grammar -> {
                    // runTests(grammar);
                    // grammar.getAllRules().forEach(rule -> rule.getTotalProductions());
                    // if(!grammar.getPositiveAcceptance()) {
                        System.out.println("Pre  mutation " + grammar);
                        grammar.mutate(Constants.P_M, Constants.P_H);
                        System.out.println("Post mutation " +  grammar);
                    // }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //TODO implement scoring system followed by mutations

    public static void runTests(GrammarReader myReader) {
        Chelsea.generateSources(myReader);
        double incScore = myReader.getScore();
        try {
            HashMap<String,LinkedList<Stack<String>>> errors = Chelsea.runTestcases();
            if(errors.size() > 0) {
                errors.forEach((key,value) -> {
                    if(value.size() > 0) {
                        System.out.println("Errors for " + key + " = " + value.size());
                        for (int i = 0; i < value.size(); i++) {
                            System.out.println((i+1) + ": " + value.get(i));
                        }
                    } else {
                        System.out.println(key + "Actually passed for " + myReader.getName());
                        myReader.setScore(myReader.getScore() + 1/errors.size());
                    }
                });
            
            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore());
            }
            } else {
                myReader.setScore(1.0);
                System.out.println("All tests pass");
                myReader.setPositiveAcceptance(true);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
