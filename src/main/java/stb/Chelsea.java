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
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
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
    static Constructor<?> lexerConstructor;
    static Constructor<?> parserConstructor;
    static String currPosDir = null;
    static List<String> posTests = new LinkedList<>();

    static String currNegDir = null;
    static List<String> negTests = new LinkedList<>();
    static int numLogs = 0;
    
    static List<String> getAllTests() {
        return Stream.of(posTests, negTests).flatMap(List::stream).collect(Collectors.toList());
        
    }
    static Timer stopwatch = new Timer();
    static ArrayList<retainedGrammar> slowGrammars = new ArrayList<>();
    private static int numGramsThisGen = 0;
    private static double  totalRunTime = 0;
    
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
    

    static Predicate<retainedGrammar> slowGramPred = g -> g.time > 2*(totalRunTime/numGramsThisGen);
    public static void logSlowGrammars(int genNum) {
        // System.err.println(format("SlowGrams: %s", slowGrammars));
        //if the slowest grammar took more that 10 seconds to generate we should flag this particular generation to not get cleared by the clearing algorithm
        String flagString = slowGrammars.get(slowGrammars.size()-1).getTime() > 10 ? "GEN" : "gen";
        

        String fileName = Constants.SLOW_LOG_DIR + format("/%s_%d_%s.log", flagString , genNum, LocalDateTime.now());
        System.err.println(format("Filtering with avg %f %s", totalRunTime/numGramsThisGen, slowGrammars));
        List<retainedGrammar> toWrite = slowGrammars.stream().filter(slowGramPred).collect(Collectors.toList());

        if(toWrite.size() == 0) return;
        try (FileWriter out = new FileWriter(new File(fileName))) {
            StringBuilder outBuilder = new StringBuilder(format("Average time: %f\n", totalRunTime/numGramsThisGen));
            toWrite.stream().forEachOrdered(gram -> 
                outBuilder.append(format("%d:Time taken: %f\n%s\n\n",slowGrammars.indexOf(gram), gram.time, gram.gram))
            );

            out.write(outBuilder.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    // This loads all the tests into memory once and reuses them from there

    public static boolean loadTests(String posDir, String negDir) {

        // Pos tests
        try (Stream<Path> paths = Files.walk(Paths.get(posDir)).filter(Files::isRegularFile)) {
            currPosDir = posDir;
            paths.forEach(path -> {
                try {
                    StringBuilder rawContent = new StringBuilder(Files.readString(path));
                    while (rawContent.indexOf("/*") != -1) {
                        rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                    }

                    String content = rawContent.toString().trim().replaceAll(" ", "");
                    posTests.add(content);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Neg tests
        try (Stream<Path> paths = Files.walk(Paths.get(negDir)).filter(Files::isRegularFile)) {

            currNegDir = negDir;

            paths.forEach(path -> {
                try {
                    StringBuilder rawContent = new StringBuilder(Files.readString(path));
                    while (rawContent.indexOf("/*") != -1) {
                        rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                    }

                    String content = rawContent.toString().trim().replaceAll(" ", "");
                    negTests.add(content);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
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
            // Setting up the arguments for th e ANTLR Tool. outputDir is in this case
            // generated-sources/

            // args arrays for writing for normal testing and for writing to localiser
            String[] args = { "-o", Constants.ANTLR_DIR  + "/" + finName, "-DcontextSuperClass=RuleContextWithAltNum" };

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


            //Loads all the class files into the hashmap
            // hm = new DynamicClassLoader().load(new File(Constants.ANTLR_DIR + "/" + grammar.getName()));
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
        addToSlowGrammars(new retainedGrammar(stopwatch.elapsedTime(), grammar.toString()));

    }

    /**
     * Notes on localiser process
     * only spectra and testrunner reference UUT so they need to be compiled after the UUT antlr files have been produced
     * @param grammar
     */
    public static void Localise(Gram grammar) {
        Map<String, Class<?>> hm = null;
        try {
            // System.err.println("LOCALISING" + grammar);
            
            
            String toWrite = grammar.toString().replaceFirst(grammar.getName(), "UUT");
            Pipeline.pipeline(toWrite);
            String[] args = { "-o", Constants.LOCALISER_JAVA_DIR, "-DcontextSuperClass=RuleContextWithAltNum"};


            // Creating a new Tool object with org.antlr.v4.Tool

            Tool tool = new Tool(args);
            GrammarRootAST grast = tool.parseGrammarFromString(toWrite);

            // Create a new Grammar object from the tool
            Grammar g = tool.createGrammar(grast);

            g.fileName = "UUT";

            tool.process(g, true);

            // Compile source files
            DynamicClassCompiler dynamicClassCompiler = new DynamicClassCompiler(List.of("-d",Constants.LOCALISER_CLASS_DIR));


            dynamicClassCompiler.compile(new File(Constants.LOCALISER_JAVA_DIR));

            hm = new DynamicClassLoader().load(new File(Constants.LOCALISER_CLASS_DIR));


            //Load and call the testRunner class that was just compiled
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
            Object[] argsToPass = {allTests, Logger.noOfRules, Logger.ruleIndices};
            // System.err.println("Calling testrunner for \n" + grammar.hashString());
            parseMethod.invoke(null, argsToPass);
            
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            //Clean out the class files that were just used
            cleanDirectory(new File(Constants.LOCALISER_CLASS_DIR));
        }
    }

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
    public static int[] runTestcases( String mode, Gram myReader) throws IOException, IllegalAccessException,
            InvocationTargetException, InstantiationException, NoSuchMethodException {

        // output array {numPasses, numTests}
        int[] out = { 0, 0 };

        List<String> tests;
        if (mode.equals(Constants.POS_MODE)) {
            tests = posTests;
        } else if (mode.equals(Constants.NEG_MODE)) {
            tests = negTests;
        } else {
            return null;
        }
        Stack<String> passingTests = new Stack<String>();
        Stack<String> failingTests = new Stack<String>();
        // System.out.println("Running " + paths.count() + " tests");
        // App.rgoSetText("Testing " + myReader + "\n");
        // System.err.println("Testing " + myReader.getName());
        int[] testNum = { 0 };
        Boolean[] passArr = new Boolean[tests.size()];
        Arrays.fill(passArr, false);
        tests.forEach(test -> {

            MyListener myListen = new MyListener();
            out[1]++;
            try {
                testNum[0]++;
                Map<String, Class<?>> hm = new DynamicClassLoader().load(new File(Constants.ANTLR_DIR + "/" + myReader.getName()));

                // Manually creates lexer.java file in outputDir
                Class<?> lexerC = hm.get(myReader.getName() + "Lexer");
                // Manually creates the lexerConstructor for use later
                // Is initialized as Constructor<?> lexerConstructor
                lexerConstructor = lexerC.getConstructor(CharStream.class);
                String.class.getConstructor(String.class);

                Class<?> parserC = hm.get(myReader.getName() + "Parser");

                // Manually creates the parserConstructor for use later
                // Is initialized as Constructor<?> parserConstructor
                parserConstructor = parserC.getConstructor(TokenStream.class);
                Lexer lexer = (Lexer) lexerConstructor.newInstance(CharStreams.fromString(test));

                CommonTokenStream tokens = new CommonTokenStream(lexer);

                // Creating a new parser constructor instance using the lexer tokens
                Parser parser = (Parser) parserConstructor.newInstance(tokens);

                // parser.addErrorListener(myListen);
                parser.removeErrorListeners();
                myListen.setGrammarName(parser.getGrammarFileName());
                parser.setErrorHandler(new BailErrorStrategy());
                
                Method parseEntrypoint = parser.getClass().getMethod(Constants.GRAM_START_SYMB);
                

                //equiv of UUTParser.program()
                parseEntrypoint.invoke(parser);
                
                passingTests.push(test);
                out[0]++;
                passArr[testNum[0] - 1] = true;
                // If this code is reached the test case was successfully parsed and numPasses
                // should be incremented

            } catch (NoSuchMethodException e) {
                // System.err.println("No such method exception " + e.getCause());
                System.err.println("Removing " + myReader.getName() + " from grammarList");
                myReader.flagForRemoval();
            } catch (Exception e) {
                failingTests.push(test);
            } finally {
                if (mode.equals(Constants.POS_MODE)) {
                    myReader.setPosPass(passArr);
                } else {
                    myReader.setNegPass(passArr);
                }
            }
        });

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
        // System.err.println("Filtering with " + name + "\non\n" + toDelete.stream().map(File::getName).collect(Collectors.joining("\n")));
        // Pattern r = Pattern.compile(name + "[^0-9]*$");
        // StringBuilder out  = new StringBuilder("Using key " +  name + "\n");
        // toDelete.removeIf(f -> {
        //     if(r.matcher(f.getName()).find()) {
        //         // out.append(f.getName() + "\n");
        //     }
        //     return !r.matcher(f.getName()).find();});
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
        if(slowGrammars.size() == 5 && toAdd.time < slowGrammars.get(0).time) return;
        // System.err.println(format("Adding %s to %s\n", toAdd, slowGrammars));
        slowGrammars.add(toAdd);
        Collections.sort(slowGrammars);
        numGramsThisGen++;
        totalRunTime += toAdd.time;
        if(slowGrammars.size() > 5) slowGrammars.remove(0);
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
