package stb;

import java.util.Arrays;

public class Constants {
        

    // public static  String ANTLR_DIR = "antlr-localizer/default/target/generated-sources/antlr4/za/ac/sun/cs/localizer";
    public static  String ANTLR_CLASS = "antlr-localizer/default/target/generated-sources/antlr4/za/ac/sun/cs/localizer";
    public static  String ANTLR_JAVA = "antlr-localizer/default/src/main/java/za/ac/sun/cs/localizer/dynamic";
    public static  String ANTLR_DIR = "./antlrOut";
    public static final String GRAMMARS_PATH = "./grammars/";
    public static final String CURR_GRAMMAR_NAME = "slearith";
    public static final String SEEDED_GRAMMAR_PATH = GRAMMARS_PATH + "seeded/seeded.g4";
    public static final String POS_TEST_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/pos";
    public static final String NEG_TEST_DIR = "./tests/" + CURR_GRAMMAR_NAME + "/neg";
    public static final String CURR_GRAMMAR_PATH = GRAMMARS_PATH+CURR_GRAMMAR_NAME+"/"+CURR_GRAMMAR_NAME+".g4";
    public static final String CURR_TERMINALS_PATH = GRAMMARS_PATH+CURR_GRAMMAR_NAME+"/"+CURR_GRAMMAR_NAME+".terminals";
    public static final String LOG_GRAMMAR_PATH = GRAMMARS_PATH + "/loggedGrammars/";
    public static final boolean DEBUG = false;
    public static final boolean USE_GUI = true;
	public static final boolean USE_LOCALIZATION = true;
	public static final String POS_MODE = "pos";
	public static final String NEG_MODE = "neg";
    private static  double P_C_MIN = 0;
    private static  double P_C_MAX = 0;


    public static double P_CHANGE_RULE_COUNT = 0.6; //P to add or remove a rule from a grammar
    public static double P_ADD_RULE = 0.5;    //P to add a rule when adding/removing rules
    public static double P_CHANGE_SYMBOL_COUNT = 0.6; //P to add or remove a rule from a grammar
    public static double P_ADD_SYMBOL = 0.6; //P to add a symbol when adding/removing symbols
	public static double P_M = 0.6;   //P to mutate a symbol in a grammar
    public static double P_H = 0.55;  //P to make a symbol iterative or optional
    public static double P_ITER = 0.55;  //P to make a symbol iterative 
    public static double P_OPTIONAL = 0.55;  //P to make a symbol optional
    public static int TOUR_SIZE = 5; //Size of tournaments when performing tour selection
    
    public static double P_C = 0.65; // P to apply crossover
    public static int NUM_CROSSOVER_PER_GEN = 5;

    public static double P_G = 0.4;   // P to group/ungroup symbols on the RHS
    public static double P_GROUP = 0.6;   // P to group symbols

    
	public static boolean CHANGE_RULE_COUNT = true;
	public static boolean CHANGE_SYMBOL_COUNT = true;
	public static boolean GROUP = true;
	public static boolean MUTATE = true;
	public static boolean HEURISTIC = true;
	public static boolean CROSSOVER = false;
    
    public static int INIT_POP_SIZE = 10000;
    public static int INIT_POP_SIZE_LOCAL = 20;
    public static int POP_SIZE = 100;
    public static int MAX_GRAMMARS = 2000;
    public static int MUTANTS_PER_BASE  = (int)MAX_GRAMMARS/POP_SIZE;
    //How many grammars from the hall of fame are selected for the next generation
    public static int HALL_OF_FAME_COUNT = 10;

    public static int NUM_ITERATIONS = 1000;
    public static int NUM_NEGATIVE_ITERATIONS = 10;
	public static int RULENAME_LEN = 7;
    public static int MAX_RULE_COUNT = 5;
    public static int MAX_RHS_SIZE = 6;


    public static String localizerGPath = "./antlr-localizer/default/src/main/antlr4/za/ac/sun/cs/localizer/UUT.g4";
    public static String localizerSPath = "./antlr-localizer/default/test.sh";
    public static String CSV_PATH = "./antlr-localizer/default/scores.csv";

    


    public static scoringLambda positiveScoring = (int[] testResult, Gram GUT) -> {
        int totalTests = testResult[1];
        
        
        double numPass = testResult[0];
        GUT.truePositivesPos = (int)numPass;
        GUT.falseNegatives = totalTests-(int)numPass;
        double out = numPass*1.0/totalTests;
        
        if(out == 1.0) {
            System.err.println(Arrays.toString(testResult));
            System.err.println("Positive grammar\n" + GUT);
            System.err.println("Press enter to continue");
            try {
                int result = System.in.read();
                if(result == 0) GUT.logGrammar();
            } catch (Exception e) {

            }
        }
        GUT.setPosScore(numPass/totalTests);
    };

    public static scoringLambda negativeScoring = (int[] testResult, Gram GUT) -> {
        int totalTests = testResult[1];
        
        double numPass = totalTests - testResult[0];
        GUT.truePositivesNeg = (int)numPass;
        GUT.falsePositives = testResult[0];
        

        GUT.setNegScore(numPass/totalTests);
    };

