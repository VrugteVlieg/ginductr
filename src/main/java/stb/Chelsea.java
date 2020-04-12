package stb;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Stream;


import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.Tool;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.tool.Grammar;
import org.antlr.v4.tool.ast.GrammarRootAST;

public class Chelsea {
    // Collection of Methods gotten from Chelsea Barraball 19768125@sun.ac.za
    // Generates, compiles and loads parser,lexer classes from the file
    static Constructor<?> lexerConstructor;
    static Constructor<?> parserConstructor;
    static GrammarReader myReader;

    /**
     * Generates the source files to be used by the following call to runTests
     * @param grammar
     */
    public static void generateSources(GrammarReader grammar, lamdaArg removeCurr) {
        myReader = grammar;
        String finName = "default";
        Map<String, Class<?>> hm = null;
        try {

            // Name of the file, for example ampl.g4
            finName = grammar.getName();
            // Setting up the arguments for th e ANTLR Tool. outputDir is in this case
            // generated-sources/
            String[] args = { "-o", Constants.OUTPUT_DIR };

            // Creating a new Tool object with org.antlr.v4.Tool

            Tool tool = new Tool(args);
            GrammarRootAST grast = tool.parseGrammarFromString(grammar.toString());

            // Create a new Grammar object from the tool
            Grammar g = tool.createGrammar(grast);

            g.fileName = grammar.getName();

            tool.process(g, true);

            // Compile source files
            DynamicClassCompiler dynamicClassCompiler = new DynamicClassCompiler();

            dynamicClassCompiler.compile(new File(Constants.OUTPUT_DIR));

            hm = new DynamicClassLoader().load(new File(Constants.OUTPUT_DIR));

            // Manually creates lexer.java file in outputDir
            Class<?> lexer = hm.get(finName + "Lexer");
            // Manually creates the lexerConstructor for use later
            // Is initialized as Constructor<?> lexerConstructor
            lexerConstructor = lexer.getConstructor(CharStream.class);
            String.class.getConstructor(String.class);

            // Manually creates parser.java file in outputDir
            //TODO implement left recursion removal from https://en.wikipedia.org/wiki/Left_recursion#Removing_all_left_recursion
            // The following sets of rules are mutually left-recursive [factor, term]
            // program : factor EOF ;
            // factor : term term Digit ;
            // term : factor? ;
            // Digit : '0' ;
            // Mulop : '*' | '/' ;
            // Addop : '+' | '-' ;
            // LPAR : '(' ;
            // RPAR : ')' ;
            Class<?> parser = hm.get(finName + "Parser");

            // Manually creates the parserConstructor for use later
            // Is initialized as Constructor<?> parserConstructor
            parserConstructor = parser.getConstructor(TokenStream.class);

        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage() + " for " + finName);
            System.err.println(grammar);
            System.err.println(hm.keySet());
            removeCurr.removeGrammar();
            
        }
    }
    /**
     * Runs the parser on all the files inside the TEST_DIR and returns a hashmap which maps filenames to a linkedList of errors found while parsing that file
     * @return Hashmap that maps filenames to a linkedlist of errors encountered during parsing
     * @throws IOException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    public static HashMap<String, LinkedList<Stack<String>>> runTestcases(lamdaArg removeCurr) throws IOException, IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        // Gettting a list of test cases from the database
        HashMap<String,LinkedList<Stack<String>>> errors = new HashMap<String,LinkedList<Stack<String>>>();
        try (Stream<Path> paths = Files.walk(Paths.get(Constants.TEST_DIR)).filter(Files::isRegularFile)) {
            // System.out.println("Running " + paths.count() + " tests");
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                // System.out.println("Testing " + fileName);
                try {
                    StringBuilder rawContent = new StringBuilder(Files.readString(path));
                    while (rawContent.indexOf("/*") != -1) {
                        rawContent.delete(rawContent.indexOf("/*"), rawContent.indexOf("*/") + 2);
                    }
                    /*
                    FIXME  when implementing regex recognition, remove the space stripping here and add grammar rules to ignore spaces
                    */
                    String content = rawContent.toString().trim().replaceAll(" ","");
                    // System.out.println("content of " + path.getFileName() + "\n" + content);
                    // Creating a new lexer constructor instance using the contents of the test case
                    Lexer lexer = (Lexer) lexerConstructor.newInstance(CharStreams.fromString(content));

                    CommonTokenStream tokens = new CommonTokenStream(lexer);

                    // Creating a new parser constructor instance using the lexer tokens
                    Parser parser = (Parser) parserConstructor.newInstance(tokens);
                    MyListener testListen = new MyListener(parser.getGrammarFileName());
                    parser.addErrorListener(testListen);
                    // parser.setErrorHandler(new myErrorStrategy());
                    
                    // Begin parsing at the first rule of the grammar. In this case it was *program*
                    // but you might need to
                    // figure out how to tell your parser what the entry point is.
                    // System.err.println("Testing " + myReader.getName() + " on " + fileName + " entrypoint " + myReader.getStartSymbol());
                    Method parseEntrypoint = parser.getClass().getMethod(myReader.getStartSymbol());
                    
                    // Finally, this will run the parser with the provided test case. Yay! Ignore
                    // the rest of this function.
                    parseEntrypoint.invoke(parser);
                    
                    LinkedList<Stack<String>> fileErrors = testListen.getErrors();
                    // if(fileErrors.isEmpty()) {
                    //     System.out.println(fileErrors);
                    //     System.err.println(myReader.getName() + " passes for " + content);
                    //     System.out.println(myReader.getName() + " passes for " + content);
                    // } else {
                    //     System.err.println(myReader.getName() + " fails for " + content);
                    //     System.out.println(myReader.getName() + " fails for " + content);
                    // }
                    removeDuplicates(fileErrors);
                    // fileErrors.forEach(ErrorStack -> System.err.println(ErrorStack));
                                        
                    errors.put(fileName.toString(), fileErrors);

                }   catch(NoSuchMethodException e) {
                        // System.err.println("No  such method exception " + e.getCause());
                        System.out.println("Removing " + myReader.getName() + " from grammar");
                        removeCurr.removeGrammar();
                }   catch (ParseCancellationException e) {
                        System.out.println("Parse cancelled for " + myReader.getName());
                }   catch (Exception e) {
                        // e.printStackTrace();
                        System.err.println("Exception lamda in Chelsea " + e.getCause());
                }
            });
        } catch (Exception e) {
            System.err.println("Exception filewalker in Chelsea " + e.getMessage());
            e.printStackTrace();
        }
        cleanDirectory(new File(Constants.OUTPUT_DIR));
        return errors;

    }

    public static <T> void removeDuplicates(List<T> input) {
        List<T> out = new LinkedList<>();
        input.forEach(element -> {
            if(!out.contains(element)) {
                out.add(element);
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
    private static List<File> getDirectoryFiles(File directory) {
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
     * Removes all the generated files to keep future hashmaps manageable
     * @param directory
     */
    public static void cleanDirectory(File directory) {
        assert directory.isDirectory();
        List<File> files = getDirectoryFiles(directory);
        for (int i = 0; i < files.size(); i++) {
            String fileName = files.get(i).getName();
            files.get(i).delete();
        }
    }
    
}
