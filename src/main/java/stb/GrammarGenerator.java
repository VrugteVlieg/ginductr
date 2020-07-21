package stb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GrammarGenerator {
    
    static int grammarCount = 0;
    static GrammarReader terminalGrammar = new GrammarReader(new File(Constants.CURR_TERMINALS_PATH));

    public static LinkedList<GrammarReader> generatePopulation(int popSize) {
        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = 1 + randInt(Constants.MAX_RULE_COUNT);
            
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE-1);
                String ruleName =  currGrammar.genRuleName();
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }

            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachable();
            // currGrammar.removeLR();
            // System.out.println("New grammar \n" + currGrammar.toString());
            App.generatedGrammars.add(currGrammar.hashString());
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
                            String ruleName = toFill.genRuleName();
                            Rule newRule = toFill.generateReturnNewRule(ruleName, currRuleLen);
                            toFill.getParserRules().add(newRule);
                            currProd.set(ruleIndex,newRule.makeMinorCopy());
                        }
                    }
                }
            }
        }
        
    }

    public static LinkedList<GrammarReader> generateLocalisablePop(int popSize, HashSet<String> checkedGrammar, scoringLambda criteria) {
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        while(out.size() < popSize) {
            App.runGrammarOutput.output("Init size " + out.size() + "/" + popSize);
            LinkedList<GrammarReader> newPop = generatePopulation(200);
            
            newPop.stream()
            .filter(grammar -> !checkedGrammar.contains(grammar.hashString()))
            .peek(grammar -> checkedGrammar.add(grammar.hashString()))
            .map(grammar -> {
                App.runTests(grammar, Constants.POS_MODE, criteria);
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