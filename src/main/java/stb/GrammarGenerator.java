package stb;

import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

public class GrammarGenerator {
    
    static int grammarCount = 0;

    public static LinkedList<GrammarReader> generatePopulation(int popSize) {
        GrammarReader terminalGrammar = new GrammarReader(Constants.CURR_Terminals_PATH);

        LinkedList<GrammarReader> output = new LinkedList<GrammarReader>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            GrammarReader currGrammar = new GrammarReader(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = ThreadLocalRandom.current().nextInt(Constants.MAX_PROD_SIZE) + 1;

            // System.out.println("Creating new grammar " + grammarName + " with " + grammarRuleCount + " rules");
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);

                //Generates a new rule with a random name
                currGrammar.generateNewRule(currRuleLen);
            }

            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachable();
            currGrammar.removeLR();
            // System.out.println("New grammar \n" + currGrammar.toString());
            output.add(currGrammar);
        }
        System.out.println("New Grammars ");
        output.forEach(grammar -> System.out.println(grammar));
        return output;
        
    }

    public static LinkedList<GrammarReader> generatePopulation(int popSize, GrammarReader bestGrammar) {
        if(bestGrammar.getScore() == 0.0) return generatePopulation(popSize);
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        int bestPopCount = Constants.BEST_GRAMMAR_COPY_COUNT;
        System.out.println("Adding " + Math.min(bestPopCount,popSize) + " copies of " + bestGrammar.getName() + " to population");
        if(popSize > bestPopCount) {
            for (int i = 0; i < bestPopCount; i++) {
                GrammarReader toAdd = new GrammarReader(bestGrammar);
                toAdd.setName("Grammar_" + grammarCount++);
                out.add(toAdd);
            }
            int newPopSize = popSize - bestPopCount;
            out.addAll(generatePopulation(newPopSize));
        } else {
            for (int i = 0; i < popSize; i++) {
                GrammarReader toAdd = new GrammarReader(bestGrammar);
                toAdd.setName("Grammar_" + grammarCount++);
                out.add(toAdd);
            }
        }
        return out; 
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}