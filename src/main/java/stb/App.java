package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import javafx.application.Application;

public class App {
    static GrammarReader demoGrammar;
    static GrammarReader demoGrammar2;
    static int numGrammarsChecked = 0;
    static int hashtableHits = 0;
    static int floatingEOF = 0;
    static int numTests = 0;
    static double testingTime = 0;
    static double codeGenTime = 0;

    public static outputLambda logOutput = (String toPrint) -> System.out.println(toPrint);
    public static outputLambda grammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runGrammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runLogOutput = (String toPrint) -> System.out.println(toPrint);

    static GrammarReader bestGrammar;

    static List<GrammarReader> myGrammars;


    public static int TOUR_SIZE = 6;

    //Used to record the min, avg, max score for a given generation
    static LinkedList<double[]> scores = new LinkedList<double[]>();
    static int MIN_SCORE_INDEX = 0;
    static int AVG_SCORE_INDEX = 1;
    static int MAX_SCORE_INDEX = 2;
    static int SCORE_DELTA_INDEX = 3;
    // Can reduce memory footprint my storing grammarStrings in positiveGrammars and
    // reconstructing when needed
    static LinkedList<GrammarReader> positiveGrammars = new LinkedList<GrammarReader>();
    static LinkedList<GrammarReader> perfectGrammars = new LinkedList<GrammarReader>();
    static HashMap<Double, LinkedList<GrammarReader>> evaluatedGrammars = new HashMap<Double, LinkedList<GrammarReader>>();
    static HashSet<String> generatedGrammars = new HashSet<String>();

    public static void main(String[] args) {
        Chelsea.loadTests(Constants.POS_TEST_DIR, Constants.NEG_TEST_DIR);
        if (Constants.USE_GUI) {
            Application.launch(Gui.class, new String[] {});

            System.exit(0);
        }

        // GrammarReader goldenGrammar = new GrammarReader(new
        // File(Constants.CURR_GRAMMAR_PATH));
        // goldenGrammar.injectEOF();
        GrammarReader seededGrammar = new GrammarReader(new File(Constants.SEEDED_GRAMMAR_PATH));
        seededGrammar.injectEOF();
        runLocaliser(seededGrammar);
    }

    public static void runTests(GrammarReader myReader) {
        if (myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            System.err.println("Flagging " + myReader.getName() + " for removal");
            myReader.flagForRemoval();
            return;
        }
        myReader.injectEOF();

        lamdaArg removeCurr = () -> myReader.flagForRemoval();
        long startTime = System.nanoTime();
        Chelsea.generateSources(myReader, removeCurr);
        myReader.stripEOF();

        if (myReader.toRemove()) {
            System.out.println("Code gen failed for \n" + myReader);
            return;
        }

        codeGenTime += System.nanoTime() - startTime;
        numGrammarsChecked++;
        try {

            int[] testResult = Chelsea.runTestcases(removeCurr, Constants.POS_MODE);
            if (myReader.toRemove()) {
                return;
            }

            Constants.positiveScoring.eval(testResult, myReader);

            testingTime += System.nanoTime() - startTime;
            numTests++;

            //Only do negative testing if all pos tests pass
            if(myReader.getPosScore() == 1.0) {
                testResult = Chelsea.runTestcases(removeCurr, Constants.NEG_MODE);
                Constants.negativeScoring.eval(testResult, myReader);
            }

            
        } catch (Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        } finally {
            //Clears out the generated files
            Chelsea.clearGenerated();
        }
    }

