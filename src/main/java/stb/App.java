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
import java.util.function.Predicate;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javafx.application.Application;
import static java.util.Comparator.reverseOrder;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;

public class App {
    static Gram demoGrammar;
    static Gram demoGrammar2;
    private static int hashtableHits = 0;
    static int floatingEOF = 0;
    static int numTests = 0;
    static List<Double> testingTime = new LinkedList<Double>();
    static List<Double> mutTime = new LinkedList<Double>();
    static double codeGenTime = 0;
    static long startTime = 0;
    
    static LinkedList<Gram> totalPop = new LinkedList<Gram>();
    
    static Predicate<Gram> notYetChecked = gram -> !totalPop.contains(gram);
    
    public static outputLambda demoOut = (String toPrint) -> System.out.println(toPrint);
    private static outputLambda runOut = (String toPrint) -> System.out.println(toPrint);
    
    static Gram bestGrammar;
    static String bestGrammarString = "";
    
    static List<Gram> myGrammars;
    
    public static int TOUR_SIZE = 6;

    private interface loggerFunc {
        public Double getVal(Object in);
    }


    
    // Used to record the min, avg, max score for a given generation
    static HashMap<String, LinkedList<Double>> scores = new HashMap<>();
    static HashMap<String, loggerFunc> logFuncs = new HashMap<>();

    static String MIN_SCORE_METRIC = "MIN_SCORE_METRIC";
    static String AVG_SCORE_METRIC = "AVG_SCORE_METRIC";
    static String MAX_SCORE_METRIC = "MAX_SCORE_METRIC";
    static String SCORE_DELTA_METRIC = "SCORE_DELTA_METRIC";
    static String NUM_PASS_POS_METRIC = "NUM_PASS_POS_METRIC";
    static String NUM_PASS_NEG_METRIC = "NUM_PASS_NEG_METRIC";
    static String NUM_PASS_TOT_METRIC = "NUM_PASS_TOT_METRIC";
    static String TIME_PER_GEN_METRIC = "TIME_PER_GEN_METRIC";
    static String TOTAL_GRAMMARS_METRIC = "TOTAL_GRAMS_METRIC";
    static String HASH_TABLE_HITS_METRIC = "HASHTABLE_HITS_METRIC";
    static String RAW_SEL = "RAW_SEL";
    static String DEL_SEL = "DEL_SEL";
    static String RAN_SEL = "RAN_SEL";
    static String SOURCE_TIME_SEL = "SOURCE_TIME_METRIC";
    static String MUTANT_COMP_TIME = "MUTANT_COMPUTATION_TIME";
    static String LOCALIZATION_TIME = "LOCAL_TIME";
    static String TEST_TIME = "TEST_TIME";
    static String LR_RECURS = "LR_RECURS";
    static String TOTAL_MUTS = "TOTAL_MUTS";
    

    
    static List<String> LOG_EVERY_GEN = List.of(MIN_SCORE_METRIC, AVG_SCORE_METRIC, MAX_SCORE_METRIC, SCORE_DELTA_METRIC, 
                            NUM_PASS_POS_METRIC, NUM_PASS_NEG_METRIC, NUM_PASS_TOT_METRIC, 
                            TIME_PER_GEN_METRIC, TOTAL_GRAMMARS_METRIC, HASH_TABLE_HITS_METRIC);


    
    // Can reduce memory footprint my storing grammarStrings in positiveGrammars and
    // reconstructing when needed
    static LinkedList<Gram> perfectGrammars = new LinkedList<Gram>();
    // static List<Gram> loggedGrammars = loadLoggedGrammars(Constants.LOG_GRAMMAR_PATH);
    static HashMap<Double, LinkedList<Gram>> evaluatedGrammars = new HashMap<Double, LinkedList<Gram>>();
    private static HashSet<String> generatedGrammars = new HashSet<String>();

