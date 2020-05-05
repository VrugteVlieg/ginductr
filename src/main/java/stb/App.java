package stb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.ThreadLocalRandom;
import javafx.application.Application;

public class App {  

    static GrammarReader demoGrammar;


    static outputLambda logOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda grammarOutput = (String toPrint) -> System.out.println(toPrint);

    static String bestGrammarString;
    static GrammarReader bestGrammar;
    static double bestScore = -1.0;
    static LinkedList<GrammarReader> myGrammars;
    //Can reduce memory footprint my storing grammarStrings in positiveGrammars and reconstructing when needed
    static LinkedList<GrammarReader> positiveGrammar = new LinkedList<GrammarReader>();
    static HashMap<Double, LinkedList<GrammarReader>> evaluatedGrammars = new HashMap<Double, LinkedList<GrammarReader>>();
    static HashMap<Integer, Boolean> generatedGrammars = new HashMap<Integer, Boolean>();
    public static void main(String[] args) {
        if(Constants.USE_GUI) {
            Application.launch(Gui.class, new String[]{});
            System.err.println("Test");
            System.exit(0);
        }

        try {
            // GrammarReader goldenGrammar = new GrammarReader(Constants.CURR_GRAMMAR_PATH);
            GrammarReader seededGrammar = new GrammarReader(Constants.SEEDED_GRAMMAR_PATH);
            

            myGrammars = GrammarGenerator.generatePopulation(Constants.POP_SIZE);
            
            for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                System.out.println(genNum);
                LinkedList<GrammarReader> totalPop  = new LinkedList<GrammarReader>();
                totalPop.addAll(myGrammars);
                myGrammars.forEach(grammar -> totalPop.addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE,generatedGrammars)));
                totalPop.forEach(App::runTests);
                totalPop.removeIf(GrammarReader::toRemove);

                Double[] scoreArr = evaluatedGrammars.keySet().toArray(new Double[0]);
                Arrays.sort(scoreArr);
                myGrammars.clear();
                int grammarsToAdd = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;

                while(true) {
                    if(grammarsToAdd == 0) break;
                    for (int i = 0; i < scoreArr.length; i++) {
                        LinkedList<GrammarReader> currList = evaluatedGrammars.get(scoreArr[i]);
                        grammarsToAdd = Constants.POP_SIZE-myGrammars.size();
                        if(currList.size() <= grammarsToAdd) {
                            myGrammars.addAll(currList);
                        } else {
                            for (int j = 0; j < grammarsToAdd; j++) {
                                myGrammars.add(currList.remove(randInt(currList.size())));
                            }
                        }
                    }
                }
                myGrammars.addAll(GrammarGenerator.generatePopulation(Constants.FRESH_POP_PER_GEN));
            }
            
        System.err.println("Best grammar " + bestGrammarString);
        System.out.println("Positive grammars " + positiveGrammar.size());
        positiveGrammar.forEach(grammar -> System.out.println(grammar));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + bestScore + "\n" + bestGrammar);
        }
    }
    
    public static void runTests(GrammarReader myReader) {
        if(myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            myReader.flagForRemoval();
            return;
        }
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

            //Add the grammar to scored hashMap
            if(evaluatedGrammars.containsKey(myReader.getScore())){
                evaluatedGrammars.get(myReader.getScore()).add(myReader);
            } else {
                LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                thisScoreList.add(myReader);
                evaluatedGrammars.put(myReader.getScore(),thisScoreList);
            }
            
            if(incScore != myReader.getScore())
                System.out.println(myReader.getName() + " score " + incScore + " -> " + myReader.getScore());   

            if(myReader.getScore() > incScore) {
                System.out.println(myReader.getName() + " has improved its score from " + incScore + " to " + myReader.getScore() + " top score " + bestScore + '\n' + myReader);
                if(myReader.getScore() == 1.0) positiveGrammar.add(new GrammarReader(myReader));
                if(myReader.getScore() == 1.0) System.out.println("positive grammar \n" + myReader);
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

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static outputLambda getGrammarOut() {
        return grammarOutput;
    }
    
    public static void setGrammarOut(outputLambda toSet) {
        grammarOutput = toSet;
    }

    public static outputLambda getLogOut() {
        return logOutput;
    }
    
    public static void setLogOut(outputLambda toSet) {
        logOutput = toSet;
    }

    public static void loadStartGrammar(String pathTo) {
        GrammarReader out = new GrammarReader(pathTo);
        demoGrammar = out;
        grammarOutput.output(out.toString());
    }

    public static void ruleCountDemo() {
        demoGrammar.demoChangeRuleCount(logOutput, grammarOutput);
        grammarOutput.output(demoGrammar.toString());
    }

    public static void symbolCountDemo() {
        demoGrammar.demoChangeSymbolCount(logOutput, grammarOutput);
        grammarOutput.output(demoGrammar.toString());
    }

    public static void groupDemo() {
        demoGrammar.demoGroupMutate(logOutput, grammarOutput);
        grammarOutput.output(demoGrammar.toString());
    }

    public static void symbMutateDemo() {
        demoGrammar.demoMutate(logOutput, grammarOutput);
        grammarOutput.output(demoGrammar.toString());
    }
}