    public static void runTestsWithLocalisation(GrammarReader myReader, scoringLambda scoreCalc) {
        // System.err.println(" " + myReader);
        // List<String> result = runLocaliser(myReader);
        // myReader.setMutationConsideration(result);
    }
    /**
     * Try to infer currGram using localisation, 
     * this does not have a positive and negative testing phase seperation, so only main loop
     */
    public static void demoMainWLocal() {
        try {
            myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE_LOCAL, generatedGrammars, Constants.positiveScoring);
            runLogOutput.output("Generated " + myGrammars.size() + " base grammars\n");
            LinkedList<GrammarReader> totalPop = new LinkedList<GrammarReader>();
            
            for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {

                runLogOutput.output("Inferring " + Constants.CURR_GRAMMAR_NAME + " with localisation");
                runLogOutput.output("Starting generation " + genNum);
                runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");
                
                totalPop.clear();
                totalPop.addAll(myGrammars);
                
                if (genNum != 0) {
                    // runGrammarOutput.output("Computing suggested mutants of " +
                    // myGrammars.stream().map(GrammarReader::getName).collect(Collectors.joining("\n")));
                    // Dont generate mutants on the first gen, we use a lot of initial grammars to
                    // cover search space
                    myGrammars.stream()
                                .map(GrammarReader::computeMutants)
                                .flatMap(LinkedList<GrammarReader>::stream)
                                .peek(App::runTests)
                                .filter(gram -> gram.getPosScore() > 0 && !gram.toRemove())
                                .forEach(totalPop::add);
                }
                

                



                List<Double> scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());

                // runGrammarOutput.output("Score list " + scoreList);

                // Add crossover grammars
                double bestScore = getBestScore();
                // Only start performing crossover if some decent grammars already exist
                if (bestScore > 0.2) {

                    ArrayList<GrammarReader> crossoverPop = performCrossover(scoreList, totalPop);
                    crossoverPop.stream()
                                .peek(App::runTests)
                                .filter(gram -> gram.getPosScore() > 0 && !gram.toRemove())
                                .forEach(totalPop::add);
                                
                }


                totalPop.forEach(gram -> {
                                double currScore = gram.getScore();

                                if(currScore == 1.0) {
                                    perfectGrammars.add(new GrammarReader(gram));
                                    runGrammarOutput.output("Perfect grammar found:\n" + gram);
                                } 

                                if(currScore > getBestScore()) {
                                    runGrammarOutput
                                        .output("\nNew best grammar\nBest score " + getBestScore() + " -> " + currScore);    
                                    runGrammarOutput.output(gram.hashString() + '\n');
                                    setBestGrammar(gram);
                                }

                                if (evaluatedGrammars.containsKey(currScore)) {
                                    evaluatedGrammars.get(currScore).add(gram);
                                } else {
                                    LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                                    thisScoreList.add(gram);
                                    evaluatedGrammars.put(currScore, thisScoreList);
                                }
                            });
                      
                            
                //I removed hall of fame, can be readded here
                // scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
                //             .collect(Collectors.toList());



                //TODO further optimisation can be done by having the localiser program printing out num pos pass, num pos fail, num neg pass, num neg fail
                // it is currently being recorded in the localiser but the pass and fail scores are combined, this way there is no need to use chelseas generation
                //would still require the maven processes though so look into  it
                totalPop.forEach(grammar -> {
                    if(grammar.getMutationConsideration().isEmpty()) {
                        System.err.println("Localizing" + totalPop.indexOf(grammar) + "/" + totalPop.size() + "\n");
                        runLocaliser(grammar);
                    } 
                });

                // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;

                List<GrammarReader> allGrammars = scoreList.stream().map(evaluatedGrammars::get)
                        .flatMap(LinkedList::stream).collect(Collectors.toList());

                int nextGenSize = Math.min(totalPop.size(), Constants.POP_SIZE);
                int tourSize = Math.min(totalPop.size(), TOUR_SIZE);

                for (int i = 0; i < nextGenSize; i++) {
                    myGrammars.add(tournamentSelect(totalPop, tourSize));
                }
                recordMetrics(myGrammars);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            System.err.println("Floating EOF " + floatingEOF);
        }
    }

    // public static void demoMainNoLocal() {
    //     try {

    //         myGrammars = GrammarGenerator.generatePopulation(Constants.INIT_POP_SIZE);
    //         // Postive testing
    //         for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
    //             runLogOutput.output("Inferring " + Constants.CURR_GRAMMAR_NAME + " without localisation");
                
                

    //             LinkedList<GrammarReader> totalPop = new LinkedList<GrammarReader>();
    //             totalPop.addAll(myGrammars);
    //             if (genNum != 0) {
    //                 // runGrammarOutput.output("Computing suggested mutants of " +
    //                 // myGrammars.stream().map(GrammarReader::getName).collect(Collectors.joining("\n")));
    //                 // Dont generate mutants on the first gen, we use a lot of initial grammars to
    //                 // cover search space
    //                 myGrammars.forEach(grammar -> totalPop
    //                         .addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE, generatedGrammars)));
    //             }
    //             totalPop.forEach(grammar -> {
    //                 runGrammarOutput.output("Testing " + totalPop.indexOf(grammar) + "/" + totalPop.size() + "\n" + grammar);

    //                 runTests(grammar, Constants.POS_MODE, Constants.positiveScoring);

    //                 double currScore = grammar.getPosScore();
    //                 if (currScore == 1.0)
    //                     positiveGrammars.add(grammar);
    //                 if (currScore == 1.0)
    //                     runGrammarOutput.output("New positive grammar\n" + grammar);
    //                 if (currScore > getBestScore()) {
    //                     String out = "\nNew best grammar\nBest score " + getBestScore() 
    //                                 + " -> " + currScore + '\n' + grammar.hashString() 
    //                                 + '\n' + grammar.getTerminalRules().stream()
    //                                         .map(Rule::toString).collect(Collectors.joining("\n"));
    //                     runLogOutput.output(out);
    //                     setBestGrammar(grammar);
    //                 }
    //             });
    //             totalPop.removeIf(GrammarReader::toRemove);

    //             totalPop.forEach(grammar -> {
    //                 double currScore = grammar.getScore();
    //                 if (evaluatedGrammars.containsKey(currScore)) {
    //                     evaluatedGrammars.get(currScore).add(grammar);
    //                 } else {
    //                     LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                     thisScoreList.add(grammar);
    //                     evaluatedGrammars.put(currScore, thisScoreList);
    //                 }
    //             });

    //             List<Double> scoreList = evaluatedGrammars.keySet().stream()
    //                                         .sorted(Comparator.reverseOrder())
    //                                         .collect(Collectors.toList());

    //             // Add crossover grammars
    //             double bestScore = scoreList.get(0);
    //             // Only start performing crossover if some decent grammars already exist
    //             if (bestScore > 0.2) {

    //                 ArrayList<GrammarReader> crossoverPop = performCrossover(scoreList, totalPop);
    //                 crossoverPop.forEach(gram -> runTests(gram, Constants.POS_MODE, Constants.positiveScoring));
    //                 crossoverPop.removeIf(GrammarReader::toRemove);
    //                 crossoverPop.forEach(grammar -> {
    //                     double currScore = grammar.getScore();
    //                     if (grammar.getPosScore() == 1.0) {
    //                         positiveGrammars.add(grammar);
    //                         grammarOutput.output("New positive grammar\n" + grammar);
    //                     }
    //                     if (currScore > getBestScore()) {
    //                         runGrammarOutput
    //                                 .output("\nNew best grammar\nBest score " + getBestScore() + " -> " + currScore);
    //                         runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream()
    //                                 .map(Rule::toString).collect(Collectors.joining("\n")));
    //                         setBestGrammar(grammar);
    //                     }
    //                     if (evaluatedGrammars.containsKey(currScore)) {
    //                         evaluatedGrammars.get(currScore).add(grammar);
    //                     } else {
    //                         LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                         thisScoreList.add(grammar);
    //                         evaluatedGrammars.put(currScore, thisScoreList);
    //                     }
    //                 });

    //                 scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
    //                         .collect(Collectors.toList());
    //             }

                
    //             myGrammars.clear();
    //             // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;

    //             List<GrammarReader> allGrammars = scoreList.stream().map(evaluatedGrammars::get)
    //                     .flatMap(LinkedList::stream).collect(Collectors.toList());

    //             for (int i = 0; i < Constants.POP_SIZE; i++) {
    //                 myGrammars.add(tournamentSelect(allGrammars, Constants.TOUR_SIZE));
    //             }
    //             recordMetrics(myGrammars);

    //         }
    //         // Negative testing
    //         for (int genNum = 0; genNum < Constants.NUM_NEGATIVE_ITERATIONS; genNum++) {
    //             runLogOutput.output("Starting generation " + genNum);
    //             runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");

    //             LinkedList<GrammarReader> totalPop = new LinkedList<GrammarReader>();
    //             totalPop.addAll(myGrammars);

    //             myGrammars.forEach(grammar -> totalPop
    //                     .addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE, generatedGrammars)));

    //             totalPop.forEach(grammar -> runTests(grammar, Constants.POS_MODE, Constants.positiveScoring));
    //             totalPop.removeIf(grammar -> grammar.getPosScore() != 1.0 || grammar.toRemove()); //Filters out all the grammars that are invalid or that do not accept positive test cases
    //             totalPop.forEach(grammar -> {
    //                 runTests(grammar, Constants.NEG_MODE, Constants.negativeScoring);
    //                 double newScore = grammar.getNegScore();
    //                 System.out.println("Negative score " + newScore + " for \n" + grammar);
    //                 if (grammar.getNegScore() == 1.0)
    //                     perfectGrammars.add(grammar);
    //                 if (grammar.getScore() > getBestScore()) {
    //                     runGrammarOutput.output(
    //                             (grammar.getNegScore() == 1.0 ? "\nFully matched grammar\n" : "\nNew best grammar\n")
    //                                     + "Best score " + getBestScore() + " -> " + newScore);
    //                     runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream()
    //                             .map(Rule::toString).collect(Collectors.joining("\n")));
    //                     setBestGrammar(grammar);
    //                 }

    //             });

    //             totalPop.removeIf(GrammarReader::toRemove);
                
    //             totalPop.forEach(grammar -> {
    //                 double currScore = grammar.getScore();
    //                 if (evaluatedGrammars.containsKey(currScore)) {
    //                     evaluatedGrammars.get(currScore).add(grammar);
    //                 } else {
    //                     LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                     thisScoreList.add(grammar);
    //                     evaluatedGrammars.put(currScore, thisScoreList);
    //                 }
    //             });

    //             while(totalPop.size() < Constants.POP_SIZE) {
    //                 GrammarReader toAdd = tournamentSelect(positiveGrammars, Constants.TOUR_SIZE);
    //                 totalPop.add(new GrammarReader(toAdd));
    //                 positiveGrammars.add(toAdd);

    //             }

    //             List<Double> scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
    //                     .collect(Collectors.toList());

    //             // Crossover
    //             ArrayList<GrammarReader> crossoverPop = performCrossover(scoreList, totalPop);
    //             crossoverPop.forEach(grammar -> runTests(grammar, Constants.POS_MODE, Constants.positiveScoring));
    //             crossoverPop.removeIf(grammar -> grammar.getPosScore() != 1.0 || grammar.toRemove());
    //             crossoverPop.forEach(grammar -> {
    //                 double currScore = grammar.getScore();
    //                 if (evaluatedGrammars.containsKey(currScore)) {
    //                     evaluatedGrammars.get(currScore).add(grammar);
    //                 } else {
    //                     LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                     thisScoreList.add(grammar);
    //                     evaluatedGrammars.put(currScore, thisScoreList);
    //                 }
    //             });
    //             scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
    //                     .collect(Collectors.toList());

    //             myGrammars.clear();

    //             int grammarsToAdd = Math.min(Constants.POP_SIZE, totalPop.size());
    //             List<GrammarReader> allGrammars = scoreList.stream()
    //                                                         .map(evaluatedGrammars::get)
    //                                                         .flatMap(LinkedList::stream)
    //                                                         .collect(Collectors.toList());

    //             while (myGrammars.size() < grammarsToAdd) {
    //                 int grammarsLeft = grammarsToAdd - myGrammars.size();
    //                 System.err.println(myGrammars.size() + " " + grammarsToAdd + " " + totalPop.size() + " "
    //                         + positiveGrammars.size());
    //                 if (!scoreList.isEmpty()) {
    //                     LinkedList<GrammarReader> currList = evaluatedGrammars.get(scoreList.remove(0));

    //                     if (currList.size() <= grammarsLeft) {
    //                         myGrammars.addAll(currList);
    //                     } else {
    //                         for (int j = 0; j < grammarsLeft; j++) {
    //                             myGrammars.add(currList.remove(randInt(currList.size())));
    //                         }
    //                     }
    //                 } else {
    //                     if (positiveGrammars.size() <= grammarsLeft) {
    //                         myGrammars.addAll(positiveGrammars);
    //                     } else {
    //                         for (int j = 0; j < grammarsLeft; j++) {
    //                             myGrammars.add(positiveGrammars.get(randInt(positiveGrammars.size())));
    //                         }
    //                     }
    //                 }
    //             }
    //         }

    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
    //         System.err.println("Floating EOF " + floatingEOF);
    //     }
    // }

    //Does the ordinary learning w/o localisation but uses both positve and negative testing suites from the start
    // public static void demoMainNoLocalCombScoring() {
    //     try {

    //         myGrammars = GrammarGenerator.generatePopulation(Constants.INIT_POP_SIZE);
    //         // myGrammars = GrammarGenerator.generatePopulation(Constants.INIT_POP_SIZE);
    //         // Postive testing
    //         for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
    //             runLogOutput.output("Inferring " + Constants.CURR_GRAMMAR_NAME + " without localisation");
    //             runLogOutput.output("Starting generation " + genNum);
    //             runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");

    //             LinkedList<GrammarReader> totalPop = new LinkedList<GrammarReader>();
    //             totalPop.addAll(myGrammars);
    //             if (genNum != 0) {
    //                 // Dont generate mutants on the first gen, we use a lot of initial grammars to
    //                 // cover search space
    //                 myGrammars.forEach(grammar -> totalPop
    //                         .addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE, generatedGrammars)));
    //             }
    //             totalPop.forEach(grammar -> {
    //                 runGrammarOutput.output("Testing " + totalPop.indexOf(grammar) + "/" + totalPop.size() + "\n" + grammar);

    //                 runTests(grammar, Constants.POS_MODE, Constants.positiveScoring);
    //                 runTests(grammar, Constants.NEG_MODE, Constants.negativeScoring);

    //                 double currScore = grammar.getScore();
    //                 if (currScore == 1.0) {
    //                     perfectGrammars.add(grammar);
    //                     runGrammarOutput.output("New perfect grammar\n" + grammar);
    //                 }
                    
    //                 if (currScore > getBestScore()) {
    //                     String out = "\nNew best grammar\nBest score " + getBestScore() + " -> " + currScore + '\n' + grammar.hashString() + '\n' + grammar.getTerminalRules().stream()
    //                     .map(Rule::toString).collect(Collectors.joining("\n"));
    //                     runLogOutput.output(out);
    //                     setBestGrammar(grammar);
    //                 }

    //                 if (evaluatedGrammars.containsKey(currScore)) {
    //                     evaluatedGrammars.get(currScore).add(grammar);
    //                 } else {
    //                     LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                     thisScoreList.add(grammar);
    //                     evaluatedGrammars.put(currScore, thisScoreList);
    //                 }
    //             });
    //             totalPop.removeIf(GrammarReader::toRemove);

                

    //             List<Double> scoreList = evaluatedGrammars.keySet().stream()
    //                                         .sorted(Comparator.reverseOrder())
    //                                         .collect(Collectors.toList());

    //             // Add crossover grammars
    //             double bestScore = scoreList.get(0);
    //             // Only start performing crossover if some decent grammars already exist
    //             if (bestScore > 0.2) {

    //                 ArrayList<GrammarReader> crossoverPop = performCrossover(scoreList, totalPop);
    //                 crossoverPop.forEach(gram ->  {
    //                     runTests(gram, Constants.POS_MODE, Constants.positiveScoring);
    //                     runTests(gram, Constants.NEG_MODE, Constants.negativeScoring);
    //                 });
    //                 crossoverPop.removeIf(GrammarReader::toRemove);
    //                 crossoverPop.forEach(grammar -> {
    //                     double currScore = grammar.getScore();
    //                     if (grammar.getScore() == 1.0) {
    //                         perfectGrammars.add(grammar);
    //                         runGrammarOutput.output("New perfect grammar\n" + grammar);
    //                     }
    //                     if (currScore > getBestScore()) {
    //                         runGrammarOutput
    //                                 .output("\nNew best grammar\nBest score " + getBestScore() + " -> " + currScore);
    //                         runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream()
    //                                 .map(Rule::toString).collect(Collectors.joining("\n")));
    //                         setBestGrammar(grammar);
    //                     }
    //                     if (evaluatedGrammars.containsKey(currScore)) {
    //                         evaluatedGrammars.get(currScore).add(grammar);
    //                     } else {
    //                         LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
    //                         thisScoreList.add(grammar);
    //                         evaluatedGrammars.put(currScore, thisScoreList);
    //                     }
    //                 });

    //                 scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
    //                         .collect(Collectors.toList());
    //                 totalPop.addAll(crossoverPop);
    //             }

    //             recordMetrics(totalPop);
    //             // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;

    //             List<GrammarReader> allGrammars = scoreList.stream().map(evaluatedGrammars::get)
    //                     .flatMap(LinkedList::stream).collect(Collectors.toList());

    //             //TODO check whether the suggested mutants are calculated correctly in next gen, the grammars that were localised shouls use guided mutation otherwise do random
    //             myGrammars = selectNewPop(totalPop, allGrammars);
    //             myGrammars.sort(Comparator.reverseOrder());
    //             myGrammars.stream()
    //                         .filter(gram -> gram.getMutationConsideration().isEmpty() && gram.getScore() > 0)
    //                         .limit(10)
    //                         .forEach(gram -> runTestsWithLocalisation(gram, Constants.localiserScoring));
                
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
    //         System.err.println("Floating EOF " + floatingEOF);
    //     }

    // }
    public static void demoMainProgram() {
        demoMainWLocal();
    }
    /**
     * Performs crossover by selecting NUM_CROSSOVER_PER_GEN pairs of grammars from  and producing 2 new grammars
     * if useLocal these new grammars are tested using localisation, else uses Chelsea testing
     * scoreArr is used for alternative selection process where you take middle of the road grammars only else uses tournament selection 
     * @param scoreArr
     * @param useLocal
     * @return The children produced 
     */
    public static ArrayList<GrammarReader> performCrossover(List<Double> scoreArr, List<GrammarReader> grammarPool) {
        // Calculate crossoverPop
        ArrayList<GrammarReader> crossoverPop = new ArrayList<GrammarReader>();
        double numScores = scoreArr.size();
        if (numScores == 0)
            return null;
        double maxIndex = numScores - 1;
        ArrayList<GrammarReader> toConsider = new ArrayList<GrammarReader>();

        // Switch between 2 selection strategies, Bernd suggested using the middle of
        // road grammars for crossover
        if (true) {
            for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
                GrammarReader toAdd1 = tournamentSelect(grammarPool, Constants.TOUR_SIZE);
                GrammarReader toAdd2 = tournamentSelect(grammarPool, Constants.TOUR_SIZE);
                toConsider.add(toAdd1);
                toConsider.add(toAdd2);
                myGrammars.add(toAdd1);
                myGrammars.add(toAdd2);
            }

        } else {
            if (maxIndex % 2 == 0) {
                toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int) (maxIndex / 2))));
            } else {
                toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int) Math.floor(maxIndex / 2))));
                toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int) Math.ceil(maxIndex / 2))));
            }
        }
        for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
            GrammarReader base1 = toConsider.remove(randInt(toConsider.size()));
            GrammarReader base2 = toConsider.remove(randInt(toConsider.size()));
            GrammarReader g1 = new GrammarReader(base1);
            GrammarReader g2 = new GrammarReader(base2);
            System.out.println("Performing crossover on " + g1 + " \nand\n" + g2);
            g1.applyCrossover(g2, runGrammarOutput);
            g1.setName(base1.getName() + "XX"  + base2.getName());
            g2.setName(base2.getName() + "XX"  + base1.getName());
            System.out.println("Produced " + g1 + "\nand\n" + g2);
            int g1Counter = 0;
            if(generatedGrammars.contains(g1.hashString()) && g1Counter++ < 5) {
                g1 = new GrammarReader(base1);
                g1.applyCrossover(new GrammarReader(base2),  runGrammarOutput);
            }
            if(g1Counter < 5) {
                crossoverPop.add(g1);
            }
            int g2Counter = 0;
            if(generatedGrammars.contains(g2.hashString()) && g2Counter++ < 5) {
                g2 = new GrammarReader(base2);
                g2.applyCrossover(new GrammarReader(base1),  runGrammarOutput);
            }
            if(g2Counter < 5) {
                crossoverPop.add(g2);
            }

        }
        return crossoverPop;

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
        GrammarReader hashedOut = new GrammarReader(out.hashString() + '\n'
                + out.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        demoGrammar = hashedOut;
        hashedOut.setName("demoGrammar");
        ArrayList<String> reachables = new ArrayList<String>();
        demoGrammar.getParserRules().get(0).getReachables(demoGrammar.getParserRules(), reachables);
        // out.getParserRules().forEach(rule -> System.out.println(rule.getName() +
        // rule.isSingular()));
        grammarOutput.output(hashedOut.toString());
    }

    public static void ruleCountDemo() {
        demoGrammar.demoChangeRuleCount(logOutput, grammarOutput);
        grammarOutput.output("\n" + demoGrammar.toString());
    }

    public static void symbolCountDemo() {
        demoGrammar.demoChangeSymbolCount(logOutput, grammarOutput);
        grammarOutput.output("\n" + demoGrammar.toString());
    }

    public static void groupDemo() {
        demoGrammar.demoGroupMutate(logOutput, grammarOutput);
        grammarOutput.output("\n" + demoGrammar.toString());
    }

    public static void symbMutateDemo() {
        demoGrammar.demoMutate(logOutput, grammarOutput);
        grammarOutput.output("\n" + demoGrammar.toString());
    }

    public static void demoHeuristic() {
        demoGrammar.demoHeuristic(logOutput, grammarOutput);
        grammarOutput.output("\n" + demoGrammar.toString());
    }

    static void setRunGrammarOutput(outputLambda newRun) {
        runGrammarOutput = newRun;
    }

    static void setRunLogOutput(outputLambda newRun) {
        runLogOutput = newRun;
    }

    static void setBestGrammar(GrammarReader newBest) {
        bestGrammar = new GrammarReader(newBest.hashString() + '\n'
                + newBest.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        bestGrammar.setName("bestCandidate");
        bestGrammar.setNegScore(newBest.getNegScore());
        bestGrammar.setPosScore(newBest.getPosScore());
        System.out.println("New Best grammar with score  " + getBestScore());
    }

    static double getBestScore() {
        if (bestGrammar == null)
            return -1;
        return bestGrammar.getScore();
    }

    static String getBestGrammarString() {
        if (bestGrammar == null)
            return "";
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

    public static List<String> getSusList() {

        try (Scanner myScanner = new Scanner(new File(Constants.CSV_PATH))) {
            HashMap<Double, List<String>> allScores = new HashMap<Double, List<String>>();
            myScanner.nextLine();
            while (myScanner.hasNext()) {

                String[] currLine = myScanner.nextLine().split(",");
                Double currScore = Double.parseDouble(currLine[5]);
                if (allScores.containsKey(currScore)) {
                    allScores.get(currScore).add(currLine[0]);
                } else {
                    List<String> toAdd = new LinkedList<String>();
                    toAdd.add(currLine[0]);
                    allScores.put(currScore, toAdd);
                }
            }

            return allScores.keySet().stream().sorted(Comparator.reverseOrder()).map(allScores::get)
                    .flatMap(List::stream).collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedList<String>();
    }

    //TODO eliminate the use of scoring func duing localization
    /**
     * Runs localiser on current grammar, sets the mutation suggestions of current grammar to a list of strings of the form (ruleName:prodIndex)
     * @param currGrammar  Grammer being tested
     */
    public static void runLocaliser(GrammarReader currGrammar) {
        List<String> out = new LinkedList<String>();
        try (FileWriter outFile = new FileWriter(new File(Constants.localizerGPath))) {
            currGrammar.injectEOF();
            outFile.append(currGrammar.toString().replaceFirst(currGrammar.getName(), "UUT"));
            outFile.close();
            // System.out.println("Localising " + currGrammar.getScore() + "\n" + currGrammar);
            Process testProc = Runtime.getRuntime().exec("sh -c " + Constants.localizerSPath);

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(testProc.getInputStream()));
            HashMap<Double, LinkedList<String>> susScore = new HashMap<Double, LinkedList<String>>();

            //For now this readLine is here to consume the output from the scripts, can be removed if not needed
            String result = inputReader.readLine();
            // if (result.equals("codeGenFailed")) {
            //     return null;
            // }

            // if (inputReader.toString().contains("nan"))
            //     return null;
            inputReader.lines()
                        .map(line -> line.split(","))
                        .filter(data -> !(data[0].equals("program:1") || data[5].equals("nan")))
                        .forEach(data -> {
                            Double tarantula = Double.valueOf(data[5]);
                            if (susScore.containsKey(tarantula)) {
                                susScore.get(tarantula).add(data[0]);
                            } else {
                                LinkedList<String> toAdd = new LinkedList<String>();
                                toAdd.add(data[0]);
                                susScore.put(tarantula, toAdd);
                            }
                        });
            

            susScore.keySet().stream()
                                .sorted(Comparator.reverseOrder())
                                .map(susScore::get)
                                .flatMap(LinkedList::stream)
                                .forEach(out::add);


            System.err.println("Mutation considerations for " +  currGrammar + "\n " + out);
            currGrammar.setMutationConsideration(out);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            currGrammar.stripEOF();
        }
    }

    /**
     * Performs a round of tournament selection on pop using tourSize tournament
     * size
     * 
     * @param pop      population to select from
     * @param tourSize size of the tournament
     * @return
     */
    public static GrammarReader tournamentSelect(List<GrammarReader> pop, int tourSize) {
        System.err.println("Performing tournament selection on a population of " + pop.size());
        LinkedList<GrammarReader> tour = new LinkedList<GrammarReader>();
        while (tour.size() < tourSize) {
            tour.add(pop.get(randInt(pop.size())));
        }
        GrammarReader out = tour.stream().max(Comparator.comparing(GrammarReader::getScore)).get();
        pop.remove(out);
        return out;
    }

    public static void recordMetrics(List<GrammarReader> pop) {
        double minScore = pop.stream().mapToDouble(GrammarReader::getScore).min().getAsDouble();
        double avgScore = pop.stream().mapToDouble(GrammarReader::getScore).average().getAsDouble();
        double maxScore = pop.stream().mapToDouble(GrammarReader::getScore).max().getAsDouble();
        double scoreDelta = scores.size() == 0 ? 0 : maxScore - scores.getLast()[MAX_SCORE_INDEX];
        scores.add(new double[]{minScore, avgScore, maxScore, scoreDelta});
    }


    public static void RECORD_MIN_SCORE(double score) {
        scores.getLast()[MIN_SCORE_INDEX] = score;
    }

    public static void RECORD_AVG_SCORE(double score) {
        scores.getLast()[AVG_SCORE_INDEX] = score;
    }

    public static void RECORD_MAX_SCORE(double score) {
        scores.getLast()[MAX_SCORE_INDEX] = score;
    }

    public static void RECORD_SCORE_DELTA(double score) {
        scores.getLast()[SCORE_DELTA_INDEX] = score;
    }

    public static List<GrammarReader> selectNewPop(List<GrammarReader> currGen, List<GrammarReader> HOF) {
        List<GrammarReader> out = new LinkedList<>();
        for (int i = 0; i < Constants.HALL_OF_FAME_COUNT; i++) {
            out.add(tournamentSelect(HOF, Constants.TOUR_SIZE));
        }
        

        for (int i = 0; i < Constants.POP_SIZE - myGrammars.size(); i++) {
            out.add(tournamentSelect(currGen, Constants.TOUR_SIZE));
        }
        return out;
    }
}
