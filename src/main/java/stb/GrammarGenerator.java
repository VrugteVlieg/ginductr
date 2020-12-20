package stb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

public class GrammarGenerator {

    static HashSet<Integer> nullGrams = new HashSet<>();
    static int grammarCount = 0;
    static Gram terminalGrammar = new Gram(new File(Constants.CURR_TERMINALS_PATH));

    public static LinkedList<Gram> generatePopulation(int popSize) {
        LinkedList<Gram> output = new LinkedList<Gram>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + Gram.genGramName();
            // System.err.println("Generating " + grammarName);
            Gram currGrammar = new Gram(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = 1 + randInt(Constants.MAX_RULE_COUNT);
            
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE-1);
                String ruleName =  currGrammar.genRuleName();
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }

            currGrammar.removeDuplicateProductions();
            
            currGrammar.removeUnreachableBoogaloo();
            boolean lrDeriv = currGrammar.containsImmediateLRDeriv();
            if(App.gramAlreadyChecked(currGrammar) || lrDeriv) {
                i--;
                continue;
            } else {
                currGrammar.initString = currGrammar.toString();
                output.add(currGrammar);
            }
        }
        return output; 
    }

    public static LinkedList<Gram> generateDemoCrossoverPop() {
        LinkedList<Gram> output = new LinkedList<Gram>();
        for (int i = 0; i < 1; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            // System.err.println("Generating " + grammarName);
            Gram currGrammar = new Gram(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = 1 + randInt(2);
            
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + randInt(2);
                String ruleName =  currGrammar.genRuleName();
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }
            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachableBoogaloo();
            boolean lrDeriv = currGrammar.containsImmediateLRDeriv();
            if(App.gramAlreadyChecked(currGrammar) || lrDeriv) {
                i--;
                continue;
            } else {
                output.add(currGrammar);
            }
        }
        return output; 
    }

    public static void fillBlanks(Gram toFill) {
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

    public static LinkedList<Gram> generateLocalisablePop(int popSize) {
        LinkedList<Gram> out = new LinkedList<Gram>();
        while(out.size() < popSize) {
            // App.rgoSetText("Generating new population: " + out.size() + "/" + popSize);
            LinkedList<Gram> newPop = generatePopulation(Constants.NUM_THREADS * 5);
            newPop.removeIf(gram -> nullGrams.contains(gram.hashString().hashCode()));
            App.runTests(newPop);
            // newPop.forEach(App::runTests);
            newPop.forEach(gram -> {if(gram.getPosScore() == 0.0) nullGrams.add(gram.hashString().hashCode());});
            newPop.removeIf(Predicate.not(Gram.passesPosTest));
            out.addAll(newPop);
        }
        
        out = new LinkedList<Gram>(out.subList(0, popSize));
        
        return out;
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}