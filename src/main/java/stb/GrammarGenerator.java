package stb;

import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    public static int MAX_PROD_SIZE = 8;
    public static int MAX_RHS_SIZE = 6;
    

    public static LinkedList<GrammarReader> generatePopulation(int popSize) {
        GrammarReader terminalGrammar = new GrammarReader(Constants.GRAMMAR_PATH + Constants.CURR_GRAMMAR + ".terminals");
        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + i;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = ThreadLocalRandom.current().nextInt(MAX_PROD_SIZE);
            System.out.println("Creating new grammar " + grammarName + " with " + grammarRuleCount + " rules");
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = ThreadLocalRandom.current().nextInt(MAX_RHS_SIZE);
                currGrammar.generateNewRule(currRuleLen);
            }
            System.out.println("New grammar \n" + currGrammar.toString());
            output.add(currGrammar);
        }
        return output;
        
    }
}