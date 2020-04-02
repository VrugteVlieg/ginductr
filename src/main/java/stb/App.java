package stb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import com.ibm.icu.util.CharsTrie.Iterator;

public class App {
    
    static LinkedList<GrammarReader> scores = new LinkedList<>();
    static LinkedList<GrammarReader> myGrammars;
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            // GrammarReader myReader = new GrammarReader("./grammars/arithmetic.g4");
            // runTests(myReader);   
            myGrammars = GrammarGenerator.generatePopulation(5);
            for (int i = 0; i < 50; i++) {
                Stack<GrammarReader> toCrossover = new Stack<GrammarReader>();
                System.out.println("\n\n\nGeneration " + i + "\n\n\n");
                myGrammars.forEach(grammar -> {
                    runTests(grammar);
                    if(grammar.toRemove()) return;
                    // grammar.getAllRules().forEach(rule -> rule.getTotalProductions());
                    // if(!grammar.getPositiveAcceptance()) {
                    if(Math.random() < Constants.P_C) {
                        if(toCrossover.size() == 0) {
                            toCrossover.push(grammar);
                        } else {
                            toCrossover.pop().crossover(grammar);
                        }
                    }
                    
                    // System.out.println("Pre  mutation " + grammar);
                    grammar.mutate(Constants.P_M, Constants.P_H);
                    // System.out.println("Post  mutation " + grammar);
                    // System.out.println("Pre  Filter " + grammar);
                    grammar.getParserRules().forEach(parserRule -> Chelsea.filterLinkedList(parserRule.getSubRules()));
                    grammar.removeUnreachable();
                    // System.out.println("Post Filter " +  grammar);
                    grammar.heuristic(Constants.P_H);
                    // System.out.println(grammar);

                    // }
                });
                myGrammars.removeIf(grammar -> grammar.toRemove());

            }
        myGrammars.forEach(grammar -> {
            System.out.println(grammar.getName() + " score = " + grammar.getScore() + "\n" + grammar);
        });
        System.out.println("Best Grammar");
        scores.forEach(grammar -> {
            System.out.println(grammar.getName() + " score = " + grammar.getScore() + "\n" + grammar);
        });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void runTests(GrammarReader myReader) {
        Chelsea.generateSources(myReader);
        double incScore = myReader.getScore();
        //TODO fix the scoring system
        try {
            lamdaArg removeCurr = () ->  myReader.flagForRemoval();;
            HashMap<String,LinkedList<Stack<String>>> errors = Chelsea.runTestcases(removeCurr);
            if(myReader.toRemove()) {
                return;
            }
            int totalTests = errors.size();
            ArrayList<LinkedList<Stack<String>>> errorArr = new ArrayList<>();
            errors.forEach((key,value) -> {
                errorArr.add(value);
            });
            double numPass = 0.0;
            if(totalTests > 0) {
                for(LinkedList<Stack<String>> currErr : errorArr) {
                    if(currErr.size() == 0) numPass++;
                }
            }
            myReader.setScore(numPass/totalTests);
            if(incScore != myReader.getScore())
                System.out.println(myReader.getName() + " score " + incScore + " -> " + myReader.getScore());   

            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore());
                if(scores.size() == 0 && myReader.getScore() > 0.0) scores.add(new GrammarReader(myReader));
                if(scores.size() == 1 && scores.get(0).getScore() < myReader.getScore()) {
                    System.out.println("New top scorer " + myReader.getScore() + "\n" + myReader);
                    scores.clear();
                    scores.add(new GrammarReader(myReader));
                }

            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