    public static void main(String[] args) {
        
        Chelsea.loadTests(Constants.POS_TEST_DIR, Constants.NEG_TEST_DIR);
        if (Constants.USE_GUI) {
            Application.launch(Gui.class, new String[] {});
            System.exit(0);
        } else {
            Gram myGram = new Gram(new File(Constants.SEEDED_GRAMMAR_PATH));
            // myGram.setMutationConsideration(List.of("akfyqsjjzp:2"));
            List<Rule>  targetProd = myGram.getAllRules().get(0).getSubRules().get(0);
            myGram.cleanEmptyClosure(targetProd);
            System.err.println(myGram.containsImmediateLRDeriv() ? " contains LR" : "NO LR :((((");
            // myGram.computeMutants();
        }
    }

    public static void demoMainProgram() {
        try {
            generatedGrammars.clear();
            evaluatedGrammars.clear();
            perfectGrammars.clear();
            startTime = System.currentTimeMillis();
            rloSetText("Inferring " + Constants.CURR_GRAMMAR_NAME + " with localisation\n");
            
            myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE_LOCAL);
            rloAppendText("Generated " + myGrammars.size() + " base grammars\n");
            
            int genNum = 0;
            for (genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                startTime = System.nanoTime();
                testingTime.clear();
                mutTime.clear();
                rloSetText("Gen:" + genNum + "\n" + "Hashtable hits : " + hashtableHits + "\n");
                System.err.println("Gen:" + genNum + "\n" + "Hashtable hits : " + hashtableHits);
                // GNSetText(genNum);

                totalPop.clear();
                totalPop.addAll(myGrammars);

                if (genNum != 0) {
                    // runGrammarOutput.output("Computing suggested mutants of " +
                    // myGrammars.stream().map(GrammarReader::getName).collect(Collectors.joining("\n")));
                    // Dont generate mutants on the first gen, we use a lot of initial grammars to
                    // cover search space
                    rloAppendText("Computing mutants\n");
                    long timeStart = System.nanoTime();
                    
                    Gram.currGramNum = 0;
                    Gram.totalBaseGrams = myGrammars.size();
                    List<Gram> allMutants = myGrammars.stream()
                                        .map(Gram::computeMutants)
                                        .flatMap(LinkedList<Gram>::stream)
                                        .collect(toCollection(ArrayList::new));

                    mutTime.add((System.nanoTime()-timeStart)/Math.pow(10,9));
                    
                    timeStart = System.nanoTime();
                    allMutants.forEach(App::runTests);
                    testingTime.add((System.nanoTime()-timeStart)/Math.pow(10,9));
                    allMutants.removeIf(Gram.passesPosTest.negate());

                    recordMetric(TEST_TIME, testingTime.stream().reduce(0.0, Double::sum));
                    recordMetric(MUTANT_COMP_TIME, mutTime.stream().reduce(0.0, Double::sum));
                    logMetric(MUTANT_COMP_TIME);
                    logMetric(TEST_TIME);

                    rloAppendText(allMutants.size() + " total mutants\n");

                    // Ensure that grammars that have passed posTests are present in this generation
                    // allMutants.stream().filter(notYetChecked.and(Gram.passesPosTest)).sorted(reverseOrder()).limit(20)
                    //         .forEach(totalPop::add);

                    // System.err.println("Added top 15 grammars that also pass postests, time taken: " + ((System.nanoTime()-timeStart)/Math.pow(10,9)));

                    timeStart = System.nanoTime();

                    allMutants.stream().filter(notYetChecked).sorted(reverseOrder()).limit(15).forEach(totalPop::add);
                    
                    double rawSel = (System.nanoTime()-timeStart)/Math.pow(10,9);
                    recordMetric(RAW_SEL, rawSel);
                    timeStart = System.nanoTime();
                    
                    allMutants.stream().sorted(reverseOrder()).limit(15).forEach(totalPop::add);
                    
                    allMutants.stream().filter(notYetChecked).sorted((g0, g1) -> -1 * Gram.CompPosScoreDelta(g0, g1))
                    .limit(15).forEach(totalPop::add);
                    
                    double delSel = (System.nanoTime()-timeStart)/Math.pow(10,9);
                    recordMetric(DEL_SEL, delSel);
                    timeStart = System.nanoTime();
                    // Add random grammars as well
                    totalPop.addAll(randomSelection(allMutants, 15));
                    
                    System.err.println("Added 15 random grammars to next generation, time taken: " + (System.nanoTime()-timeStart));
                }

                List<Double> scoreList = evaluatedGrammars.keySet().stream().sorted(reverseOrder()).collect(toList());

                // Add crossover grammars
                double bestScore = getBestScore();
                // Only start performing crossover if some decent grammars already exist
                if (bestScore > 0.6) {

                    ArrayList<Gram> crossoverPop = performCrossover(scoreList, totalPop);
                    crossoverPop.stream().peek(App::runTests).filter(Gram.passesPosTest).forEach(totalPop::add);

                }

                final int currGenNum = genNum;
                totalPop.forEach(gram -> {
                    double currScore = gram.getScore();
                    if (currScore == 1.0) {
                        Gram toAdd = new Gram(gram);
                        toAdd.genNum = currGenNum;
                        perfectGrammars.add(new Gram(gram));
                        rgoSetText("Perfect grammar found:\n" + gram);

                    }
                    if (currScore > getBestScore()) {
                        String out = "New best grammar\nBest score " + getBestScore() + " -> " + currScore + "\n"
                                + gram.hashString() + '\n';
                        long numPassPos = Arrays.stream(gram.passPosArr).filter(a -> a).count();
                        long numPassNeg = Arrays.stream(gram.passNegArr).filter(a -> a).count();
                        out += "passes " + numPassPos + " positive tests" + " " + gram.getPosScore() + "\n" + numPassNeg
                                + "negative tests" + gram.getNegScore();
                        rcoSetText(out);
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

                // I removed hall of fame, can be readded here
                // scoreList =
                // evaluatedGrammars.keySet().stream().sorted(Comparator.reverseOrder())
                // .collect(Collectors.toList());

                List<Gram> allGrammars = scoreList.stream()
                                            .map(evaluatedGrammars::get)
                                            .flatMap(LinkedList::stream)
                                            .collect(toList());

                int nextGenSize = Math.min(totalPop.size(), Constants.POP_SIZE);
                int tourSize = Math.min(totalPop.size(), TOUR_SIZE);
                myGrammars.clear();
                for (int i = 0; i < nextGenSize; i++) {
                    myGrammars.add(tournamentSelect(totalPop, tourSize));
                }
                recordPopMetrics(myGrammars);
                logMetric(MAX_SCORE_METRIC);
                logMetric(NUM_PASS_TOT_METRIC);
                logMetric(TIME_PER_GEN_METRIC);
                logMetric(NUM_PASS_POS_METRIC);
                logMetric(NUM_PASS_NEG_METRIC);
                logMetric(TOTAL_GRAMMARS_METRIC);
                logMetric(HASH_TABLE_HITS_METRIC);
                logMetric(RAW_SEL);
                logMetric(DEL_SEL);
                logMetric(RAN_SEL);

                recordMetric(LR_RECURS, Double.valueOf(Gram.NUM_LR));
                recordMetric(TOTAL_MUTS, Double.valueOf(Gram.NUM_MUTS));
                logMetric(LR_RECURS);
                logMetric(TOTAL_MUTS);

                
                
                try(FileWriter out = new FileWriter(new File(Constants.LOG_DIR+"/"+ (Constants.USE_LOCALIZATION ? "local_" : "") +System.nanoTime()))) {
                    out.write(bestGrammar.fullHashString());
                } catch(Exception e) {
    
                }


                long timeStart = System.nanoTime();
                myGrammars.forEach(grammar -> {
                    System.err.println(myGrammars.indexOf(grammar) + "/" + myGrammars.size());

                    if(Constants.USE_LOCALIZATION && grammar.getMutationConsideration().isEmpty()) {
                        runLocaliser(grammar);
                    } else {
                        grammar.genFakeSuggestions();
                    }
                });

                double localizationTime = (System.nanoTime()-timeStart)/Math.pow(10, 9);
                recordMetric(LOCALIZATION_TIME, localizationTime);
                logMetric(LOCALIZATION_TIME);

                // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;

            }
            if (perfectGrammars.size() > 0) {
                writePerfectGrammars(perfectGrammars, genNum);

            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            System.err.println("Floating EOF " + floatingEOF + "genNum " + scores.get(MIN_SCORE_METRIC).size());
            logMetric(MAX_SCORE_METRIC);
            logMetric(NUM_PASS_TOT_METRIC);
            logMetric(TIME_PER_GEN_METRIC);
            logMetric(TOTAL_GRAMMARS_METRIC);
            try(FileWriter out = new FileWriter(new File(Constants.LOG_DIR+"/"+ (Constants.USE_LOCALIZATION ? "local_" : "") +System.nanoTime()))) {
                out.write(bestGrammar.fullHashString());
            } catch(Exception e) {

            }
        }
    }

    public static void runTests(Gram myReader) {

        // System.err.println("Testing " + myReader);
        if (myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            // System.err.println("Flagging " + myReader.getName() + "\n for removal");
            myReader.flagForRemoval();
            return;
        }
        // System.err.println("Injecting eof");

        myReader.injectEOF();
        lamdaArg removeCurr = () -> myReader.flagForRemoval();
        long startTime = System.nanoTime();
        Chelsea.generateSources(myReader, removeCurr);
        // System.err.println("Sources generated");
        if (myReader.toRemove()) {
            System.err.println("Code gen failed for \n" + myReader);
            return;
        }

        codeGenTime += System.nanoTime() - startTime;

        try {


            int[] testResult = Chelsea.runTestcases(removeCurr, Constants.POS_MODE);
            if (myReader.toRemove()) {
                return;
            }

            Constants.positiveScoring.eval(testResult, myReader);
            myReader.stripEOF();

            numTests++;

            // Only do negative testing if all pos tests pass
            // this || true is to get parity with how localiser hanndles testing
            //you dont have to neg score here, only do pos scoring thats what we care about, can check neg scoring when doing localisation
            testResult = Chelsea.runTestcases(removeCurr, Constants.NEG_MODE);
            Constants.negativeScoring.eval(testResult, myReader);

        } catch (Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        } finally {
            // Clears out the generated files
            Chelsea.clearGenerated();
        }
    }

    static void writePerfectGrammars(List<Gram> grammars, int genNum) {
        System.out.println("Found " + perfectGrammars.size() + " perfect grammars");
        grammars.forEach(gram -> {
            try (FileWriter out = new FileWriter(new File(gram.getName()))) {
                out.write(String.format("startTime: %s\nfoundTime%s\niteration count: %d\n%s",
                        System.currentTimeMillis(), gram.genNum, gram));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Performs crossover by selecting NUM_CROSSOVER_PER_GEN pairs of grammars from
     * and producing 2 new grammars if useLocal these new grammars are tested using
     * localisation, else uses Chelsea testing scoreArr is used for alternative
     * selection process where you take middle of the road grammars only else uses
     * tournament selection
     * 
     * param scoreArr
     * @param useLocal
     * @return The children produced
     */
    public static ArrayList<Gram> performCrossover(List<Double> scoreArr, List<Gram> grammarPool) {
        // Calculate crossoverPop
        ArrayList<Gram> crossoverPop = new ArrayList<Gram>();
        double numScores = scoreArr.size();
        // if (numScores == 0)
        // return null;
        double maxIndex = numScores - 1;
        ArrayList<Gram> toConsider = new ArrayList<Gram>();

        // Switch between 2 grammar pool selection strategies, Bernd suggested using the
        // middle of
        // road grammars for crossover
        // new selection strategy selectes
        if (true) {
            for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
                Gram toAdd1 = tournamentSelect(grammarPool, Constants.TOUR_SIZE);
                grammarPool.stream().sorted(Comparator.reverseOrder()).limit(10)
                        .sorted((g0, g1) -> toAdd1.compPosPassSimil(g0, g1)).limit(3).forEach(toConsider::add);

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
            g1.setName(base1.getName() + "XX" + base2.getName());
            g2.setName(base2.getName() + "XX" + base1.getName());
            System.out.println("Produced " + g1 + "\nand\n" + g2);
            crossoverPop.add(g1);
            crossoverPop.add(g2);

        }
        // remove all grammars that are already in the generatedGrammars hashset
        crossoverPop.removeIf(App::gramAlreadyChecked);
        return crossoverPop;

    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static void setDemoOut(outputLambda toSet) {
        demoOut = toSet;
    }

    public static void loadStartGrammar() {
        Gram out = new Gram(new File(Constants.SEEDED_GRAMMAR_PATH));
        Gram hashedOut = new Gram(out.hashString() + '\n'
                + out.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        demoGrammar = hashedOut;
        hashedOut.setName("demoGrammar");
        ArrayList<String> reachables = new ArrayList<String>();
        // demoGrammar.getParserRules().get(0).getReachables(demoGrammar.getParserRules(),
        // reachables);
        // out.getParserRules().forEach(rule -> System.out.println(rule.getName() +
        // rule.isSingular()));
        // demoOut.output(hashedOut.toString());
    }

    public static void displayStartGrammar() {
        goSetText(demoGrammar.toString() + "\n");
    }

    public static String[] getDemoTargets() {
        displayStartGrammar();
        List<String> allTargets = demoGrammar.getAllSuggestions();
        allTargets.add(0, "Random");
        return allTargets.toArray(String[]::new);
    }

    public static void newNTDemo(String target) {
        displayStartGrammar();
        Optional<String> Otarget = target.equals("Random") ? Optional.empty() : Optional.of(target);
        demoGrammar.demoNewNT(Otarget);
        goAppendText("\nLeads to \n" + demoGrammar);
    }

    public static void symbolCountDemo(String target) {
        displayStartGrammar();
        Optional<String> Otarget = target.equals("Random") ? Optional.empty() : Optional.of(target);
        demoGrammar.demoChangeSymbolCount(Otarget);
        goAppendText("\nLeads to \n" + demoGrammar);
        // demoOut.output("\n" + demoGrammar.toString());
    }

    public static void groupDemo(String target) {
        displayStartGrammar();
        Optional<String> Otarget = target.equals("Random") ? Optional.empty() : Optional.of(target);
        demoGrammar.demoGroupMutate(Otarget);
        goAppendText("\nLeads to \n" + demoGrammar);
    }

    public static void demoHeuristic(String target) {
        displayStartGrammar();
        Optional<String> Otarget = target.equals("Random") ? Optional.empty() : Optional.of(target);
        demoGrammar.demoHeuristic(Otarget);
        goAppendText("\nLeads to \n" + demoGrammar);
    }

    public static void symbMutateDemo() {
        demoGrammar.demoMutate(demoOut, demoOut);
        demoOut.output("\n" + demoGrammar.toString());
    }

    public static void demoCrossover() {
        Gram g1 = GrammarGenerator.generatePopulation(1).getFirst();
        Gram g2 = GrammarGenerator.generatePopulation(1).getFirst();
        goClear();

        StringBuilder out = new StringBuilder();
        out.append(String.format("Generated 2 grammars to demonstate crossover\n%s\n%s\nand\n%s\n%s", g1.getName(),
                g1.prettyPrintRules(g1.getParserRules()), g2.getName(), g2.prettyPrintRules(g2.getParserRules())));

        // goSetText(g1.hashString() + "\n\n");
        // goAppendText(g2.hashString() + "\n\n");
        Gram.loggedCrossover(g1, g2, out);
        out.append(String.format("\n\nResults in \n%s\n%s\nand\n%s\n%s", g1.getName(),
                g1.prettyPrintRules(g1.getParserRules()), g2.getName(), g2.prettyPrintRules(g2.getParserRules())));
        // goAppendText(g1.hashString() + "\n\n");
        // goAppendText(g2.hashString() + "\n\n");
        goSetText(out.toString());
    }

    static void setRunOut(outputLambda newRun) {
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
        // System.err.println("Pos pass " + Arrays.toString(newBest.passPosArr));
        // System.err.println("Neg pass " + Arrays.toString(newBest.passNegArr));

        // blockRead("New Best grammar with score " + getBestScore());
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

    /**
     * Runs localiser on current grammar, sets the mutation suggestions of current
     * grammar to a list of strings of the form (ruleName:prodIndex)
     * 
     * @param currGrammar Grammer being tested
     */
    public static void runLocaliser(Gram currGrammar) {
        List<String> out = new LinkedList<String>();
        try {
            currGrammar.injectEOF();
            Chelsea.Localise(currGrammar);
            // System.out.println("Localising " + currGrammar.getScore() + "\n" +
            // currGrammar);
            Process testProc = Runtime.getRuntime().exec("sh -c " + Constants.localScript);

            BufferedReader inputReader = new BufferedReader(new InputStreamReader(testProc.getInputStream()));
            HashMap<Double, LinkedList<String>> susScore = new HashMap<Double, LinkedList<String>>();

            // The first line is always going to be program:1 so we throw it out
            inputReader.readLine();

            inputReader.lines().map(line -> line.split(",")).forEach(data -> {
                Double tarantula = Double.valueOf(data[5]);
                if (susScore.containsKey(tarantula)) {
                    susScore.get(tarantula).add(data[0]);
                } else {
                    LinkedList<String> toAdd = new LinkedList<String>();
                    toAdd.add(data[0]);
                    susScore.put(tarantula, toAdd);
                }
            });

            susScore.keySet().stream().sorted(reverseOrder()).map(susScore::get).flatMap(LinkedList::stream)
                    .forEach(out::add);

            System.err.println("Mutation considerations for " + currGrammar + "\n " + out);
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
        // System.err.println("Performing tournament selection on a population of " +
        // pop.size());
        LinkedList<Gram> tour = new LinkedList<Gram>();
        while (tour.size() < tourSize) {
            tour.add(pop.get(randInt(pop.size())));
        }
        Gram out = tour.stream().max(Comparator.comparing(Gram::getScore)).get();
        pop.remove(out);
        return out;
    }

    /**
     * Randomly selects min(pop.size, size) unique grammars from pop
     * 
     * @param pop
     * @param size
     * @return List of unique grammars
     */
    public static List<Gram> randomSelection(List<Gram> pop, int size) {
        size = Math.min(pop.size(), size);
        Set<Gram> out = new HashSet<>();
        while (out.size() < size)
            out.add(randGet(pop, true));
        return new LinkedList<>(out);
    }

    /**
     * Records all metrics relating to the entire pop
     * @param pop
     */
    public static void recordPopMetrics(List<Gram> pop) {
        
        double minScore = pop.stream().mapToDouble(Gram::getScore).min().getAsDouble();
        double avgScore = pop.stream().mapToDouble(Gram::getScore).average().getAsDouble();
        double maxScore = pop.stream().mapToDouble(Gram::getScore).max().getAsDouble();
        double scoreDelta = scores.getOrDefault(MAX_SCORE_METRIC, new LinkedList<>()).size() == 0 ? 0 : maxScore - scores.get(MAX_SCORE_METRIC).getLast();
        double posTestPass = pop.stream().mapToLong(Gram::numPassPos).max().getAsLong();
        double negTestPass = pop.stream().mapToLong(Gram::numPassNeg).max().getAsLong();
        double allTestPass = negTestPass+posTestPass;
        double timeTaken = (System.nanoTime()-startTime)/(Math.pow(10,9));
        double numGrams = generatedGrammars.size();

        recordMetric(MIN_SCORE_METRIC, minScore);
        recordMetric(AVG_SCORE_METRIC, avgScore);
        recordMetric(MAX_SCORE_METRIC, maxScore);
        recordMetric(SCORE_DELTA_METRIC, scoreDelta);
        recordMetric(NUM_PASS_POS_METRIC, posTestPass);
        recordMetric(NUM_PASS_NEG_METRIC, negTestPass);
        recordMetric(NUM_PASS_TOT_METRIC, allTestPass);
        recordMetric(TIME_PER_GEN_METRIC, timeTaken);
        recordMetric(TOTAL_GRAMMARS_METRIC, numGrams);
        recordMetric(HASH_TABLE_HITS_METRIC, Double.valueOf(hashtableHits));
    }

    public static void recordMetric(String key, Double val) {
        scores.putIfAbsent(key, new LinkedList<Double>());
        scores.get(key).add(val);
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
        return Chelsea.getDirectoryFiles(new File(path)).stream().map(Gram::new)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static void blockRead(String heading) {
        System.err.println(heading);
        try {
            System.in.read();
        } catch (Exception e) {

        }
    }

    /**
     * output wrappers These functions produce tokens that get passed to GUI where
     * they are interpreted
     */

    // RGO tokens
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

    public static void rgoPush() {
        runOut.output(Gui.RGOToken(Gui.pushToken()));
    }

    public static void rgoPop() {
        runOut.output(Gui.RGOToken(Gui.popToken()));
    }

    // RLO tokens
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

    public static void rloPush() {
        runOut.output(Gui.RLOToken(Gui.pushToken()));
    }

    public static void rloPop() {
        runOut.output(Gui.RLOToken(Gui.popToken()));
    }

    public static void goSetText(String toSet) {
        demoOut.output(Gui.GOToken(Gui.setToken(toSet)));
    }

    public static void goAppendText(String input) {
        demoOut.output(Gui.GOToken(Gui.appendToken(input)));
    }

    public static void goPush() {
        demoOut.output(Gui.GOToken(Gui.pushToken()));
    }

    public static void goPop() {
        demoOut.output(Gui.GOToken(Gui.popToken()));
    }

    public static void goClear() {
        demoOut.output(Gui.GOToken(Gui.clearToken()));
    }

    public static void loSetText(String toSet) {
        demoOut.output(Gui.LOToken(Gui.setToken(toSet)));
    }

    public static void loAppendText(String input) {
        demoOut.output(Gui.LOToken(Gui.appendToken(input)));
    }

    public static void loClear() {
        demoOut.output(Gui.LOToken(Gui.clearToken()));
    }

    public static void loPush() {
        runOut.output(Gui.LOToken(Gui.pushToken()));
    }

    public static void loPop() {
        runOut.output(Gui.LOToken(Gui.popToken()));
    }

    // RCO tokens
    public static void rcoSetText(String toSet) {
        runOut.output(Gui.RCOToken(Gui.setToken(toSet)));
    }

    public static void rcoAppendText(String input) {
        runOut.output(Gui.RCOToken(Gui.appendToken(input)));
    }

    public static void rcoClear() {
        runOut.output(Gui.RCOToken(Gui.clearToken()));
    }

    public static void rcoUpdate(String newText) {
        runOut.output(Gui.RCOToken(Gui.updateToken(newText)));
    }

    public static void rcoPush() {
        runOut.output(Gui.RCOToken(Gui.pushToken()));
    }

    public static void rcoPop() {
        runOut.output(Gui.RCOToken(Gui.popToken()));
    }

    public static void GNSetText(int genNum) {
        runOut.output(Gui.GENNUMToken(Gui.setToken("generation: " + genNum)));
    }




    public static <E> E randGet(List<E> input, boolean replace) {
        if (replace) {
            return input.get(randInt(input.size()));
        } else {
            return input.remove(randInt(input.size()));
        }
    }

    public static boolean gramAlreadyChecked(Gram gram) {
        if (generatedGrammars.contains(gram.hashString())) {
            hashtableHits++;
            return true;
        } else {
            generatedGrammars.add(gram.hashString());
            return false;
        }
    }

    public static void logMetrics(List<String> key) {
        key.forEach(App::logMetric);
    }

    public static void logMetric(String key) {
        String fileName =  (Constants.USE_LOCALIZATION ? "local_" : "") + (Constants.ANY_PASS_POS ? "passAny_" : "passPos_") + Constants.CURR_GRAMMAR_NAME + "_"  + key + "_" + Rule.genName() + ".json";
        try (FileWriter out = new FileWriter(Constants.LOG_DIR+"/"+fileName)) {
            StringBuilder outText = new StringBuilder();
            outText.append("\"numGens\":" + scores.get(key).size() + ",\n");
            outText.append("\"dataPoints\":" + Arrays.toString(scores.get(key).stream().toArray(Double[]::new)) + ",\n");
            outText.append("\"legend\":\"myLegend\"" + ",\n");
            outText.append("\"title\":\""+ key + " over time\"" + "\n");

            out.write(String.format("{\n %s \n}", outText.toString()));


        } catch(Exception e) {

        }
    }

    // public static Double[] extractMetric(int index) {
    //     System.err.println("Scores");
    //     System.err.println(scores.stream().map(Arrays::toString).collect(Collectors.joining("\n")));
    //     return scores.stream()
    //             .map(entry -> entry[index])
    //             .toArray(Double[]::new);
    // }


    
    
}
