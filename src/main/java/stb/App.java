package stb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Stack;

public class App {
    /**
     * TODO add mutations
     * grouping rules on rhs term: expr factor factor; ->term: (expr factor) factor;
     * adding a new rule to a production    term: expr factor; -> term: expr Digit factor;
     * removing a rule from a production    term: expr Digit factor; -> term: expr factor;
     * adding a new rule to a grammar
     * when a new top scorer is found, make copies of it and add to list, apply grouping mutation to copies
     */
    static GrammarReader bestGrammar;
    static double bestScore = -1.0;
    static LinkedList<GrammarReader> myGrammars;
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            System.err.println(Constants.CURR_GRAMMAR_PATH);
            GrammarReader goldenGrammar = new GrammarReader(Constants.CURR_GRAMMAR_PATH);
            System.out.println(goldenGrammar);
            GrammarReader seededGrammar = new GrammarReader(Constants.SEEDED_GRAMMAR_PATH);
            seededGrammar.getParserRules().forEach(rule -> rule.nullable(seededGrammar.getParserRules()));
            // seededGrammar.removeLR();
            // seededGrammar.getParserRules().forEach(rule -> rule.nullable(seededGrammar.getParserRules()));
            // System.out.println(seededGrammar);
            
            // runTests(seededGrammar);
            // runTests(myReader);   
            myGrammars = GrammarGenerator.generatePopulation(Constants.POP_SIZE);
            // myGrammars.forEach(grammar -> System.out.println(grammar));
            for (int i = 0; i < Constants.NUM_ITERATIONS; i++) {
                Stack<GrammarReader> toCrossover = new Stack<GrammarReader>();
                System.out.println("\n\n\nGeneration " + i + "\n\n\n");
                myGrammars.forEach(grammar -> {
                    runTests(grammar);

                    if(grammar.toRemove()) return;

                    if(Constants.CROSSOVER && Math.random() < Constants.P_C) {
                        if(toCrossover.size() == 0) {
                            toCrossover.push(grammar);
                        } else {
                            toCrossover.pop().crossover(grammar);
                        }
                    }
                    
                    
                    
                    if(Constants.MUTATE) {
                        grammar.mutate(Constants.P_M, Constants.P_H);
                        
                    }
                    
                    
                    if(Constants.HEURISTIC) grammar.heuristic(Constants.P_H);
                    for(Rule rule  : grammar.getParserRules()) {
                        if(rule.getName().contains("* | ? | +")) {
                            System.out.println("Oh Shit " + rule);
                            Scanner in = new Scanner(System.in);
                            in.nextLine();
                            in.close();
                        }
                    }
                    grammar.fixUndefinedRules();
                    grammar.removeDuplicateProductions();
                    grammar.removeUnreachable();
                    grammar.removeLR();
                });
                // StringBuilder grammarList = new StringBuilder();
                // myGrammars.forEach(grammar -> grammarList.append(grammar.getName() + ","));
                // System.out.println("Pre removal grammars "  + grammarList);
                myGrammars.removeIf(grammar -> grammar.toRemove());
                if(myGrammars.size() != Constants.POP_SIZE) {
                    myGrammars.addAll(GrammarGenerator.generatePopulation(Constants.POP_SIZE - myGrammars.size()));
                }
                // grammarList.delete(0, grammarList.length());
                // myGrammars.forEach(grammar -> grammarList.append(grammar.getName() + ","));
                // System.out.println("Post removal " + grammarList);
                // Scanner in = new Scanner(System.in);
                // in.nextLine();

            }
        myGrammars.forEach(grammar -> {
            System.out.println(grammar.getName() + " score = " + grammar.getScore() + "\n" + grammar);
        });
        System.out.println("Best Grammar \n");
        System.out.println(bestGrammar.getName() + " score = " + bestGrammar.getScore() + "\n" + bestGrammar);
        } catch (Exception e) {
            // System.err.println("Exception in mainApp loop " + e.getCause());
            e.printStackTrace();
        } finally {
            // if(bestGrammar.getScore() == 0) {
            //     myGrammars.forEach(grammar -> {
            //         System.err.println(grammar.getName() + " score = " + grammar.getScore() + "\n" + grammar);
            //     });
            // } else {
                // System.err.println("Best Grammar \n");
                // System.err.println(bestGrammar.getName() + " score = " + bestGrammar.getScore() + "\n" + bestGrammar);
            // }
        }
    }
    
    public static void runTests(GrammarReader myReader) {
        myReader.injectEOF();
        lamdaArg removeCurr = () ->  myReader.flagForRemoval();
        Chelsea.generateSources(myReader, removeCurr);
        if(myReader.toRemove()) {
            System.out.println("Code gen failed for \n" + myReader);
            return;
        }
        // System.out.println("Code gen succesful for \n " + myReader);  
        double incScore = myReader.getScore();
        //TODO fix the scoring system
        try {
            
            HashMap<String,LinkedList<Stack<String>>> errors = Chelsea.runTestcases(removeCurr);
            myReader.stripEOF();
            if(myReader.toRemove()) {
                return;
            }
            int totalTests = errors.size();
            ArrayList<LinkedList<Stack<String>>> errorArr = new ArrayList<>();
            errors.forEach((key,value) -> {
                errorArr.add(value);
            });
            // System.out.println("Pre Filter errorCount for " + myReader.getName() + " " + errorArr);
            errorArr.removeIf(element -> element.size() == 0);
            // System.out.println("Post Filter errorCount for " + myReader.getName() + " " + errorArr);
            double numPass = totalTests - 1.0*errorArr.size();
            myReader.setScore(numPass/totalTests);
            if(incScore != myReader.getScore())
                System.out.println(myReader.getName() + " score " + incScore + " -> " + myReader.getScore());   

            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore() + " top score " + bestScore + '\n' + myReader);
                if(bestScore < myReader.getScore()) {
                    System.out.println("New top scorer " + myReader.getScore() + "\n" + myReader);
                    bestGrammar = new GrammarReader(myReader);
                    bestScore = myReader.getScore();
                }
            }
        } catch(Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        }
    }
}
