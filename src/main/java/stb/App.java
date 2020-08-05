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
    static Gram demoGrammar;
    static Gram demoGrammar2;
    static int numGrammarsChecked = 0;
    static int hashtableHits = 0;
    static int floatingEOF = 0;
    static int numTests = 0;
    static double testingTime = 0;
    static double codeGenTime = 0;
    static long startTime = 0;

    public static outputLambda logOutput = (String toPrint) -> System.out.println(toPrint);
    public static outputLambda grammarOutput = (String toPrint) -> System.out.println(toPrint);
    private static outputLambda runOut = (String toPrint) -> System.out.println(toPrint);

    static Gram bestGrammar;
    static String bestGrammarString = "";

    static List<Gram> myGrammars;


    public static int TOUR_SIZE = 6;

    //Used to record the min, avg, max score for a given generation
    static LinkedList<double[]> scores = new LinkedList<double[]>();
    static int MIN_SCORE_INDEX = 0;
    static int AVG_SCORE_INDEX = 1;
    static int MAX_SCORE_INDEX = 2;
    static int SCORE_DELTA_INDEX = 3;
    // Can reduce memory footprint my storing grammarStrings in positiveGrammars and
    // reconstructing when needed
    static LinkedList<Gram> positiveGrammars = new LinkedList<Gram>();
    static LinkedList<Gram> perfectGrammars = new LinkedList<Gram>();
    static List<Gram> loggedGrammars = loadLoggedGrammars(Constants.LOG_GRAMMAR_PATH);
    static HashMap<Double, LinkedList<Gram>> evaluatedGrammars = new HashMap<Double, LinkedList<Gram>>();
    static HashSet<String> generatedGrammars = new HashSet<String>();


    //TODO compare .java files produces by chelsea and localiser
    //TODO move constants into relevant files

    public static void main(String[] args) {
        Chelsea.loadTests(Constants.POS_TEST_DIR, Constants.NEG_TEST_DIR);
        if (Constants.USE_GUI) {
            Application.launch(Gui.class, new String[] {});

            System.exit(0);
        }

        // GrammarReader goldenGrammar = new GrammarReader(new
        // File(Constants.CURR_GRAMMAR_PATH));
        // goldenGrammar.injectEOF();
        Gram seededGrammar = new Gram(new File(Constants.SEEDED_GRAMMAR_PATH));
        seededGrammar.injectEOF();
        runLocaliser(seededGrammar);
    }

    public static void runTests(Gram myReader) {
        if (myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            System.err.println("Flagging " + myReader + "\n for removal");
            myReader.flagForRemoval();
            return;
        }
        
        myReader.injectEOF();
        lamdaArg removeCurr = () -> myReader.flagForRemoval();
        long startTime = System.nanoTime();
        Chelsea.generateSources(myReader, removeCurr);
        
        if (myReader.toRemove()) {
            System.err.println("Code gen failed for \n" + myReader);
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
            myReader.stripEOF();

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

    public static void runTestsWithLocalisation(Gram myReader, scoringLambda scoreCalc) {
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
            startTime = System.currentTimeMillis();
            rloSetText("Inferring " + Constants.CURR_GRAMMAR_NAME + " with localisation\n");
            myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE_LOCAL, generatedGrammars);
            rloAppendText("Generated " + myGrammars.size() + " base grammars\n");
            
            LinkedList<Gram> totalPop = new LinkedList<Gram>();
            
            int genNum = 0;
            for (genNum = 0; perfectGrammars.size() == 0 && genNum < Constants.NUM_ITERATIONS; genNum++) {
                rloSetText("Gen:" + genNum + "\n" + "Hashtable hits : " + hashtableHits + "\n");
                rloAppendText(bestGrammarString);
                
                totalPop.clear();
                totalPop.addAll(myGrammars);
                
                if (genNum != 0) {
                    // runGrammarOutput.output("Computing suggested mutants of " +
                    // myGrammars.stream().map(GrammarReader::getName).collect(Collectors.joining("\n")));
                    // Dont generate mutants on the first gen, we use a lot of initial grammars to
                    // cover search space

                    List<Gram> allMutants = myGrammars.stream()
                                    .map(Gram::computeMutants)
                                    .flatMap(LinkedList<Gram>::stream)
                                    .peek(App::runTests)
                                    .filter(gram -> gram.getPosScore() > 0 && !gram.toRemove())
                                    .collect(Collectors.toCollection(ArrayList::new));

                    allMutants.stream()
                        .filter(gram -> !totalPop.contains(gram))
                        .sorted(Comparator.reverseOrder())
                        .limit(15)
                        .forEach(totalPop::add);

                    System.err.println("Added top 15 grammars by raw score to next generation");
                    
                    allMutants.stream()
                    .filter(gram -> !totalPop.contains(gram))
                    .sorted((g0, g1) -> -1*Gram.CompPosScoreDelta(g0, g1))
                    .limit(15)
                    .forEach(totalPop::add);
                    
                    System.err.println("Added top 15 grammars by raw score to next generation");

                }
                

                



                List<Double> scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());

                // runGrammarOutput.output("Score list " + scoreList);

                // Add crossover grammars
                double bestScore = getBestScore();
                // Only start performing crossover if some decent grammars already exist
                if (bestScore > 0.2) {

                    ArrayList<Gram> crossoverPop = performCrossover(scoreList, totalPop);
                    crossoverPop.stream()
                                .peek(App::runTests)
                                .filter(gram -> gram.getPosScore() > 0 && !gram.toRemove())
                                .forEach(totalPop::add);
                                
                }

                final int currGenNum = genNum;
                totalPop.forEach(gram -> {
                                double currScore = gram.getScore();

                                if(currScore == 1.0) {
                                    Gram toAdd = new Gram(gram);
                                    toAdd.genNum = currGenNum;
                                    perfectGrammars.add(new Gram(gram));
                                    rgoSetText("Perfect grammar found:\n" + gram);
                                    
                                } 

                                if(currScore > getBestScore()) {
                                    String out = "New best grammar\nBest score " 
                                                    + getBestScore() + " -> " + currScore + "\n" 
                                                    + gram.hashString() + '\n';

                                    rgoSetText(out);
                                    setBestGrammar(gram);

                                }

                                if (evaluatedGrammars.containsKey(currScore)) {
                                    evaluatedGrammars.get(currScore).add(gram);
                                } else {
                                    LinkedList<Gram> thisScoreList = new LinkedList<Gram>();
                                    thisScoreList.add(gram);
                                    evaluatedGrammars.put(currScore, thisScoreList);
                                }
                            });
                      
                            
                //I removed hall of fame, can be readded here
                // scoreList = evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
                //             .collect(Collectors.toList());



                List<Gram> allGrammars = scoreList.stream().map(evaluatedGrammars::get)
                        .flatMap(LinkedList::stream).collect(Collectors.toList());

                int nextGenSize = Math.min(totalPop.size(), Constants.POP_SIZE);
                int tourSize = Math.min(totalPop.size(), TOUR_SIZE);
                myGrammars.clear();
                for (int i = 0; i < nextGenSize; i++) {
                    myGrammars.add(tournamentSelect(totalPop, tourSize));
                }
                recordMetrics(myGrammars);

                //TODO further optimisation can be done by having the localiser program printing out num pos pass, num pos fail, num neg pass, num neg fail
                // it is currently being recorded in the localiser but the pass and fail scores are combined, this way there is no need to use chelseas generation
                //would still require the maven processes though so look into  it
                myGrammars.forEach(grammar -> {
                    if(grammar.getMutationConsideration().isEmpty()) {
                        System.err.println("Localizing" + myGrammars.indexOf(grammar) + "/" + myGrammars.size() + "\n");
                        runLocaliser(grammar);
                    } 
                });

                // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;


            }
            if(perfectGrammars.size() > 0) {
                writePerfectGrammars(perfectGrammars, genNum);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            System.err.println("Floating EOF " + floatingEOF);
        }
    }


    static void writePerfectGrammars(List<Gram> grammars, int genNum) {
        System.out.println("Found " + perfectGrammars.size() + " perfect grammars");
        grammars.forEach(gram -> {
            try(FileWriter out = new FileWriter(new File(gram.getName()))) {
                out.write(String.format("startTime: %s\nfoundTime%s\niteration count: %d\n%s", System.currentTimeMillis(), gram.genNum, gram));
            } catch(Exception e) {
                e.printStackTrace();
            }
        });
    }

    
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
    public static ArrayList<Gram> performCrossover(List<Double> scoreArr, List<Gram> grammarPool) {
        // Calculate crossoverPop
        ArrayList<Gram> crossoverPop = new ArrayList<Gram>();
        double numScores = scoreArr.size();
        if (numScores == 0)
            return null;
        double maxIndex = numScores - 1;
        ArrayList<Gram> toConsider = new ArrayList<Gram>();

        // Switch between 2 selection strategies, Bernd suggested using the middle of
        // road grammars for crossover
        if (true) {
            for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
                Gram toAdd1 = tournamentSelect(grammarPool, Constants.TOUR_SIZE);
                Gram toAdd2 = tournamentSelect(grammarPool, Constants.TOUR_SIZE);
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
            Gram base1 = toConsider.remove(randInt(toConsider.size()));
            Gram base2 = toConsider.remove(randInt(toConsider.size()));
            Gram g1 = new Gram(base1);
            Gram g2 = new Gram(base2);
            System.out.println("Performing crossover on " + g1 + " \nand\n" + g2);
            Gram.Crossover(g1, g2);
            g1.setName(base1.getName() + "XX"  + base2.getName());
            g2.setName(base2.getName() + "XX"  + base1.getName());
            System.out.println("Produced " + g1 + "\nand\n" + g2);

            if(!generatedGrammars.contains(g1.hashString())) {
                generatedGrammars.add(g1.hashString());
                crossoverPop.add(g1);
            }

            if(!generatedGrammars.contains(g2.hashString())) {
                generatedGrammars.add(g2.hashString());
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
        Gram out = new Gram(new File(Constants.SEEDED_GRAMMAR_PATH));
        GrammarGenerator.fillBlanks(out);
        Gram hashedOut = new Gram(out.hashString() + '\n'
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
        runOut = newRun;
    }

    static void setRunLogOutput(outputLambda newRun) {
        runOut = newRun;
    }

    static void setBestGrammar(Gram newBest) {
        bestGrammar = new Gram(newBest.hashString() + '\n'
                + newBest.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        bestGrammar.setName("bestCandidate");
        bestGrammar.setNegScore(newBest.getNegScore());
        bestGrammar.setPosScore(newBest.getPosScore());
        StringBuilder newBestString = new StringBuilder("Best Score: " + newBest.getScore() + "\n");
        newBestString.append(newBest.fullHashString());
        bestGrammarString = newBestString.toString();
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
        Gram g1 = GrammarGenerator.generatePopulation(1).getFirst();
        Gram g2 = GrammarGenerator.generatePopulation(1).getFirst();
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
    public static void runLocaliser(Gram currGrammar) {
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
    public static Gram tournamentSelect(List<Gram> pop, int tourSize) {
        System.err.println("Performing tournament selection on a population of " + pop.size());
        LinkedList<Gram> tour = new LinkedList<Gram>();
        while (tour.size() < tourSize) {
            tour.add(pop.get(randInt(pop.size())));
        }
        Gram out = tour.stream().max(Comparator.comparing(Gram::getScore)).get();
        pop.remove(out);
        return out;
    }

    public static void recordMetrics(List<Gram> pop) {
        double minScore = pop.stream().mapToDouble(Gram::getScore).min().getAsDouble();
        double avgScore = pop.stream().mapToDouble(Gram::getScore).average().getAsDouble();
        double maxScore = pop.stream().mapToDouble(Gram::getScore).max().getAsDouble();
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

    public static List<Gram> selectNewPop(List<Gram> currGen, List<Gram> HOF) {
        List<Gram> out = new LinkedList<>();
        for (int i = 0; i < Constants.HALL_OF_FAME_COUNT; i++) {
            out.add(tournamentSelect(HOF, Constants.TOUR_SIZE));
        }
        

        for (int i = 0; i < Constants.POP_SIZE - myGrammars.size(); i++) {
            out.add(tournamentSelect(currGen, Constants.TOUR_SIZE));
        }
        return out;
    }

    public static List<Gram> loadLoggedGrammars(String path) {
        return Chelsea.getDirectoryFiles(new File(path))
                .stream()
                .map(Gram::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }



    /**
     * output wrappers
     * These functions produce tokens that get passed to GUI where they are interpreted
     */

    //RGO tokens
    public static void rgoSetText(String toSet) {
        runOut.output(Gui.RGOToken(Gui.setToken(toSet)));
    }
    
    public static void rgoAppendText(String input) {
        runOut.output(Gui.RGOToken(Gui.appendToken(input)));
    }

    public static void rgoClear() {
        runOut.output(Gui.RGOToken(Gui.clearToken()));
    }

    public static void rgoUpdate(String newText) {
        runOut.output(Gui.RGOToken(Gui.updateToken(newText)));
    }


    //RLO tokens
    public static void rloSetText(String toSet) {
        runOut.output(Gui.RLOToken(Gui.setToken(toSet)));
    }

    public static void rloAppendText(String input) {
        runOut.output(Gui.RLOToken(Gui.appendToken(input)));
    }

    public static void rloClear() {
        runOut.output(Gui.RLOToken(Gui.clearToken()));
    }

    public static void rloUpdate(String newText) {
        runOut.output(Gui.RLOToken(Gui.updateToken(newText)));
    }
    

    public static void goSetText(String toSet) {
        runOut.output(Gui.GOToken(Gui.setToken(toSet)));
    }

    public static void goAppendText(String input) {
        runOut.output(Gui.GOToken(Gui.appendToken(input)));
    }

    public static void goClear() {
        runOut.output(Gui.GOToken(Gui.clearToken()));
    }

    public static void loSetText(String toSet) {
        runOut.output(Gui.LOToken(Gui.setToken(toSet)));
    }

    public static void loAppendText(String input) {
        runOut.output(Gui.LOToken(Gui.appendToken(input)));
    }

    public static void loClear() {
        runOut.output(Gui.LOToken(Gui.clearToken()));
    }
    
    
}
