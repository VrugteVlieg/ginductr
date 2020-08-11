package za.ac.sun.cs.localizer;

import za.ac.sun.cs.localizer.depend.Pipeline;
import za.ac.sun.cs.localizer.dynamic.TestRunner;

public class Main {
    /**
     * Main class for the project
     */ 
    public static void main(String[] args) {
        /*
         * the grammar under test is located inside proj. dir.
         * for now, pass as commandline 
         */
        if (args.length != 2) {
            System.out.println("Usage: java Main <v4 grammar file> <dir to tests>");
            System.exit(0);
        }
        pipeline(args[0]);
        runTests(args[1]);
    }
    private static void pipeline(String grammar) {
        try {
            Pipeline.pipeline(grammar);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    private static void runTests(String tests) {
        try {
            TestRunner.parse(tests);
        } catch(Exception e) {
            e.printStackTrace();
        } 
    }
}
