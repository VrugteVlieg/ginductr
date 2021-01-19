package stb;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ListTokenSource;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.Tool;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarRootAST;

import stb.localiser.depend.Pipeline;
import stb.localiser.depend.Logger;

import static java.lang.String.format;

public class Chelsea {
    // Collection of Methods gotten from Chelsea Barraball 19768125@sun.ac.za
    // Generates, compiles and loads parser,lexer classes from the file
    // static Constructor<?> lexerConstructor;
    // static Constructor<?> parserConstructor;

    // Sets of learning test cases
    static List<String> posTests = new LinkedList<>();
    static List<String> negTests = new LinkedList<>();

    // Sets of validation test cases
    static List<String> posValTests = new LinkedList<>();
    static List<String> negValTests = new LinkedList<>();

    static List<String> getAllTests() {
        return Stream.of(posTests, negTests).flatMap(List::stream).collect(Collectors.toList());

    }

    static Timer stopwatch = new Timer();
    static ArrayList<retainedGrammar> slowGrammars = new ArrayList<>();
    private static int numGramsThisGen = 0;
    private static double totalRunTime = 0;

    private static class retainedGrammar implements Comparable<retainedGrammar> {
        double time;
        String gram;

        public retainedGrammar(double time, String gram) {
            this.time = time;
            this.gram = gram;
        }

        @Override
        public int compareTo(retainedGrammar arg0) {
            return Double.compare(this.time, arg0.time);
        }

        public double getTime() {
            return time;
        }

        @Override
        public String toString() {
            return time + ", " + gram.split("\n")[0];
        }
    }

    public static void clearSlowGrammars() {
        System.err.println("Clearing " + slowGrammars.toString());
        slowGrammars.clear();
        totalRunTime = 0;
        numGramsThisGen = 0;
    }

    static Predicate<retainedGrammar> slowGramPred = g -> g.time > 2 * (totalRunTime / numGramsThisGen);

