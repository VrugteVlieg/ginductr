package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javafx.application.Application;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.lang.Math.min;

public class App {
    static String outputID = Constants.CURR_GRAMMAR_NAME + "_" + LocalDateTime.now().toString();
    static Gram demoGrammar;
    static Gram demoGrammar2;
    private static int hashtableHits = 0;
    static int floatingEOF = 0;
    static int numTests = 0;
    public static int numBrokenGrammars = 0;
    static int numToTest = 0;
    static int currTestNum = 0;

    static LinkedList<Gram> totalPop = new LinkedList<Gram>();

    static Predicate<Gram> notYetChecked = gram -> !totalPop.contains(gram);

    public static outputLambda demoOut = (String toPrint) -> System.out.println(toPrint);
    private static outputLambda runOut = (String toPrint) -> System.out.println(toPrint);

    static Gram bestGrammar;
    static String bestGrammarString = "";

    static List<Gram> myGrammars;

    public static int TOUR_SIZE = 10;

    private interface loggerFunc {
        public Double getVal(Stream<Gram> in);
    }

    private static class metricLog {
        List<Double> AVG_SCORE;
        List<Double> MAX_SCORE;
        List<Double> SCORE_DELTA;

        metricLog() {
            AVG_SCORE = scores.get(AVG_SCORE_METRIC);
            MAX_SCORE = scores.get(MAX_SCORE_METRIC);
            SCORE_DELTA = scores.get(SCORE_DELTA_METRIC);
        }
    }

    // Used to record the min, avg, max score for a given generation
    static HashMap<String, LinkedList<Double>> scores = new HashMap<>();
    static HashMap<String, loggerFunc> logFuncs = new HashMap<>();

    static String AVG_SCORE_METRIC = "AVG_SCORE_METRIC";
    static String MAX_SCORE_METRIC = "MAX_SCORE_METRIC";
    static String SCORE_DELTA_METRIC = "SCORE_DELTA_METRIC";
    static String NUM_PASS_POS_METRIC = "NUM_PASS_POS_METRIC";
    static String NUM_PASS_NEG_METRIC = "NUM_PASS_NEG_METRIC";
    static String NUM_PASS_TOT_METRIC = "NUM_PASS_TOT_METRIC";
    static String TOTAL_GRAMMARS_METRIC = "TOTAL_GRAMS_METRIC";
    static String HASH_TABLE_HITS_METRIC = "HASHTABLE_HITS_METRIC";
    static String MAIN_START_TIME = "MAIN_START_TIME";

    static void setupLogging() {
        logFuncs.clear();
        logFuncs.put(MAX_SCORE_METRIC, (g) -> g.mapToDouble(Gram::getScore).max().getAsDouble());
        logFuncs.put(AVG_SCORE_METRIC, (g) -> g.mapToDouble(Gram::getScore).average().getAsDouble());
        logFuncs.put(NUM_PASS_POS_METRIC,
                (g) -> (double) g.max(Comparator.comparing(Gram::getScore)).get().numPassPos());
        logFuncs.put(NUM_PASS_NEG_METRIC,
                (g) -> (double) g.max(Comparator.comparing(Gram::getScore)).get().numPassNeg());
        logFuncs.put(NUM_PASS_TOT_METRIC, (g) -> {
            Gram b = g.max(Comparator.comparing(Gram::getScore)).get();
            return (double) (b.numPassNeg() + b.numPassPos());
        });

        logFuncs.put(SCORE_DELTA_METRIC, g -> {
            if (scores.get(MAX_SCORE_METRIC) == null || scores.get(MAX_SCORE_METRIC).isEmpty())
                return 0.0;
            double prevMax = scores.get(MAX_SCORE_METRIC).getLast();
            double currMax = g.mapToDouble(Gram::getScore).max().getAsDouble();
            // System.err.println("Prev max was " + prevMax + " curr max is " + currMax);
            return currMax - prevMax;
        });

        logFuncs.put(TOTAL_GRAMMARS_METRIC, g -> (double) generatedGrammars.size());
        logFuncs.put(HASH_TABLE_HITS_METRIC, g -> (double) hashtableHits);

    }

