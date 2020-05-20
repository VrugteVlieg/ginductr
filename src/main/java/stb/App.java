package stb;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javafx.application.Application;

public class App {  
    static GrammarReader demoGrammar;
    static GrammarReader demoGrammar2;
    static int numGrammarsChecked = 0;
    static int hashtableHits = 0;
    static int floatingEOF = 0;

    static outputLambda logOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda grammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runGrammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runLogOutput = (String toPrint) -> System.out.println(toPrint);

    static GrammarReader bestGrammar;

    static LinkedList<GrammarReader> myGrammars;
    //Can reduce memory footprint my storing grammarStrings in positiveGrammars and reconstructing when needed
    static LinkedList<GrammarReader> positiveGrammars = new LinkedList<GrammarReader>();
    static LinkedList<GrammarReader> perfectGrammars = new LinkedList<GrammarReader>();
    static HashMap<Double, LinkedList<GrammarReader>> evaluatedGrammars = new HashMap<Double, LinkedList<GrammarReader>>();
    static HashMap<Integer, Boolean> generatedGrammars = new HashMap<Integer, Boolean>();
    public static void main(String[] args) {
        if(Constants.USE_GUI) {
            Application.launch(Gui.class, new String[]{});
            
            System.exit(0);
        }

        try {
            // GrammarReader goldenGrammar = new GrammarReader(Constants.CURR_GRAMMAR_PATH);
            // GrammarReader seededGrammar = new GrammarReader(new File(Constants.SEEDED_GRAMMAR_PATH));
            

            myGrammars = GrammarGenerator.generatePopulation(Constants.POP_SIZE);
            bestGrammar  = myGrammars.getFirst();
            for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                System.out.println(genNum);
                LinkedList<GrammarReader> totalPop  = new LinkedList<GrammarReader>();
                totalPop.addAll(myGrammars);
                myGrammars.forEach(grammar -> totalPop.addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE,generatedGrammars)));
                totalPop.forEach(grammar -> runTests(grammar, Constants.POS_TEST_DIR, Constants.positiveScoring));
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
            
