package stb;

import java.util.Arrays;

public class Constants {

    public static final String CURR_GRAMMAR_NAME = "dyck4";
    public static final String CURR_MLCS_PATH = String.join("/", "grammars", CURR_GRAMMAR_NAME, CURR_GRAMMAR_NAME + ".mlcs");
    public static boolean USE_LOCALIZATION = true;
    public static boolean USE_PARTIAL_SCORING = true;
    public static boolean USE_MLCS = false;

    public static String ANTLR_CLASS = "antlr-localizer/default/target/generated-sources/antlr4/za/ac/sun/cs/localizer";
    public static String ANTLR_JAVA = "antlr-localizer/default/src/main/java/za/ac/sun/cs/localizer/dynamic";
    public static String ANTLR_DIR = "./antlrOut";
    public static String GRAM_START_SYMB = "program";
    public static int NUM_THREADS = 8;
    public static String LOG_DIR = "./logs";
    public static String PERFECT_LOG_DIR = "./logs/perfectGrams";
    public static String SLOW_LOG_DIR = "./logs/slowGrams";
    public static String LOCALISER_JAVA_DIR = "./localiserDependJava";
    public static String LOCALISER_CLASS_DIR = "./localiserDependClass";
    public static final String GRAMMARS_PATH = "./grammars/";
    public static final String SEEDED_GRAMMAR_PATH = GRAMMARS_PATH + "seeded/seeded.g4";
    public static String POS_TEST_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/mass/test0/pos";
    public static final String NEG_TEST_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/neg";
    public static final String VALIDATION_POS_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/symmetricTesting/pos";
    public static final String VALIDATION_NEG_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/symmetricTesting/neg";
    public static final String CURR_GRAMMAR_PATH = GRAMMARS_PATH + CURR_GRAMMAR_NAME + "/" + CURR_GRAMMAR_NAME + ".g4";
    public static final String CURR_TERMINALS_PATH = GRAMMARS_PATH + CURR_GRAMMAR_NAME + "/" + CURR_GRAMMAR_NAME
            + ".terminals";
    public static final String LOG_GRAMMAR_PATH = GRAMMARS_PATH + "loggedGrammars/";
    public static final boolean DEBUG = false;
    public static final boolean USE_GUI = true;
    public static final String POS_MODE = "pos";
    public static final String NEG_MODE = "neg";
    public static final String POS_VAL_MODE = "posVal";
    public static final String NEG_VAL_MODE = "negVal";
    

    public static double[] P_CHANGE_RULE_COUNT_VALS = { 0.5 };
    public static double P_CHANGE_RULE_COUNT = 0.5; // P to add or remove a rule from a grammar

    public static double[] P_CHANGE_SYMB_VALS = { 0.3, 0.4, 0.5 };
    public static double P_CHANGE_SYMBOL_COUNT = 0.5; // P to add or remove a symbol from a rule

    public static double[] P_ADD_SYMB_VALS = { 0.5 };
    public static double P_ADD_SYMBOL = 0.5; // P to add a symbol when adding/removing symbols

    public static double[] P_M_VALS = { 0.3, 0.4, 0.5 };
    public static double P_M = 0.5; // P to mutate a symbol in a grammar

    public static double[] P_H_VALS = { 0.3, 0.4, 0.5 };
    public static double P_H = 0.5; // P to make a symbol iterative or optional

    public static double[] TOUR_SIZE_VALS = { 0.1, 0.15 };
    public static int TOUR_SIZE = 11; // Size of tournaments when performing tour selection

    public static int NUM_CROSSOVER_PER_GEN = 10;

    public static double[] P_G_VALS = { 0.5 };
    public static double P_G = 0.5; // P to group/ungroup symbols on the RHS

    public static double P_GROUP = 0.5; // P to group symbols

    public static boolean CHANGE_RULE_COUNT = true;
    public static boolean CHANGE_SYMBOL_COUNT = true;
    public static boolean GROUP = true;
    public static boolean MUTATE = true;
    public static boolean HEURISTIC = true;
    public static boolean CROSSOVER = false;

    public static int INIT_POP_SIZE = 100;
    public static int POP_SIZE = 100;
    public static int FRESH_POP = 10;
    // How many grammars from the hall of fame are selected for the next generation
    public static int HALL_OF_FAME_COUNT = 10;

    public static int NUM_ITERATIONS = 70;
    public static int RULENAME_LEN = 10;
    public static int MAX_RULE_COUNT = 4;
    public static int MAX_RHS_SIZE = 8;

    public static String localizerGPath = "./antlr-localizer/default/src/main/antlr4/za/ac/sun/cs/localizer/UUT.g4";
    public static String localizerCompilPath = "./antlr-localizer/default/target/generated-sources/antlr4";
    public static String localizerSPath = "./antlr-localizer/default/test.sh";
    public static String localScript = "./localise.sh";
    public static String CSV_PATH = "./antlr-localizer/default/scores.csv";

    public static scoringLambda positiveScoring = (int[] testResult, Gram GUT) -> {
        int totalTests = testResult[1];

        double numPass = testResult[0];
        GUT.truePostives = (int) numPass;
        GUT.falseNegatives = totalTests - (int) numPass;
        double out = numPass * 1.0 / totalTests;
        // System.err.println("Pos scoring with " + Arrays.toString(testResult) + " score: " + out);

        GUT.setPosScore(out);
    };

    public static scoringLambda negativeScoring = (int[] testResult, Gram GUT) -> {
        int totalTests = testResult[1];

        double numPass = totalTests - testResult[0];
        GUT.trueNegatives = (int) numPass;
        GUT.falsePositives = testResult[0];

        double out = numPass * 1.0 / totalTests;
        // System.err.println("Neg scoring with " + Arrays.toString(testResult) + " score: " + out);
        GUT.setNegScore(out);
    };

    public static String getParamString() {
        return String.format(
                "iterCount:%d\n" + "popSize:%d\n" + "maxRuleCount:%d\n" + "initPopSize:%d\n" + "xCount:%d\n"
                        + "freshPop:%d" + "pCRC:%f",
                NUM_ITERATIONS, POP_SIZE, MAX_RULE_COUNT, INIT_POP_SIZE, NUM_CROSSOVER_PER_GEN, FRESH_POP, P_CHANGE_RULE_COUNT);
    }

    /**
     * Sets the pos and negscores to numPass/totalTests This is done because
     * localisastion does not use pos and neg testing phases
     */
    public static scoringLambda localiserScoring = (int[] testResult, Gram GUT) -> {

        int totalTests = testResult[0] + testResult[1];

        double numPass = testResult[0];

        GUT.setNegScore(numPass / totalTests);
        GUT.setPosScore(numPass / totalTests);
    };

    public static double calculatePM(double posScore, double negScore) {

        return (1 - (posScore + negScore) / 2.0) * P_M;
    }
}