    static void resetProg() {
        evaluatedGrammars.clear();
        generatedGrammars.clear();
        perfectGrammars.clear();
        scores.clear();
        numBrokenGrammars = 0;
        bestGrammar = null;
        bestGrammarString = "";
        // myGrammars.clear();
        hashtableHits = 0;
        stopwatch.clear();
        setupLogging();
        clearANTLRfolder();
    }

    static Optional<File> currLogFile = empty();
    static StringBuilder outText = new StringBuilder();

    static LinkedList<Gram> perfectGrammars = new LinkedList<Gram>();

    // Grammars that have been evaluated during the current generation
    static HashMap<Double, LinkedList<Gram>> evaluatedGrammars = new HashMap<Double, LinkedList<Gram>>();

    // Hashstrings of all the grammars that have been produced
    private static HashSet<String> generatedGrammars = new HashSet<String>();

    static Timer stopwatch = new Timer();

    public static void main(String[] args) {

        Chelsea.loadTests(Constants.POS_TEST_DIR, Constants.NEG_TEST_DIR);
        if (Constants.USE_GUI && false) {
            Application.launch(Gui.class, new String[] {});
            System.exit(0);
        } else {
            mainWTuning();
            // clearANTLRfolder();
            GrammarGenerator.readFromMLCS(Constants.CURR_MLCS_PATH);
            // Gram myGram = GrammarGenerator.generatePopulation(100).getFirst();
            // runTests(myGram);
            // System.err.println(myGram);
            stopwatch.startClock();
            Gram myGram = GrammarGenerator.generateLocalisablePop(10).getFirst();

            System.err.println("With mlcs takes " + stopwatch.split());
            System.err.println(myGram.hashString());
            generatedGrammars.clear();
            GrammarGenerator.skeletonBases = null;
            GrammarGenerator.generateLocalisablePop(10);
            System.err.println("Without mlcs takes " + stopwatch.split());
            // Gram seeded = new Gram(new File(Constants.SEEDED_GRAMMAR_PATH));
            // seeded.groupMutate(seeded.getParserRules().get(0).getSubRules().get(0));
            // seeded.scrambleRuleNames();
            // System.err.println(seeded.getParserRules().get(0).getReachables(seeded.getParserRules()));
            // stopwatch.startClock();
            // Gram.loggedCrossover(pop.get(0), pop.get(1), new StringBuilder());
            // System.err.println(pop.get(0).prevCross.toString());
            // for (int tCount : new int[] { 1, 4, 8, 16 }) {
            // Constants.NUM_THREADS = tCount;
            // runTests(pop);
            // System.err.println("Using " + Constants.NUM_THREADS + pop.size() + " grams
            // took " + stopwatch.split());

            // }
        }
    }

    /**
     * TODO setup tuning decide what to compare Convergence rate for Local non-local
     * on Dyck4, toy, miniJava max score across 100gens Impact of using MLCS for
     * initialization comapared to random initial grammars
     */

    /**
     * Structure of paper Can we successfully infer using GP Show that blind search
     * works for smaller grammars, struggles for larger ones Introduce Local as a
     * way to guide mutation, show comparison Introduce MLCS as a way to improve
     * initial grammars, show comparison
     */

    public static void mainWTuning() {
        System.err.println(System.getenv("set"));
        String baseOutputID = (Constants.USE_LOCALIZATION ? "local_" : "") + Constants.CURR_GRAMMAR_NAME
                + System.getenv("outLbl").replaceAll(" ", "");

        createLogDir(baseOutputID);
        Constants.P_CHANGE_RULE_COUNT = Double.parseDouble(System.getenv("pCRC"));
        Constants.P_CHANGE_SYMBOL_COUNT = Double.parseDouble(System.getenv("pCSC"));
        Constants.P_M = Double.parseDouble(System.getenv("pM"));
        Constants.P_H = Double.parseDouble(System.getenv("pH"));
        Double ts = Double.parseDouble(System.getenv("tS"));
        Constants.TOUR_SIZE = Math.max((int) (Constants.POP_SIZE * ts), 1);
        String format = format("CRC:%f_CS:%f_PM:%f_PH:%f_TS:%f", Constants.P_CHANGE_RULE_COUNT,
                Constants.P_CHANGE_SYMBOL_COUNT, Constants.P_M, Constants.P_H, ts);
        createLogDir(baseOutputID + "/" + format);
        for (int runCount = 0; runCount < 2; runCount++) {
            System.err.println("Testing " + format + "_" + runCount + "@ " + LocalDateTime.now().toString());
            System.err.println(Constants.getParamString());
            outputID = String.join("/", List.of(baseOutputID, format, "run_" + runCount));
            stopwatch.startClock(MAIN_START_TIME);
            benchmarkMain();
        }

    }

