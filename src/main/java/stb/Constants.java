package stb;

public class Constants {
    public static final String OUTPUT_DIR = "./antlrOut";
    public static final String GRAMMARS_PATH = "./grammars/";
    public static final String CURR_GRAMMAR_NAME = "dyck";
    public static final String SEEDED_GRAMMAR_PATH = GRAMMARS_PATH + "seeded/seeded.g4";
    public static final String TEST_DIR = "./tests/" + CURR_GRAMMAR_NAME;
    public static final String CURR_GRAMMAR_PATH = GRAMMARS_PATH+CURR_GRAMMAR_NAME+"/"+CURR_GRAMMAR_NAME+".g4";
    public static final String CURR_Terminals_PATH = GRAMMARS_PATH+CURR_GRAMMAR_NAME+"/"+CURR_GRAMMAR_NAME+".terminals";
    public static final boolean DEBUG = false;


    public static final double P_CHANGE_RULE_COUNT = 0.6; //P to add or remove a rule from a grammar
    public static final double P_ADD_RULE = 0.5;    //P to add a rule when adding/removing rules
    public static final double P_CHANGE_SYMBOL_COUNT = 0.6; //P to add or remove a rule from a grammar
    public static final double P_ADD_SYMBOL = 0.6; //P to add a symbol when adding/removing symbols
	public static final double P_M = 0.4;   //P to mutate a symbol in a grammar
    public static final double P_H = 0.55;  //P to make a symbol iterative or optional
    public static final double P_ITER = 0.55;  //P to make a symbol iterative 
    public static final double P_OPTIONAL = 0.55;  //P to make a symbol optional
    
    public static final double P_C = 0.65; // P to apply crossover
    public static final double P_G = 1.0;   // P to group/ungroup symbols on the RHS
    public static final double P_GROUP = 0.5;   // P to group symbols

    
	public static final boolean CHANGE_RULE_COUNT = true;
	public static final boolean CHANGE_SYMBOL_COUNT = true;
	public static final boolean GROUP = true;
	public static final boolean MUTATE = true;
	public static final boolean HEURISTIC = true;
	public static final boolean CROSSOVER = false;
    
	public static final int MAX_GRAMMAR_AGE = 15;
    public static final int POP_SIZE = 100;
    public static final int MAX_GRAMMARS  = 10000;
    public static final int MUTANTS_PER_BASE  = (int)MAX_GRAMMARS/POP_SIZE;

    public static final int NUM_ITERATIONS = 0;
	public static final int RULENAME_LEN = 7;
	public static final int BEST_GRAMMAR_COPY_COUNT = 5;
    public static final int MAX_RULE_COUNT = 5;
    public static final int MAX_RHS_SIZE = 6;
}