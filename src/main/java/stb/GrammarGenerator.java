package stb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    
    static int grammarCount = 0;

    public static LinkedList<GrammarReader> generatePopulation(int popSize) {
        GrammarReader terminalGrammar = new GrammarReader(new File(Constants.CURR_Terminals_PATH));
        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = ThreadLocalRandom.current().nextInt(Constants.MAX_RULE_COUNT) + 1;
            // System.out.println("Creating new grammar " + grammarName + " with " + grammarRuleCount + " rules");
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);

                //Generates a new rule with a random name
                currGrammar.generateNewRule(currRuleLen);
            }
            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachable();
            // currGrammar.removeLR();
            // System.out.println("New grammar \n" + currGrammar.toString());
            output.add(currGrammar);
        }
        // System.out.println("New Grammars ");
        // output.forEach(grammar -> System.out.println(grammar));

        return output; 
    }

    public static void fillBlanks(GrammarReader toFill) {
        ArrayList<Rule> mainRules = toFill.getParserRules();
        for (int mainIndex = 0; mainIndex < mainRules.size(); mainIndex++) {
            if(mainRules.get(mainIndex).toString().contains("_new_")) {
                ArrayList<LinkedList<Rule>> currRule = mainRules.get(mainIndex).getSubRules();
                for (int prodIndex = 0; prodIndex < currRule.size(); prodIndex++) {
                    LinkedList<Rule> currProd = currRule.get(prodIndex);
                    for (int ruleIndex = 0; ruleIndex < currProd.size(); ruleIndex++) {
                        if(currProd.get(ruleIndex).getName().equals("_new_")){
                            int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);
                            Rule newRule = toFill.generateReturnNewRule(currRuleLen);
                            toFill.getParserRules().add(newRule);
                            currProd.set(ruleIndex,newRule.makeMinorCopy());
                        }
                    }
                }
            }
        }
        
    }

    public static LinkedList<GrammarReader> generatePopulation(GrammarReader baseGrammar, HashMap<Integer,Boolean> checkedGrammars) {
       LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
       int maxRules = Constants.MAX_RULE_COUNT-1;
       for (int i = 0; i < maxRules; i++) {
           GrammarReader curr = new GrammarReader(baseGrammar);
           for (int j = 0; j < i; j++) {
                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);
                curr.generateNewRule(currRuleLen);
            }
            out.addAll(curr.computeMutants(Constants.BEST_GRAMMAR_COPY_COUNT, checkedGrammars));
       }

       return out;
    }


    public static LinkedList<GrammarReader> generateLocalisablePop(int popSize, HashMap<Integer, Boolean> checkedGrammar, scoringLambda criteria) {
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        while(out.size() < popSize) {
            App.runGrammarOutput.output("Init size " + out.size() + "/" + popSize);
            LinkedList<GrammarReader> newPop = generatePopulation(200);
            newPop.stream()
            .filter(grammar -> !checkedGrammar.containsKey(grammar.hashCode()))
            .forEach(grammar -> checkedGrammar.put(grammar.hashCode(), true));
            newPop.stream()
            .map(grammar -> {
                App.runTests(grammar, Constants.POS_TEST_DIR, criteria);
                return grammar; })
            .filter(grammar -> grammar.getPosScore() != 0.0)
            .limit(popSize-out.size())
            .forEach(out::add);
        }
        return out;
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}