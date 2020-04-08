package stb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class App {
    
    static GrammarReader bestGrammar;
    static double bestScore = -1.0;
    static LinkedList<GrammarReader> myGrammars;
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            // GrammarReader goldenGrammar = new GrammarReader("./grammars/arithmetic.g4");
            // System.out.println(goldenGrammar);
            GrammarReader seededGrammar = new GrammarReader("./grammars/seededArithmetic.g4");
            seededGrammar.removeDirectLeftRecursion();
            // runTests(seededGrammar);
            // runTests(myReader);   
            myGrammars = GrammarGenerator.generatePopulation(0);
            // myGrammars.add(seededGrammar);
            for (int i = 0; i < 50; i++) {
                Stack<GrammarReader> toCrossover = new Stack<GrammarReader>();
                System.out.println("\n\n\nGeneration " + i + "\n\n\n");
                myGrammars.forEach(grammar -> {
                    grammar.ensureValidity();
                    runTests(grammar);
                    if(grammar.toRemove()) return;
                    // if(Math.random() < Constants.P_C) {
                    //     if(toCrossover.size() == 0) {
                    //         toCrossover.push(grammar);
                    //     } else {
                    //         toCrossover.pop().crossover(grammar);
                    //     }
                    // }
                    
                    // System.out.println("Pre  mutation " + grammar);
                    grammar.mutate(Constants.P_M, Constants.P_H);
                    // System.out.println("Post  mutation " + grammar);
                    // System.out.println("Pre  Filter " + grammar);
                    grammar.getParserRules().forEach(parserRule -> Chelsea.removeDuplicates(parserRule.getSubRules()));
                    grammar.removeUnreachable();
                    // System.out.println("Post Filter " +  grammar);
                    grammar.heuristic(Constants.P_H);
                    // System.out.println(grammar);

                    // }
                });
                // StringBuilder grammarList = new StringBuilder();
                // myGrammars.forEach(grammar -> grammarList.append(grammar.getName() + ","));
                // System.out.println("Pre removal grammars "  + grammarList);
                myGrammars.removeIf(grammar -> grammar.toRemove());
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
        }
    }
    
    public static void runTests(GrammarReader myReader) {
        myReader.injectEOF();
        Chelsea.generateSources(myReader);
        System.out.println(myReader);
        double incScore = myReader.getScore();
        //TODO fix the scoring system
        try {
            lamdaArg removeCurr = () ->  myReader.flagForRemoval();;
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
            System.out.println("Pre Filter errorCount for " + myReader.getName() + " " + errorArr);
            errorArr.removeIf(element -> element.size() == 0);
            System.out.println("Post Filter errorCount for " + myReader.getName() + " " + errorArr);
            double numPass = totalTests - 1.0*errorArr.size();
            myReader.setScore(numPass/totalTests);
            if(incScore != myReader.getScore())
                System.out.println(myReader.getName() + " score " + incScore + " -> " + myReader.getScore());   

            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore() + " top score " + bestScore);
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
