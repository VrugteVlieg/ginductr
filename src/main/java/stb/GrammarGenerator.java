package stb;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    public static int MAX_PROD_SIZE = 8;
    public static int MAX_RHS_SIZE = 6;
    

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
        System.out.println("Non terminals = " + Arrays.toString(nonTerminals));
        GrammarReader terminalGrammar = new GrammarReader(Constants.GRAMMAR_PATH + Constants.CURR_GRAMMAR + ".terminals");

        terminalGrammar.getAllRules().forEach(rule -> rule.getTotalProductions());
        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + i;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = ThreadLocalRandom.current().nextInt(MAX_PROD_SIZE) + 1;

            System.out.println("Creating new grammar " + grammarName + " with " + grammarRuleCount + " rules");
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = ThreadLocalRandom.current().nextInt(MAX_RHS_SIZE);

                while(currGrammar.getParserRules().size() == 0 && currRuleLen == 0)
                    currRuleLen = ThreadLocalRandom.current().nextInt(MAX_RHS_SIZE);
                    
                String ruleName = nonTerminals[randInt(nonTerminals.length)];
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }
            
            System.out.println("New grammar \n" + currGrammar.toString());
            output.add(currGrammar);
        }
        return output;
        
    }
    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}