package stb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    
    

    public static LinkedList<GrammarReader> generatePopulation(int popSize) {
        String[] nonTerminals = {"default"};
        try(BufferedReader Buffer = new BufferedReader(new FileReader(Constants.GRAMMAR_PATH + Constants.CURR_GRAMMAR+".non_terminals")); ) {
            String line = "";
            while((line = Buffer.readLine()) != null) {
                nonTerminals = line.split(",");
            }
        } catch(Exception e ) {
            e.printStackTrace();
        }
        GrammarReader terminalGrammar = new GrammarReader(Constants.GRAMMAR_PATH + Constants.CURR_GRAMMAR + ".terminals");

        terminalGrammar.getAllRules().forEach(rule -> rule.getTotalProductions());
        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + i;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = ThreadLocalRandom.current().nextInt(Constants.MAX_PROD_SIZE) + 1;

            // System.out.println("Creating new grammar " + grammarName + " with " + grammarRuleCount + " rules");
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE);

                while(currGrammar.getParserRules().size() == 0 && currRuleLen == 0)
                    currRuleLen = ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE);
                StringBuilder ruleName = new StringBuilder();
                for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
                    ruleName.append((char)(97 + randInt(26)));
                }
                currGrammar.generateNewRule(ruleName.toString(), currRuleLen);
            }

            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachable();
            currGrammar.removeLR();
            // System.out.println("New grammar \n" + currGrammar.toString());
            output.add(currGrammar);
        }
        return output;
        
    }
    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}