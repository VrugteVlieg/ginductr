package stb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class App {  
     
    static String bestGrammarString;
    static GrammarReader bestGrammar;
    static double bestScore = -1.0;
    static LinkedList<GrammarReader> myGrammars;
    static LinkedList<GrammarReader> positiveGrammar = new LinkedList<GrammarReader>();
    public static void main(String[] args) {
        try {
            GrammarReader goldenGrammar = new GrammarReader(Constants.CURR_GRAMMAR_PATH);
            
            myGrammars = GrammarGenerator.generatePopulation(Constants.POP_SIZE);
            goldenGrammar.computeMutants(Constants.MUTANTS_PER_BASE).forEach(grammar -> {
                System.out.println(grammar);
            });
            /*TODO
                Generate 1k candidates by randomly mutating each base grammar 1k/popSize times 
                store hashes of mutants in hashmap so they dont get regenerated
                Keep top popSize from each generation for next
                randomly restart some grammars each generation and add grammar + score to different hashmap
            */
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
                    
                    
                    
                    if(Constants.MUTATE && Math.random() < Constants.P_M) {
                        grammar.mutate(); 
                    }

                    if(Constants.GROUP && Math.random() < Constants.P_G) {
                        
                    }
                    grammar.fixUndefinedRules();
                    
                    
                    if(Constants.HEURISTIC) grammar.heuristic();

                    grammar.removeDuplicateProductions();
                    grammar.removeUnreachable();
                    grammar.removeLR();
                });
                // StringBuilder grammarList = new StringBuilder();
                // myGrammars.forEach(grammar -> grammarList.append(grammar.getName() + ","));
                // System.out.println("Pre removal grammars "  + grammarList);
                myGrammars.removeIf(grammar -> grammar.toRemove());
                if(myGrammars.size() != Constants.POP_SIZE) {
                    myGrammars.addAll(GrammarGenerator.generatePopulation(Constants.POP_SIZE - myGrammars.size(),bestGrammar));
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
        System.out.println("Best Grammar " + bestScore + "\n" + bestGrammar);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + bestScore + "\n" + bestGrammar);
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
            if(incScore >= myReader.getScore()) {
                myReader.incAge();
            } else {
                myReader.resetAge();
            }
            if(incScore != myReader.getScore())
                System.out.println(myReader.getName() + " score " + incScore + " -> " + myReader.getScore());   

            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore() + " top score " + bestScore + '\n' + myReader);
                if(myReader.getScore() == 1.0) positiveGrammar.add(new GrammarReader(myReader));
                if(bestScore < myReader.getScore()) {
                    System.out.println("New top scorer " + myReader.getScore() + "\n" + myReader);
                    bestGrammarString = myReader.toString();
                    bestGrammar = new GrammarReader(myReader);
                    System.out.println("Best grammar " + bestGrammar.getScore() + "\n" + bestGrammar);
                    bestScore = myReader.getScore();
                }
            }
        } catch(Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        }
    }
}