    public static void logSlowGrammars(int genNum) {
        // System.err.println(format("SlowGrams: %s", slowGrammars));
        // if the slowest grammar took more that 10 seconds to generate we should flag
        // this particular generation to not get cleared by the clearing algorithm
        String flagString = slowGrammars.get(slowGrammars.size() - 1).getTime() > 10 ? "GEN" : "gen";

        String fileName = Constants.SLOW_LOG_DIR + format("/%s_%d_%s.log", flagString, genNum, LocalDateTime.now());
        System.err.println(format("Filtering with avg %f %s", totalRunTime / numGramsThisGen, slowGrammars));
        List<retainedGrammar> toWrite = slowGrammars.stream().filter(slowGramPred).collect(Collectors.toList());

        if (toWrite.size() == 0)
            return;
        try (FileWriter out = new FileWriter(new File(fileName))) {
            StringBuilder outBuilder = new StringBuilder(format("Average time: %f\n", totalRunTime / numGramsThisGen));
            toWrite.stream().forEachOrdered(gram -> outBuilder
                    .append(format("%d:Time taken: %f\n%s\n\n", slowGrammars.indexOf(gram), gram.time, gram.gram)));

            out.write(outBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    // This loads all the tests into memory once and reuses them from there

    public static boolean loadTests(String posDir, String negDir, String validPosDir, String validNegDir) {
        String[] pathsToTests = { posDir, negDir, validPosDir, validNegDir };
        List<List<String>> testLists = List.of(posTests, negTests, posValTests, negValTests);

        for (int i = 0; i < pathsToTests.length; i++) {
            try {
                for (Path path : Files.walk(Paths.get(pathsToTests[i])).filter(Files::isRegularFile)
                        .collect(Collectors.toList())) {

                    StringBuilder rawContent = new StringBuilder(Files.readString(path));

                    while (rawContent.indexOf("/*") != -1) {
                        rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                    }

                    String content = rawContent.toString().trim().replaceAll(" ", "");
                    testLists.get(i).add(content);

                }

            } catch (Exception e) {
                // e.printStackTrace();
                System.err.println("Could not load tests in " + pathsToTests[i]);
                // return false;
            }
        }
        return true;
    }

    /**
     * Generates the source files to be used by the following call to runTests
     * 
     * @param grammar
     */
    public static void generateSources(Gram grammar) {
        String finName = "default";
        Map<String, Class<?>> hm = null;
        stopwatch.startClock();
        try {
            // System.err.println("Generating sources for " + grammar);

            // Name of the file, for example ampl.g4
            finName = grammar.getName();
            // Setting up the arguments for the ANTLR Tool. outputDir is in this case
            // generated-sources/

            // args arrays for writing for normal testing and for writing to localiser
            String[] args = { "-o", Constants.ANTLR_DIR + "/" + finName, "-DcontextSuperClass=RuleContextWithAltNum" };

            // Creating a new Tool object with org.antlr.v4.Tool

            Tool tool = new Tool(args);
            GrammarRootAST grast = tool.parseGrammarFromString(grammar.toString());

            // Create a new Grammar object from the tool
            Grammar g = tool.createGrammar(grast);

            g.fileName = grammar.getName();

            tool.process(g, true);
            // System.err.println("Tool done");
            if (tool.getNumErrors() != 0) {
                App.numBrokenGrammars++;
                grammar.flagForRemoval();
                return;
            }

            // Compile source files
            DynamicClassCompiler dynamicClassCompiler = new DynamicClassCompiler();
            File myOut = new File(Constants.ANTLR_DIR + "/" + grammar.getName());
            dynamicClassCompiler.compile(myOut);

            // Loads all the class files into the hashmap
            // hm = new DynamicClassLoader().load(new File(Constants.ANTLR_DIR + "/" +
            // grammar.getName()));
            // // Manually creates lexer.java file in outputDir
            // Class<?> lexer = hm.get(finName + "Lexer");
            // // Manually creates the lexerConstructor for use later
            // // Is initialized as Constructor<?> lexerConstructor
            // lexerConstructor = lexer.getConstructor(CharStream.class);
            // String.class.getConstructor(String.class);

            // Class<?> parser = hm.get(finName + "Parser");

            // // Manually creates the parserConstructor for use later
            // // Is initialized as Constructor<?> parserConstructor
            // parserConstructor = parser.getConstructor(TokenStream.class);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            grammar.mutHist.add(e.toString());
            grammar.logGrammar(true);

            // System.err.println(myReader.mutHist.stream().collect(Collectors.joining("\n")));
            if (hm == null) {
                System.out.println("Looking for " + grammar + "\n found");
                List<File> files = getDirectoryFiles(new File(Constants.ANTLR_DIR));
                System.out.println(files.stream().map(File::getName).collect(Collectors.joining("\n")));

            } else {
                System.err.println(grammar.hashString());
                // System.out.println(hm.keySet());
            }

            grammar.flagForRemoval();
        }
        // addToSlowGrammars(new retainedGrammar(stopwatch.elapsedTime(),
        // grammar.toString()));

    }

    /**
     * Notes on localiser process only spectra and testrunner reference UUT so they
     * need to be compiled after the UUT antlr files have been produced
     * 
     * @param grammar
     */
    public static void Localise(Gram grammar) {
        Map<String, Class<?>> hm = null;
        try {
            // System.err.println("LOCALISING" + grammar);
            String toWrite = grammar.toString().replaceFirst(grammar.getName(), "UUT");
            Pipeline.pipeline(toWrite);
            String[] args = { "-o", Constants.LOCALISER_JAVA_DIR, "-DcontextSuperClass=RuleContextWithAltNum" };

            // Creating a new Tool object with org.antlr.v4.Tool

            Tool tool = new Tool(args);
            GrammarRootAST grast = tool.parseGrammarFromString(toWrite);
            // System.err.println("Tool created using " + Arrays.toString(args));
            // Create a new Grammar object from the tool
            Grammar g = tool.createGrammar(grast);

            g.fileName = "UUT";

            tool.process(g, true);

            // Compile source files
            DynamicClassCompiler dynamicClassCompiler = new DynamicClassCompiler(
                    List.of("-d", Constants.LOCALISER_CLASS_DIR));

            dynamicClassCompiler.compile(new File(Constants.LOCALISER_JAVA_DIR));

            hm = new DynamicClassLoader().load(new File(Constants.LOCALISER_CLASS_DIR));
            // System.err.println(hm.keySet().stream().collect(Collectors.joining(",\n",
            // "[\n", "\n]")));
            // Load and call the testRunner class that was just compiled
            Class<?> TR = hm.get("TestRunner");
            Class<?>[] cArg = new Class[3];
            cArg[0] = List.class;
            cArg[1] = Integer.class;
            cArg[2] = Map.class;

            List<String> allTests = new LinkedList<String>();
            posTests.forEach(test -> {
                allTests.add("pos" + test);
            });

            negTests.forEach(test -> {
                allTests.add("neg" + test);
            });

            Method parseMethod = TR.getMethod("parse", cArg);
            Object[] argsToPass = { allTests, Logger.noOfRules, Logger.ruleIndices };
            // System.err.println("Calling testrunner for \n" + grammar.hashString());
            parseMethod.invoke(null, argsToPass);

        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            // Clean out the class files that were just used
            cleanDirectory(new File(Constants.LOCALISER_CLASS_DIR));
        }
    }

    static boolean failedFirst = false;

    /**
     * Runs the parser on all the files inside the TEST_DIR and returns a hashmap
     * which maps filenames to a linkedList of errors found while parsing that file
     * 
     * @return Hashmap that maps filenames to a linkedlist of errors encountered
     *         during parsing
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    public static int[] runTestcases(String mode, Gram myGram) throws IOException, IllegalAccessException,
            InvocationTargetException, InstantiationException, NoSuchMethodException {

        // output array {numPasses, numTests}

        List<String> tests;
        switch (mode) {
            case Constants.POS_MODE:
                tests = posTests;
                break;

            case Constants.NEG_MODE:
                tests = negTests;
                break;

            case Constants.POS_VAL_MODE:
                tests = posValTests;
                break;

            case Constants.NEG_VAL_MODE:
                tests = negValTests;
                break;
            default:
                return null;
        }

        int[] out = { 0, tests.size() };
        Stack<String> passingTests = new Stack<String>();
        Stack<String> failingTests = new Stack<String>();
        // System.out.println("Running " + paths.count() + " tests");
        // App.rgoSetText("Testing " + myReader + "\n");
        // System.err.println("Testing " + myReader.getName());
        int testNum = 0;
        Boolean[] passArr = new Boolean[tests.size()];
        double[] partialScores = new double[tests.size()];
        Arrays.fill(passArr, false);
        try {
            Map<String, Class<?>> hm = new DynamicClassLoader()
                    .load(new File(Constants.ANTLR_DIR + "/" + myGram.getName()));

            // Manually creates lexer.java file in outputDir
            Class<?> lexerC = hm.get(myGram.getName() + "Lexer");
            // Manually creates the lexerConstructor for use later
            // Is initialized as Constructor<?> lexerConstructor
            Constructor<?> lexerConstructor = lexerC.getConstructor(CharStream.class);
            String.class.getConstructor(String.class);

            Class<?> parserC = hm.get(myGram.getName() + "Parser");

            // Manually creates the parserConstructor for use later
            // Is initialized as Constructor<?> parserConstructor
            Constructor<?> parserConstructor = parserC.getConstructor(TokenStream.class);
            for (String test : tests) {
                testNum++;
                Lexer lexer = (Lexer) lexerConstructor.newInstance(CharStreams.fromString(test));
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                Parser parser = (Parser) parserConstructor.newInstance(tokens);
                parser.removeErrorListeners();
                parser.setErrorHandler(new BailErrorStrategy());
                Method parseEntrypoint = parser.getClass().getMethod(Constants.GRAM_START_SYMB);
                try {
                    parseEntrypoint.invoke(parser);

                    // Creating a new parser constructor instance using the lexer tokens

                    // equiv of UUTParser.program()

                    passingTests.push(test);
                    out[0]++;
                    passArr[testNum - 1] = true;
                    if (mode.equals(Constants.POS_MODE)) {
                        partialScores[testNum - 1] = 1.0;
                    }
                    // If this code is reached the test case was successfully parsed and numPasses
                    // should be incremented
                } catch (Exception e) {

                    if (mode.equals(Constants.POS_MODE) && Constants.USE_PARTIAL_SCORING) {
                        int maxNumTokens = 0;
                        String bestRuleName = "";

                        lexer.reset();
                        List<? extends Token> allToks = lexer.getAllTokens();
                        // System.err.println("Failed " + test);
                        // List<? extends Token> newTokenSource = allToks.subList(i, allToks.size());
                        tokens.setTokenSource(new ListTokenSource(allToks, "myTokenSource"));
                        tokens.fill();
                        parser = (Parser) parserConstructor.newInstance(tokens);
                        parser.setErrorHandler(new BailErrorStrategy());
                        parser.removeErrorListeners();
                        for (int i = 0; i < allToks.size(); i++) {
                            for (Rule r : myGram.getParserRules().subList(1, myGram.getParserRules().size())) {
                                tokens.seek(i);
                                try {
                                    // System.err.println("Parsing using " + r + " from index " + i);
                                    int startIndex = tokens.index();
                                    parser.getClass().getMethod(r.name).invoke(parser);
                                    int numTokensParsed = tokens.index() - startIndex;
                                    if (maxNumTokens <= numTokensParsed) {
                                        // System.err.println("maxTokensParsed: " + maxNumTokens + " -> " +
                                        // numTokensParsed);
                                        // System.err.println("Using rule " + r.name + " from index " + i + " to " +
                                        // (i+numTokensParsed));
                                        // System.err.println("Partial score: " + (1.0*maxNumTokens/allToks.size()) + "
                                        // -> " + (1.0*numTokensParsed/allToks.size()));
                                        bestRuleName = r.name;
                                        maxNumTokens = numTokensParsed;
                                    }
                                    maxNumTokens = maxNumTokens < numTokensParsed ? numTokensParsed : maxNumTokens;
                                    // System.err.println("Successfully parsed " + numTokensParsed + " tokens");
                                } catch (Exception f) {
                                    // System.err.println("Parsing from " + i + " failed.");
                                }
                            }
                        }
                        // LinkedList<Token> remainingConfigs = new LinkedList<>(tokens.get(1,
                        // tokens.size()))
                        double maxPartialScore = (1.0 * maxNumTokens / allToks.size());
                        myGram.setBestMatchingRule(bestRuleName);
                        // System.err.println("Best partial score " + maxPartialScore);
                        partialScores[testNum - 1] = maxPartialScore;
                        // System.err.println("The first token in the stream is " +
                        // tokens.getText(tokens.get(0),tokens.get(0)));
                    }

                    // List<String> tokens = Arrays.stream(test.split(" ")).collect();
                    failingTests.push(test);
                    // findLongest(test);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mode.equals(Constants.POS_MODE)) {
            myGram.setPosPass(passArr);
            myGram.setPassingPosTests(passingTests);
            myGram.setFailingPosTests(failingTests);
            myGram.setPartialScoreArr(partialScores);
        } else {
            myGram.setPassingNegTests(passingTests);
            myGram.setFailingNegTests(failingTests);
            myGram.setNegPass(passArr);
        }
        return out;

    }

    public static <T> void removeDuplicates(List<T> input) {
        List<T> out = new LinkedList<>();
        input.forEach(element -> {
            if (!out.contains(element)) {
                out.add(element);
            } else {
                // System.err.println("Filtering " + element + " from " + input.toString());
            }
        });
        input.clear();
        input.addAll(out);
    }

    /**
     * Recurses output instrumented directory and returns list of class files.
     *
     * @param directory Input directory.
     * @return ArrayList of class files.
     */
    public static List<File> getDirectoryFiles(File directory) {
        List<File> fileList = new ArrayList<>();
        if (directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                fileList.addAll(getDirectoryFiles(file));
            }
        } else {
            return Collections.singletonList(directory);
        }
        return fileList;
    }

    /**
     * Removes all files in target directory, used to keep code gen hashmaps clean
     * 
     * @param directory
     */
    public static void cleanDirectory(File directory) {
        assert directory.isDirectory();

        List<File> files = getDirectoryFiles(directory);
        // System.err.println("Deleting " + files.size() + " files:\n" + files);
        for (int i = 0; i < files.size(); i++) {
            files.get(i).delete();
        }
    }

    /**
     * Removes all files in target directory, as well as the directory itself
     * 
     * @param directory
     */
    public static void deepCleanDirectory(File directory) {
        assert directory.isDirectory();
        List<File> toDelete = getDirectoryFiles(directory);
        // System.err.println("Filtering with " + name + "\non\n" +
        // toDelete.stream().map(File::getName).collect(Collectors.joining("\n")));
        // Pattern r = Pattern.compile(name + "[^0-9]*$");
        // StringBuilder out = new StringBuilder("Using key " + name + "\n");
        // toDelete.removeIf(f -> {
        // if(r.matcher(f.getName()).find()) {
        // // out.append(f.getName() + "\n");
        // }
        // return !r.matcher(f.getName()).find();});
        // System.err.println("Removing \n" + out.toString());
        toDelete.forEach(File::delete);
        directory.delete();
    }

    /**
     * Removes the generated files used for current test
     */
    public static void clearGenerated() {
        cleanDirectory(new File(Constants.ANTLR_DIR));
    }

    static void addToSlowGrammars(retainedGrammar toAdd) {
        if (slowGrammars.size() == 5 && toAdd.time < slowGrammars.get(0).time)
            return;
        // System.err.println(format("Adding %s to %s\n", toAdd, slowGrammars));
        slowGrammars.add(toAdd);
        Collections.sort(slowGrammars);
        numGramsThisGen++;
        totalRunTime += toAdd.time;
        if (slowGrammars.size() > 5)
            slowGrammars.remove(0);
    }

    static void logSlowGrammar(retainedGrammar gram, int genNum) {
        String fileName = Constants.SLOW_LOG_DIR + format("/gen_%d_gram_%d.log", genNum, slowGrammars.indexOf(gram));
        try (FileWriter out = new FileWriter(new File(fileName))) {
            out.write(format("Time taken: %f\n%s", gram.time, gram.gram));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