    /**
     * Sets the pos and negscores to numPass/totalTests
     * This is done because localisastion does not use pos and neg testing phases
     */
    public static scoringLambda localiserScoring = (int[] testResult, Gram GUT) -> {

        int totalTests = testResult[0] + testResult[1];
        
        double numPass = testResult[0];
        
        GUT.setNegScore(numPass/totalTests);
        GUT.setPosScore(numPass/totalTests);
    };

    public static double calculatePM(double posScore, double negScore) {

        return (1-(posScore+negScore)/2.0)*P_M;
    }
        




    public static void setAntlrDir(String newDir) {
        ANTLR_DIR = newDir;
    }

    public static void setPopSize(int newSize) {
        POP_SIZE = newSize;
    }

    public static double getP_CHANGE_RULE_COUNT() {
        return P_CHANGE_RULE_COUNT;
    }

    public static void setP_CHANGE_RULE_COUNT(double p_CHANGE_RULE_COUNT) {
        P_CHANGE_RULE_COUNT = p_CHANGE_RULE_COUNT;
    }

    public static double getP_ADD_RULE() {
        return P_ADD_RULE;
    }

    public static void setP_ADD_RULE(double p_ADD_RULE) {
        P_ADD_RULE = p_ADD_RULE;
    }

    public static double getP_CHANGE_SYMBOL_COUNT() {
        return P_CHANGE_SYMBOL_COUNT;
    }

    public static void setP_CHANGE_SYMBOL_COUNT(double p_CHANGE_SYMBOL_COUNT) {
        P_CHANGE_SYMBOL_COUNT = p_CHANGE_SYMBOL_COUNT;
    }

    public static double getP_ADD_SYMBOL() {
        return P_ADD_SYMBOL;
    }

    public static void setP_ADD_SYMBOL(double p_ADD_SYMBOL) {
        P_ADD_SYMBOL = p_ADD_SYMBOL;
    }

    public static double getP_M() {
        return P_M;
    }

    public static void setP_M(double p_M) {
        P_M = p_M;
    }

    public static double getP_H() {
        return P_H;
    }

    public static void setP_H(double p_H) {
        P_H = p_H;
    }

    public static double getP_ITER() {
        return P_ITER;
    }

    public static void setP_ITER(double p_ITER) {
        P_ITER = p_ITER;
    }

    public static double getP_OPTIONAL() {
        return P_OPTIONAL;
    }

    public static void setP_OPTIONAL(double p_OPTIONAL) {
        P_OPTIONAL = p_OPTIONAL;
    }

    public static double getP_C() {
        return P_C;
    }

    public static void setP_C(double p_C) {
        P_C = p_C;
    }

    public static double getP_G() {
        return P_G;
    }

    public static void setP_G(double p_G) {
        P_G = p_G;
    }

    public static double getP_GROUP() {
        return P_GROUP;
    }

    public static void setP_GROUP(double p_GROUP) {
        P_GROUP = p_GROUP;
    }

    public static boolean isCHANGE_RULE_COUNT() {
        return CHANGE_RULE_COUNT;
    }

    public static void setCHANGE_RULE_COUNT(boolean cHANGE_RULE_COUNT) {
        CHANGE_RULE_COUNT = cHANGE_RULE_COUNT;
    }

    public static boolean isCHANGE_SYMBOL_COUNT() {
        return CHANGE_SYMBOL_COUNT;
    }

    public static void setCHANGE_SYMBOL_COUNT(boolean cHANGE_SYMBOL_COUNT) {
        CHANGE_SYMBOL_COUNT = cHANGE_SYMBOL_COUNT;
    }

    public static boolean isGROUP() {
        return GROUP;
    }

    public static void setGROUP(boolean gROUP) {
        GROUP = gROUP;
    }

    public static boolean isMUTATE() {
        return MUTATE;
    }

    public static void setMUTATE(boolean mUTATE) {
        MUTATE = mUTATE;
    }

    public static boolean isHEURISTIC() {
        return HEURISTIC;
    }

    public static void setHEURISTIC(boolean hEURISTIC) {
        HEURISTIC = hEURISTIC;
    }

    public static boolean isCROSSOVER() {
        return CROSSOVER;
    }

    public static void setCROSSOVER(boolean cROSSOVER) {
        CROSSOVER = cROSSOVER;
    }

    public static int getPOP_SIZE() {
        return POP_SIZE;
    }

    public static void setPOP_SIZE(int pOP_SIZE) {
        POP_SIZE = pOP_SIZE;
    }

    public static int getMAX_GRAMMARS() {
        return MAX_GRAMMARS;
    }

    public static void setMAX_GRAMMARS(int mAX_GRAMMARS) {
        MAX_GRAMMARS = mAX_GRAMMARS;
    }

    public static int getNUM_ITERATIONS() {
        return NUM_ITERATIONS;
    }

    public static void setNUM_ITERATIONS(int nUM_ITERATIONS) {
        NUM_ITERATIONS = nUM_ITERATIONS;
    }

    public static double getP_C_MIN() {
        return P_C_MIN;
    }

    public static void setP_C_MIN(double p_C_MIN) {
        P_C_MIN = p_C_MIN;
    }

    public static double getP_C_MAX() {
        return P_C_MAX;
    }

    public static void setP_C_MAX(double p_C_MAX) {
        P_C_MAX = p_C_MAX;
    }

    }