        System.err.println("Best grammar " + getBestGrammarString());
        System.out.println("Positive grammars " + positiveGrammars.size());
        positiveGrammars.forEach(grammar -> System.out.println(grammar));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
        }
    }
    
    public static void runTests(GrammarReader myReader, String testDir, scoringLambda scoreCalc) {
        if(myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            System.out.println("Flagging " + myReader.getName() + " for removal");
            myReader.flagForRemoval();
            return;
        }
        myReader.injectEOF();

        lamdaArg removeCurr = () ->  myReader.flagForRemoval();
        Chelsea.generateSources(myReader, removeCurr);
        if(myReader.toRemove()) {
            System.out.println("Code gen failed for \n" + myReader);
            myReader.stripEOF();
            return;
        }
        numGrammarsChecked++;
        try {
            
            int[] testResult = Chelsea.runTestcases(removeCurr, testDir);
            myReader.stripEOF();
            if(myReader.toRemove()) {
                return;
            }

            scoreCalc.eval(testResult, myReader);

        } catch(Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        }
    }

    

    public static void demoMainProgram() {
        try {
            myGrammars = GrammarGenerator.generatePopulation(Constants.POP_SIZE);
            runLogOutput.output("Generated " + myGrammars.size() + " base grammars\n");
            
            //Postive testing 
            for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                runLogOutput.output("Starting generation " + genNum);
                runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");
                
                LinkedList<GrammarReader> totalPop  = new LinkedList<GrammarReader>();
                totalPop.addAll(myGrammars);
                myGrammars.forEach(grammar -> totalPop.addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE,generatedGrammars)));
                totalPop.forEach(grammar -> {
                    runTests(grammar, Constants.POS_TEST_DIR, Constants.positiveScoring);
                    double outScore = grammar.getScore();
                    if(grammar.getPosScore() == 1.0) positiveGrammars.add(grammar);
                    if(grammar.getPosScore() == 1.0) grammarOutput.output("New positive grammar\n"  +  grammar);
                    if(outScore > getBestScore()) {
                        runGrammarOutput.output("\nNew best grammar\nBest score " + getBestScore() + " -> " +  outScore);
                        runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
                        setBestGrammar(grammar);
                    }
                });
                totalPop.removeIf(GrammarReader::toRemove);

                
                totalPop.forEach(grammar -> {
                    double currScore = grammar.getScore();
                    if(evaluatedGrammars.containsKey(currScore)){
                        evaluatedGrammars.get(currScore).add(grammar);
                    } else {
                        LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                        thisScoreList.add(grammar);
                        evaluatedGrammars.put(currScore,thisScoreList);
                    }
                });
                
    
                Double[] scoreArr = evaluatedGrammars.keySet().stream()
                                    .sorted(Comparator.reverseOrder())
                                    .toArray(Double[]::new);

                System.out.println("scoreArr " + Arrays.toString(scoreArr));
                
                //Add crossover grammars
                double bestScore = scoreArr[0];
                //Only start performing crossover if some decent grammar already exist
                if(bestScore > 0.2) {

                    //Calculate crossoverPop
                    ArrayList<GrammarReader> crossoverPop = new ArrayList<GrammarReader>();
                    double numScores = scoreArr.length;
                    double maxIndex = numScores-1;
                    ArrayList<GrammarReader> toConsider = new ArrayList<GrammarReader>();
                    if(maxIndex % 2 == 0) {
                        toConsider.addAll(evaluatedGrammars.get(scoreArr[(int)(maxIndex/2)]));
                    } else {
                        toConsider.addAll(evaluatedGrammars.get(scoreArr[(int)Math.floor(maxIndex/2)]));
                        toConsider.addAll(evaluatedGrammars.get(scoreArr[(int)Math.ceil(maxIndex/2)]));
                    }
                    for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
                        GrammarReader g1 = new GrammarReader(toConsider.remove(randInt(toConsider.size())));
                        GrammarReader g2 = new GrammarReader(toConsider.remove(randInt(toConsider.size())));
                        System.out.println("Performing crossover on " + g1 + " \nand\n" + g2);
                        g1.applyCrossover(g2, runGrammarOutput);
                        crossoverPop.add(g1);
                        crossoverPop.add(g2);

                    }

                    //Run tests on crossoverPop
                    crossoverPop.forEach(grammar -> {
                        runTests(grammar, Constants.POS_TEST_DIR, Constants.positiveScoring);
                        double outScore = grammar.getScore();
                        if(grammar.getPosScore() == 1.0) positiveGrammars.add(grammar);
                        if(grammar.getPosScore() == 1.0) grammarOutput.output("New positive grammar\n"  +  grammar);
                        if(outScore > getBestScore()) {
                            runGrammarOutput.output("\nNew best grammar\nBest score " + getBestScore() + " -> " +  outScore);
                            runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
                            setBestGrammar(grammar);
                        }
                    });
                    crossoverPop.removeIf(GrammarReader::toRemove);
    
                    
                    crossoverPop.forEach(grammar -> {
                        double currScore = grammar.getScore();
                        if(evaluatedGrammars.containsKey(currScore)){
                            evaluatedGrammars.get(currScore).add(grammar);
                        } else {
                            LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                            thisScoreList.add(grammar);
                            evaluatedGrammars.put(currScore,thisScoreList);
                        }
                    });

                    scoreArr = evaluatedGrammars.keySet().stream()
                                    .sorted(Comparator.reverseOrder())
                                    .toArray(Double[]::new);

                }




                
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
    
        runGrammarOutput.output("Finished Pos best grammar\n" + getBestGrammarString());
        positiveGrammars.forEach(grammar -> System.out.println(grammar));
        evaluatedGrammars.clear();
        myGrammars.clear();
        myGrammars.addAll(positiveGrammars.stream().map(GrammarReader::new).collect(Collectors.toList()));
        
        runGrammarOutput.output("Starting negative testing, num positive grammars : " + myGrammars.size()); 
        System.out.println("Positive grammars \n" + myGrammars.stream().map(GrammarReader::toString).collect(Collectors.joining("\n")));
        runLogOutput.output("Starting negative testing");
        //Negative testing 
        for (int genNum = 0; genNum < Constants.NUM_NEGATIVE_ITERATIONS; genNum++) {
            runLogOutput.output("Starting generation " + genNum);
            // runLogOutput.output("GrammarCount base size " + myGrammars.size());
            runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");
            
            LinkedList<GrammarReader> totalPop  = new LinkedList<GrammarReader>();
            totalPop.addAll(myGrammars);

            myGrammars.forEach(grammar -> totalPop.addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE,generatedGrammars)));

            totalPop.forEach(grammar -> runTests(grammar, Constants.POS_TEST_DIR, Constants.positiveScoring));
            totalPop.removeIf(grammar -> grammar.getPosScore() != 1.0 || grammar.toRemove());
            totalPop.forEach(grammar -> {
                runTests(grammar, Constants.NEG_TEST_DIR, Constants.negativeScoring);
                double newScore = grammar.getScore();
                System.out.println("Negative score " + newScore + " for \n" + grammar);
                if(grammar.getNegScore() == 1.0) perfectGrammars.add(grammar);
                if(grammar.getScore() > getBestScore()) {
                    runGrammarOutput.output((grammar.getNegScore() == 1.0 ? "\nFully matched grammar\n" : "\nNew best grammar\n")+ "Best score " + getBestScore() + " -> " +  newScore);
                    runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
                    setBestGrammar(grammar);
                }


            });

            totalPop.removeIf(GrammarReader::toRemove);

            totalPop.forEach(grammar -> {
                double currScore = grammar.getScore();
                if(evaluatedGrammars.containsKey(currScore)){
                    evaluatedGrammars.get(currScore).add(grammar);
                } else {
                    LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                    thisScoreList.add(grammar);
                    evaluatedGrammars.put(currScore,thisScoreList);
                }
            });
            
            List<Double> scoreList = evaluatedGrammars.keySet().stream()
                                .sorted(Comparator.reverseOrder())
                                .collect(Collectors.toList());
            
            
            myGrammars.clear();
            int grammarsToAdd = Math.min(Constants.POP_SIZE, totalPop.size());
            
            while(myGrammars.size() < grammarsToAdd) {
                int grammarsLeft = grammarsToAdd-myGrammars.size();
                System.err.println(myGrammars.size() + " " + grammarsToAdd + " " + totalPop.size() + " " + positiveGrammars.size());
                if(!scoreList.isEmpty()) {
                    LinkedList<GrammarReader> currList = evaluatedGrammars.get(scoreList.remove(0));
                    
                    if(currList.size() <= grammarsLeft) {
                        myGrammars.addAll(currList);
                    } else {
                        for (int j = 0; j < grammarsLeft; j++) {
                            myGrammars.add(currList.remove(randInt(currList.size())));
                        }
                    }
                } else {
                    if(positiveGrammars.size() <= grammarsLeft) {
                        myGrammars.addAll(positiveGrammars);
                    } else {
                        for (int j = 0; j < grammarsLeft; j++) {
                            myGrammars.add(positiveGrammars.get(randInt(positiveGrammars.size())));
                        }
                    }
                }
            }
        }
        runGrammarOutput.output("Finished all number of perfect match grammars: " + perfectGrammars.size());


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            System.err.println("Floating EOF " + floatingEOF);
        }

    }

    public static void positiveTesting() {
       
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

    public static void loadStartGrammar() {
        GrammarReader out = new GrammarReader(new File(Constants.SEEDED_GRAMMAR_PATH));
        GrammarGenerator.fillBlanks(out);
        GrammarReader hashedOut = new GrammarReader(out.hashString() + '\n' + out.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        demoGrammar = hashedOut;
        hashedOut.setName("demoGrammar");
        ArrayList<String> reachables = new ArrayList<String>();
        demoGrammar.getParserRules().get(0).getReachables(demoGrammar.getParserRules(),reachables);
        grammarOutput.output("Reachables\n" + reachables);
        // out.getParserRules().forEach(rule -> System.out.println(rule.getName() + rule.isSingular()));
        grammarOutput.output(hashedOut.toString());
    }



    public static void ruleCountDemo() {
        demoGrammar.demoChangeRuleCount(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void symbolCountDemo() {
        demoGrammar.demoChangeSymbolCount(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void groupDemo() {
        demoGrammar.demoGroupMutate(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void symbMutateDemo() {
        demoGrammar.demoMutate(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void demoHeuristic() {
        demoGrammar.demoHeuristic(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }



    static void setRunGrammarOutput(outputLambda newRun) {
        runGrammarOutput = newRun;
    }

    static void setRunLogOutput(outputLambda newRun) {
        runLogOutput = newRun;
    }

    static void setBestGrammar(GrammarReader newBest) {
        bestGrammar = new GrammarReader(newBest.hashString() + '\n' + newBest.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        bestGrammar.setName("bestCandidate");
        bestGrammar.setNegScore(newBest.getNegScore());
        bestGrammar.setPosScore(newBest.getPosScore());
        System.out.println("New Best grammar with score  " +  getBestScore());
    }

    static double getBestScore() {
        if(bestGrammar == null) return -1;
        return bestGrammar.getScore();
    }
    
    static String getBestGrammarString() {
        if(bestGrammar == null) return "";
        return bestGrammar.toString();
    }

    public static int numGrammarsEvalled() {
        return numGrammarsChecked;
    }

	public static void demoCrossover() {
		GrammarReader g1 = GrammarGenerator.generatePopulation(1).getFirst();
        GrammarReader g2 = GrammarGenerator.generatePopulation(1).getFirst();
        grammarOutput.output("clear");
        grammarOutput.output(g1.toString());
        grammarOutput.output(g2.toString());
        g1.applyCrossover(g2, grammarOutput);
        grammarOutput.output(g1.toString());
        grammarOutput.output(g2.toString());
	}

   
}
