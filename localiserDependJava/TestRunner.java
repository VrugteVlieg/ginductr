
// package stb.localiser.dynamic;
import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.*;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class TestRunner {

    private static ArrayList<File> Recursive(File[] arr, ArrayList<File> files) {
        for (File f : arr) {
            if (f.isFile())
                files.add(f);
            else if (f.isDirectory() && f.list().length > 0)
                Recursive(f.listFiles(), files);
        }
        return files;
    }

    public static void parse(List<String> tests, Integer noRules, Map<String, Integer> ruleIndices) throws Exception {
        new Logger(noRules.intValue(), ruleIndices);
        File directory = null;
        FileInputStream fis = null;
        CharStream input = null;
        UUTLexer lexer = null;
        CommonTokenStream tokens = null;
        UUTParser parser = null;

        // Iterates over posTests, removes default eListenrs and attaches
        // Spectra.getErrorListener
        // Then records num passes and fails and adds to logger
        for (String test : tests) {
            // System.err.println("Running tests on " + tests.indexOf(test) + "/" + tests.size());
            String content = test.substring(3);
            boolean pos = test.substring(0, 3).equalsIgnoreCase("pos");
            input = CharStreams.fromString(content);
            // input = CharStreams.fromStream(fis);
            lexer = new UUTLexer(input);
            tokens = new CommonTokenStream(lexer);
            parser = new UUTParser(tokens);
            /* disable anltr error listeners */
            parser.removeErrorListeners();
            parser.addErrorListener(Spectra.getErrorListener());
            
            ParseTree tree = parser.program();
            ParseTreeWalker walker = new ParseTreeWalker();
            ArrayList<String> spec = Spectra.produceSpectra(parser, tree, walker);
            // System.err.println("Tracking " + spec);
            int noOfErrors = parser.getNumberOfSyntaxErrors();
            if ((noOfErrors == 0 && pos) || (noOfErrors != 0 && !pos)) {
                // System.err.println(test + " " + pos + " " + noOfErrors);
                Logger.trackPassed(test, spec);
            } else {
                Logger.trackFailed(test, spec);
            }
        }
        Logger.spitJson();
    }
}