    public static void benchmarkMain() {
        stopwatch.startClock();
        System.err.println("resetting program");
        resetProg();
        System.err.println("Reset took " + stopwatch.elapsedTime());
        myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE);
        stopwatch.startClock("GEN");
        for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
            totalPop.clear();
            totalPop.addAll(myGrammars);
            System.err.println(
                    "Starting  gen " + genNum + "@" + LocalDateTime.now() + "   prevGen: " + stopwatch.split("GEN"));
            if (genNum != 0) {
                System.err.println("Generating mutants from  " + myGrammars.size() + " base grammars");
                List<Gram> allMutants = new ArrayList<>();
                for (int i = 0; i < myGrammars.size(); i++) {
                    System.err.print(format("%d/%d\r", i, myGrammars.size()));
                    allMutants.addAll(Gram.computeMutants(myGrammars.get(i)));
                }
                System.err.println();

                allMutants.addAll(GrammarGenerator.generateLocalisablePop(Constants.FRESH_POP));
                // System.err.println("TotalPop in: " + totalPop.size() + "\n" + "numMuts: " +
                // allMutants.size());
                stopwatch.startClock("mutTest");
                System.err.println("Starting mutTesting with " + allMutants.size() + " grams");
                runTests(allMutants);
                System.err.println(
                        format("Testing %d grams took %f", allMutants.size(), stopwatch.elapsedTime("mutTest")));
                // allMutants.stream()
                // .filter(Gram.passesPosTest.negate())
                // .map(Gram::hashString)
                // .forEach(GrammarGenerator.nullGrams::add);

                // allMutants.removeIf(Gram.passesPosTest.negate());
                totalPop.addAll(allMutants);
                // allMutants.stream().filter(g ->
                // !App.gramAlreadyChecked(g)).forEach(totalPop::add);
                // System.err.println("TotalPop out: " + totalPop.size());
            }

            // Add crossover grammars
            double bestScore = getBestScore();
            // Only start performing crossover if some decent grammars already exist
            if (bestScore > 0.55) {

                ArrayList<Gram> crossoverPop = performCrossover(totalPop);
                runTests(crossoverPop);
                crossoverPop.removeIf(Gram.passesPosTest.negate());
                // Select grammars from xoverPop using tour select and add to totalPop
                for (int i = 0; i < min(10, crossoverPop.size()); i++) {
                    totalPop.add(tournamentSelect(crossoverPop, TOUR_SIZE));
                }
            }
            String bestGramName = "";
            for (Gram gram : totalPop) {
                double currScore = gram.getScore();
                System.err.print(currScore + "\r");

                if (currScore == 1.0) {
                    Gram toAdd = new Gram(gram);
                    toAdd.genNum = genNum;
                    perfectGrammars.add(toAdd);
                }
                if (currScore > getBestScore()) {
                    bestGramName = gram.getName();
                    setBestGrammar(gram);
                }
                // if (evaluatedGrammars.containsKey(currScore)) {
                // evaluatedGrammars.get(currScore).add(gram);
                // } else {
                // LinkedList<Gram> thisScoreList = new LinkedList<Gram>();
                // thisScoreList.add(gram);
                // evaluatedGrammars.put(currScore, thisScoreList);
                // }
            }
            System.err.println();

            int nextGenSize = Math.min(totalPop.size(), Constants.POP_SIZE);
            int tourSize = Math.min(totalPop.size(), TOUR_SIZE);
            // System.err.println("Next gen size: " + nextGenSize);
            // System.err.println("TG:" + totalPop.size());
            // System.err.println("MG:" + myGrammars.size());
            // System.err.println("TS:" + tourSize);
            myGrammars.clear();

            System.err.println("Recording metrics of " + totalPop.size() + " gen: " + genNum);
            logFuncs.keySet().forEach(k -> recordMetric(k, totalPop));

