package stb;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    
    static int grammarCount = 0;
    static Gram terminalGrammar = new Gram(new File(Constants.CURR_TERMINALS_PATH));

    public static LinkedList<Gram> generatePopulation(int popSize) {
        LinkedList<Gram> output = new LinkedList<Gram>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            System.err.println("Generating " + grammarName);
            Gram currGrammar = new Gram(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = 1 + randInt(Constants.MAX_RULE_COUNT);
            
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE-1);
                String ruleName =  currGrammar.genRuleName();
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }

            currGrammar.removeDuplicateProductions();
            // StringBuilder curr = new StringBuilder();
            // curr.append("Before removeUnreach\n");
            // curr.append(currGrammar.toString());
            System.err.println("Removing unreachables");
            currGrammar.removeUnreachable();
            System.err.println("Done removing");
            // currGrammar.removeUnreachableBoogaloo();
            // curr.append("\nPost removeUnreach\n");
            // curr.append(currGrammar.toString());
            // App.rgoSetText(curr.toString());
            // try {
            //     System.out.println("Displaying removeUnreachable\nPress enter to cont");
            //     System.in.read();
            // } catch(Exception e) {

            // }

            // currGrammar.removeLR();
            // System.out.println("New grammar \n" + currGrammar.toString());
            if(App.generatedGrammars.contains(currGrammar.hashString())) {
                i--;
                continue;
            } else {
                App.generatedGrammars.add(currGrammar.hashString());
                output.add(currGrammar);
            }
        }
        // System.out.println("New Grammars ");
        // output.forEach(grammar -> System.out.println(grammar));

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

    public static LinkedList<Gram> generateLocalisablePop(int popSize, HashSet<String> checkedGrammar) {
        LinkedList<Gram> out = new LinkedList<Gram>();
        while(out.size() < popSize) {
            App.rgoSetText("Init size " + out.size() + "/" + popSize);
            LinkedList<Gram> newPop = generatePopulation(10);
            System.err.println("testing grammars");
            newPop.stream()
            .peek(App::runTests)
            .filter(grammar -> grammar.getPosScore() > 0 && !grammar.toRemove())
            .forEach(out::add);
        }
        
        out = new LinkedList<Gram>(out.subList(0, popSize));
        
        return out;
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}