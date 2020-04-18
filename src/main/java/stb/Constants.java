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

	public static final Double P_M = 0.4;
	public static final Double P_H = 0.55;
    public static final Double P_C = 0.65;
    public static final Double P_G = 1.0;
    
	public static final boolean CROSSOVER = false;
	public static final boolean MUTATE = true;
	public static final boolean HEURISTIC = true;
	public static final boolean GROUP = true;
    
	public static final int MAX_GRAMMAR_AGE = 15;
    public static final int POP_SIZE = 100;
    public static final int MAX_GRAMMARS  = 10000;
    public static final int NUM_ITERATIONS = 100;
	public static final int RULENAME_LEN = 7;
	public static final int BEST_GRAMMAR_COPY_COUNT = 5;
    public static final int MAX_PROD_SIZE = 4;
    public static final int MAX_RHS_SIZE = 4;
}