            for (int i = 0; i < nextGenSize; i++) {
                myGrammars.add(tournamentSelect(totalPop, tourSize));
            }

            for (Gram g : myGrammars) {
                if (Constants.USE_LOCALIZATION) {
                    if (g.getMutationConsideration().isEmpty())
                        // System.err.println("\nLocalising " + g + "\nscore: " + g.getScore());
                        runLocaliser(g);
                    if (g.getName().equals(bestGramName))
                        setBestGrammar(g);
                } else {
                    g.genFakeSuggestions();
                }
            }

            createLogDir(outputID);
            logMetricsJSON(new metricLog());
            if (perfectGrammars.size() > 0) {
                writePerfectGrammars(perfectGrammars, genNum);
            } else {
                writeGrammar(bestGrammar, genNum);
            }
        }
    }

    public static void demoMainProgram() {
        try {
            outputID = (Constants.USE_LOCALIZATION ? "local_" : "") + Constants.CURR_GRAMMAR_NAME
                    + LocalDateTime.now().toString();
            System.err.println("Inferring with" + (Constants.USE_LOCALIZATION ? " localization" : " no localization"));
            resetProg();
            rloSetText("Inferring " + Constants.CURR_GRAMMAR_NAME
                    + (Constants.USE_LOCALIZATION ? " with  localisation\n" : "\n"));
            stopwatch.startClock();
            stopwatch.startClock(MAIN_START_TIME);
            myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE);
            rloAppendText(format("Generated %d base grammars in %f\n", myGrammars.size(), stopwatch.elapsedTime()));

            int genNum = 0;
            for (genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                numBrokenGrammars = 0;
                stopwatch.startClock("GEN_START");

                rloSetText(format("Gen: %d\n", genNum));

                totalPop.clear();
                totalPop.addAll(myGrammars);

                if (genNum != 0) {
                    // Dont generate mutants on the first gen, we use a lot of initial grammars to
                    // cover search space
                    rloAppendText("Computing mutants\n");
                    stopwatch.startClock();

                    Gram.currGramNum = 0;
                    Gram.totalBaseGrams = myGrammars.size();
                    List<Gram> allMutants = new ArrayList<>();
                    int counter = 0;
                    for (Gram g : myGrammars) {
                        System.err.print("Computing mutants for " + (counter++) + "/" + myGrammars.size() + "\r");
                        rgoSetText("Computing mutants for " + (counter++) + "/" + myGrammars.size() + "\r");
                        // allMutants.addAll(Gram.computeMutantsBoogaloo(g));
                        allMutants.addAll(Gram.computeMutants(g));
                    }
                    rloAppendText("Done Computing mutants\n");
                    // myGrammars.stream().map(Gram::computeMutants)
                    // .flatMap(LinkedList<Gram>::stream).collect(toCollection(ArrayList::new));

                    allMutants.addAll(GrammarGenerator.generateLocalisablePop(Constants.FRESH_POP));
                    numToTest = allMutants.size();
                    System.err.println(format("Testing %d grams", numToTest));
                    rloAppendText(format("Testing %d grams\n", numToTest));
                    int progress = 0;
                    // for (Gram gram : allMutants) {
                    // System.err.print(format("Testing: %d/%d\r", progress, allMutants.size()));
                    // rgoSetText(format("Testing: %d/%d\r", progress++, allMutants.size()));
                    // runTests(gram);
                    // }

                    runTests(allMutants);
                    rloAppendText("Done testing\n");
                    // allMutants.forEach(App::runTests);
                    allMutants.removeIf(Gram.passesPosTest.negate());

                    // rloAppendText(allMutants.size() + " total mutants\n");

                    allMutants.stream().filter(notYetChecked).forEach(totalPop::add);
                }

                // Add crossover grammars
                double bestScore = getBestScore();
                // Only start performing crossover if some decent grammars already exist
                if (bestScore > 0.55) {

                    ArrayList<Gram> crossoverPop = performCrossover(totalPop);
                    runTests(crossoverPop);
                    crossoverPop.removeIf(Gram.passesPosTest.negate());
                    for (int i = 0; i < min(10, crossoverPop.size()); i++) {
                        totalPop.add(tournamentSelect(crossoverPop, TOUR_SIZE));
                    }
                }

                for (Gram gram : totalPop) {
                    double currScore = gram.getScore();
                    if (currScore == 1.0) {
                        Gram toAdd = new Gram(gram);
                        toAdd.genNum = genNum;
                        perfectGrammars.add(toAdd);
                        rgoSetText("Perfect grammar found:\n" + toAdd);

                        // blockRead("Perfect grammar found: " + perfectGrammars.size() +"\n" + gram);

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
                        System.err.println(out);
                    }
                    if (evaluatedGrammars.containsKey(currScore)) {
                        evaluatedGrammars.get(currScore).add(gram);
                    } else {
                        LinkedList<Gram> thisScoreList = new LinkedList<Gram>();
                        thisScoreList.add(gram);
                        evaluatedGrammars.put(currScore, thisScoreList);
                    }
                }

                // List<Gram> allGrammars =
                // scoreList.stream().map(evaluatedGrammars::get).flatMap(LinkedList::stream)
                // .collect(toList());

                Chelsea.logSlowGrammars(genNum);
                Chelsea.clearSlowGrammars();

                int nextGenSize = Math.min(totalPop.size(), Constants.POP_SIZE);
                int tourSize = Math.min(totalPop.size(), TOUR_SIZE);
                myGrammars.clear();
                totalPop.stream().sorted(Comparator.comparing(Gram::getScore).reversed()).limit(10)
                        .forEach(myGrammars::add);
                for (int i = 0; i < nextGenSize - myGrammars.size(); i++) {
                    myGrammars.add(tournamentSelect(totalPop, tourSize));
                }

                // recordMetric(TEST_TIME, testingTime.stream().reduce(0.0, Double::sum));
                // logMetric(TEST_TIME);

                // recordMetric(CODE_GEN_TIME, codeGenTime.stream().reduce(0.0, Double::sum));
                // logMetric(CODE_GEN_TIME);
                // blockRead("Code gen took " + codeGenTime.stream().reduce(0.0, Double::sum));

                // recordPopMetrics(myGrammars);

                // logMetric(List.of(MAX_SCORE_METRIC, TIME_PER_GEN_METRIC, NUM_PASS_POS_METRIC,
                // NUM_PASS_NEG_METRIC, HASH_TABLE_HITS_METRIC));

                logFuncs.keySet().forEach(k -> recordMetric(k, totalPop));
                if (!perfectGrammars.isEmpty()) {
                    // perfectGrammars.forEach(g -> g.logGrammar(false));
                    break;
                }

                // try (FileWriter out = new FileWriter(new File(
                // Constants.LOG_DIR + "/" + (Constants.USE_LOCALIZATION ? "local_" : "") +
                // System.nanoTime()))) {
                // out.write(bestGrammar.fullHashString());
                // } catch (Exception e) {

                // }

                int gCount = 0;
                for (Gram g : myGrammars) {
                    // System.err.print("Localizing " + (gCount) + "/" + myGrammars.size() + "\r");
                    rgoSetText("Localizing " + (gCount++) + "/" + myGrammars.size() + "\r");
                    if (Constants.USE_LOCALIZATION) {
                        if (g.getMutationConsideration().isEmpty())
                            runLocaliser(g);
                    } else {
                        g.genFakeSuggestions();
                    }
                }

            }
            createLogDir(outputID);
            if (perfectGrammars.size() > 0) {
                logFuncs.keySet().forEach(App::logMetric);
                writePerfectGrammars(perfectGrammars, genNum);
            } else {
                logFuncs.keySet().forEach(App::logMetric);
                writeGrammar(bestGrammar, genNum);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            try (FileWriter out = new FileWriter(new File(
                    Constants.LOG_DIR + "/" + (Constants.USE_LOCALIZATION ? "local_" : "") + System.nanoTime()))) {
                out.write(bestGrammar.fullHashString());
            } catch (Exception e) {

            }
        }
    }

    public static void runTests(Gram myReader) {

        if (myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            myReader.flagForRemoval();
            return;
        }

        myReader.injectEOF();

        Chelsea.generateSources(myReader);

        if (myReader.toRemove()) {
            System.err.println("Code gen failed for \n" + myReader);
            return;
        }

        try {

            int[] testResult = Chelsea.runTestcases(Constants.POS_MODE, myReader);
            if (myReader.toRemove()) {
                return;
            }

            Constants.positiveScoring.eval(testResult, myReader);
            myReader.stripEOF();

            numTests++;

            testResult = Chelsea.runTestcases(Constants.NEG_MODE, myReader);
            Constants.negativeScoring.eval(testResult, myReader);

        } catch (Exception e) {
            System.err.println("Exception in runTests " + e.getCause());

        } finally {
            // Clears out the generated files
            File toDel = new File(Constants.ANTLR_DIR + "/" + myReader.getName());
            Chelsea.cleanDirectory(toDel);
            toDel.delete();
            // Chelsea.clearGenerated();
        }
    }

    public static void runTests(List<Gram> pop) {
        pop.removeIf(Gram::containsInfLoop);
        class testRunner implements Callable<List<Gram>> {
            private List<Gram> myGrams;

            testRunner(List<Gram> toRun) {
                myGrams = toRun;
            }

            @Override
            public List<Gram> call() {
                myGrams.forEach(myGram -> {
                    // System.err.println(myGram.toRemove());
                    myGram.injectEOF();
                    Chelsea.generateSources(myGram);
                    if (myGram.toRemove()) {
                        System.err.println("Code gen failed post generation for \n" + myGram);
                        return;
                    }
                    try {

                        int[] testResult = Chelsea.runTestcases(Constants.POS_MODE, myGram);
                        if (myGram.toRemove()) {
                            return;
                        }

                        Constants.positiveScoring.eval(testResult, myGram);
                        myGram.stripEOF();

                        // numTests++;

                        testResult = Chelsea.runTestcases(Constants.NEG_MODE, myGram);
                        Constants.negativeScoring.eval(testResult, myGram);
                        // System.err.println(myGram.getName() + ": " + myGram.getScore());

                    } catch (Exception e) {
                        System.err.println("Exception in runTests " + e.getCause());

                    } finally {
                        // Clears out the generated files
                        Chelsea.deepCleanDirectory(myGram.getOutputDir());
                    }
                });
                myGrams.removeIf(Gram::toRemove);
                return myGrams;
            }

        }
        ExecutorService myExecutors = Executors.newFixedThreadPool(Constants.NUM_THREADS);
        List<List<Gram>> splitPop = new LinkedList<>();
        for (int i = 0; i < Constants.NUM_THREADS; i++) {
            splitPop.add(new LinkedList<Gram>());
        }
        int counter = 0;
        // System.err.println("Running on " + Constants.NUM_THREADS + " threads");
        // System.err.println("Testing " + pop.size() + " grams");
        while (!pop.isEmpty()) {
            splitPop.get(counter++ % splitPop.size()).add(pop.remove(0));
        }
        List<Integer> allocation = splitPop.stream().map(List<Gram>::size).collect(toList());
        // System.err.println(format("Testing %d grammars using %d threads\nAllocation:
        // %s",
        // allocation.stream().reduce(Integer::sum).get(), splitPop.size(),
        // allocation));
        stopwatch.startClock("testing");
        try {
            List<Future<List<Gram>>> res = myExecutors
                    .invokeAll(splitPop.stream().map(testRunner::new).collect(toList()));
            for (Future<List<Gram>> r : res) {
                pop.addAll(r.get());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // System.err.println("Testing took " + stopwatch.elapsedTime("testing"));
        myExecutors.shutdown();
        // splitPop.forEach(l -> myExecutors.submit(new testRunner(l)));
        // splitPop.forEach(pop::addAll);

    }

    static void writePerfectGrammars(List<Gram> grammars, int genNum) {
        grammars.forEach(gram -> {
            try (FileWriter out = new FileWriter(new File(Constants.LOG_DIR + "/" + outputID + "/"
                    + (Constants.USE_LOCALIZATION ? "local_" : "") + gram.getName()))) {
                out.write(String.format("time taken:%s\niteration count: %d\n%s\n%s",
                        stopwatch.elapsedTime(MAIN_START_TIME), genNum, gram, Constants.getParamString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    static void writeGrammar(Gram gram, int genNum) {
        try (FileWriter out = new FileWriter(new File(Constants.LOG_DIR + "/" + outputID + "/"
                + (Constants.USE_LOCALIZATION ? "local_" : "") + gram.getName()))) {
            out.write(String.format("iteration count: %d\nScore: %f\n%s\n%s", genNum, gram.getScore(), gram,
                    Constants.getParamString()));
            System.err.println(String.format("iteration count: %d\nScore: %f\n%s\nMutation options\n%s", genNum,
                    gram.getScore(), gram, gram.getMutationConsideration().get(0)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static void createLogDir(String path) {
        File logFile = new File(Constants.LOG_DIR + "/" + path);
        logFile.mkdir();
    }

    /**
     * Performs crossover by selecting NUM_CROSSOVER_PER_GEN pairs of grammars from
     * and producing 2 new grammars if useLocal these new grammars are tested using
     * localisation, else uses Chelsea testing scoreArr is used for alternative
     * selection process where you take middle of the road grammars only else uses
     * tournament selection
     * 
     * param scoreArr
     * 
     * @return The children produced
     */
    public static ArrayList<Gram> performCrossover(List<Gram> grammarPool) {
        // Calculate crossoverPop
        ArrayList<Gram> crossoverPop = new ArrayList<Gram>();
        System.err.println("Performing crossover on pop of " + grammarPool.size());
        HashMap<Gram, List<Gram>> pairings = new HashMap<>();

        for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
            Gram base = tournamentSelect(grammarPool, Constants.TOUR_SIZE);

            pairings.put(base, grammarPool.stream().sorted((g0, g1) -> base.compAllPassSimil(g0, g1)).limit(10)
                    .sorted((Comparator.reverseOrder())).limit(3).collect(toList()));
        }

        pairings.keySet().forEach(grammarPool::add);

        for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
            Gram base1 = randGet(new ArrayList<Gram>(pairings.keySet()), true);
            Gram base2 = randGet(pairings.get(base1), true);
            Gram g1 = new Gram(base1);
            Gram g2 = new Gram(base2);
            g1.setNegScore(0.0);
            g1.setPosScore(0.0);
            g2.setNegScore(0.0);
            g2.setPosScore(0.0);

            Gram.loggedCrossover(g1, g2, new StringBuilder());

            g1.setName(Gram.genGramName());
            g2.setName(Gram.genGramName());

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
        Gram g1 = GrammarGenerator.generateDemoCrossoverPop().getFirst();
        Gram g2 = GrammarGenerator.generateDemoCrossoverPop().getFirst();
        goClear();

        StringBuilder out = new StringBuilder();
        out.append(String.format("Generated 2 grammars to demonstate crossover\n%s\n%s\nand\n%s\n%s", g1.getName(),
                g1.prettyPrintRules(g1.getParserRules()), g2.getName(), g2.prettyPrintRules(g2.getParserRules())));

        Gram.loggedCrossover(g1, g2, out);
        out.append(String.format("\n\nResults in \n%s\n%s\nand\n%s\n%s", g1.getName(),
                g1.prettyPrintRules(g1.getParserRules()), g2.getName(), g2.prettyPrintRules(g2.getParserRules())));

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
        bestGrammar.setMutationConsideration(
                newBest.getMutationConsideration().stream().map(i -> i[0] + ":" + i[1]).collect(toList()));
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
                // System.err.println(Arrays.toString(data));
                if (data[5].equals("nan")) {
                    String toPrint = "\nNAN when localizing\n" + currGrammar + "\nPos: " + currGrammar.getPosScore()
                            + "\nNeg: " + currGrammar.getNegScore() + "\nTot: " + currGrammar.getScore() + "\nPassing: "
                            + currGrammar.getPassingPosTests() + "\nPassArr: " + Arrays.toString(currGrammar.passPosArr)
                            + "\nNegArr: " + Arrays.toString(currGrammar.passNegArr) + "\nToolString "
                            + currGrammar.getToolString();

                    blockRead(toPrint);
                    currGrammar.stripEOF();
                    runTests(new ArrayList<>(List.of(currGrammar)));
                    currGrammar.injectEOF();
                    blockRead(format("\n\nRerun scores:\nPos: %f\nNeg:  %f", currGrammar.getPosScore(),
                            currGrammar.getNegScore()));

                }
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
     * Samples a genome without replacement from population
     * 
     * @param pop      population to sample from
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
     * Samples a genome with replacement from population
     * 
     * @param pop      population to sample from
     * @param tourSize size of the tournament
     * @return
     */
    public static Gram tournamentSelectWReplace(List<Gram> pop, int tourSize) {
        // System.err.println("Performing tournament selection on a population of " +
        // pop.size());
        LinkedList<Gram> tour = new LinkedList<Gram>();
        while (tour.size() < tourSize) {
            tour.add(pop.get(randInt(pop.size())));
        }
        Gram out = Collections.max(tour);
        tour.stream().max(Comparator.comparing(Gram::getScore)).get();
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

    // /**
    // * Records all metrics relating to the entire pop
    // *
    // * @param pop
    // */
    // public static void recordPopMetrics(List<Gram> pop) {

    // double minScore =
    // pop.stream().mapToDouble(Gram::getScore).min().getAsDouble();
    // double avgScore =
    // pop.stream().mapToDouble(Gram::getScore).average().getAsDouble();
    // double maxScore =
    // pop.stream().mapToDouble(Gram::getScore).max().getAsDouble();
    // double scoreDelta = scores.getOrDefault(MAX_SCORE_METRIC, new
    // LinkedList<>()).size() == 0 ? 0
    // : maxScore - scores.get(MAX_SCORE_METRIC).getLast();
    // double posTestPass =
    // pop.stream().mapToLong(Gram::numPassPos).max().getAsLong();
    // double negTestPass =
    // pop.stream().mapToLong(Gram::numPassNeg).max().getAsLong();
    // double allTestPass = negTestPass + posTestPass;
    // double timeTaken = stopwatch.elapsedTime("GEN_START");
    // double numGrams = generatedGrammars.size();

    // recordMetric(MIN_SCORE_METRIC, minScore);
    // recordMetric(AVG_SCORE_METRIC, avgScore);
    // recordMetric(MAX_SCORE_METRIC, maxScore);
    // recordMetric(SCORE_DELTA_METRIC, scoreDelta);
    // recordMetric(NUM_PASS_POS_METRIC, posTestPass);
    // recordMetric(NUM_PASS_NEG_METRIC, negTestPass);
    // recordMetric(NUM_PASS_TOT_METRIC, allTestPass);
    // recordMetric(TIME_PER_GEN_METRIC, timeTaken);
    // recordMetric(TOTAL_GRAMMARS_METRIC, numGrams);
    // recordMetric(HASH_TABLE_HITS_METRIC, Double.valueOf(hashtableHits));
    // }

    public static void recordMetric(String key, List<Gram> pop) {
        scores.putIfAbsent(key, new LinkedList<Double>());
        scores.get(key).add(logFuncs.get(key).getVal(pop.stream()));
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

    public static void logMetricsJSON(metricLog currMetrics) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String parsedString = gson.toJson(currMetrics);
        try (FileWriter out = new FileWriter(new File(Constants.LOG_DIR + "/" + outputID + "/metrics.json"))) {
            out.write(parsedString);
        } catch (Exception e) {

        }
    }

    public static void logMetric(String key) {
        if (!scores.containsKey(key))
            return;
        int genNum = scores.get(key).size();

        StringBuilder toWrite = new StringBuilder();
        if (currLogFile.isEmpty())
            currLogFile = Optional.of(new File(outputID + "_" + (Constants.USE_LOCALIZATION ? "local_" : "") + genNum
                    + "_" + LocalDateTime.now() + key + ".json"));

        try (FileWriter out = new FileWriter(new File(Constants.LOG_DIR + "/" + outputID + "/" + key))) {

            // toWrite.append(format("\"%s\": {\n", key));

            toWrite.append(key + ", " + Arrays.toString(scores.get(key).stream().toArray(Double[]::new)));

            out.write(toWrite.toString());

        } catch (Exception e) {

        }
    }

    public static void logMetric(List<String> keys) {
        keys.forEach(App::logMetric);
    }

    public static void clearANTLRfolder() {
        Chelsea.cleanDirectory(new File(Constants.ANTLR_DIR));
    }

    // public static Double[] extractMetric(int index) {
    // System.err.println("Scores");
    // System.err.println(scores.stream().map(Arrays::toString).collect(Collectors.joining("\n")));
    // return scores.stream()
    // .map(entry -> entry[index])
    // .toArray(Double[]::new);
    // }

}
