package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class GrammarReader {

    private File grammarFile;
    private String grammarName;
    private ArrayList<Rule> parserRules  = new ArrayList<Rule>();;
    private ArrayList<Rule> terminalRules;
    private boolean positiveAcceptance = false; //does this grammar accept all positive cases
    private double score = 0.0;
    private boolean remove = false;

    /**
     * Constructs a grammar from a given file
     * @param filePath
     */
    public GrammarReader(String filePath) {
        grammarFile = new File(filePath);
        grammarName = grammarFile.getName().split("\\.")[0];
        this.terminalRules = new ArrayList<Rule>();
        readRules();
    }

    public GrammarReader(GrammarReader toCopy) {
        grammarName = toCopy.grammarName;
        parserRules = new ArrayList<Rule>(toCopy.parserRules);
        terminalRules = new ArrayList<Rule>(toCopy.terminalRules);
    }

    /**
     * Constructs a grammar with a given set of terminal rules
     * @param name
     * @param terminalRules
     */
    public GrammarReader(String name, ArrayList<Rule> terminalRules) {
        grammarName = name;
        this.terminalRules = new ArrayList<Rule>(terminalRules);
    }

    public ArrayList<Rule> getAllRules() {
        return new ArrayList<Rule>(Stream.of(parserRules, terminalRules).flatMap(x -> x.stream()).collect(Collectors.toList()));
    }

    private void readRules() {
        try (BufferedReader in = new BufferedReader(new FileReader(grammarFile));){
            String input;
            in.readLine();
            while((input = in.readLine()) != null) {
                System.out.println(input);
                input = input.replaceAll("[ ]+", " ").trim();
                String RuleName = input.substring(0,input.indexOf(":")).trim();
                String RuleString = input.substring(input.indexOf(":")+1); //cuts off the ; at the end
                Rule currRule = new Rule(RuleName, RuleString);
                // currRule.addRule(RuleString);
                if(currRule.isTerminal()) {
                    terminalRules.add(currRule);
                } else {
                    parserRules.add(currRule);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getStartSymbol() {
        return parserRules.get(0).getName();
    }

    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("grammar " + grammarName + ";\n");
        getAllRules().forEach(Rule -> out.append(Rule + "\n"));
        return out.toString();
    }

    public String getName() {return grammarName;}

    ArrayList<Rule> getParserRules() {
        return parserRules;
    }

    public void setTerminalRules(ArrayList<Rule> newRules) {
        this.terminalRules =  newRules;
    }

    public void generateNewRule(String ruleName, int RHSLen) {
        if(RHSLen == 0) return;
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        // System.out.println(allRules);
        for (int i = 0; i < RHSLen; i++) {
            int newRulesIndex = ThreadLocalRandom.current().nextInt(allRules.size());
            newRuleText.append(allRules.get(newRulesIndex).getName() + " ");
        }
        newRuleText.append(";");
        Rule toAdd =  new Rule(ruleName,newRuleText.toString());
        for(Rule rule: parserRules) {
            if(rule.getName().equals(ruleName)) {
                // System.out.println("generating " + toAdd.getSubRules() + " as an alternative to " + toAdd.getName());
                rule.addAlternative(toAdd.getSubRules());
                return;
            }
        }
        parserRules.add(0,toAdd);
    }

	public void setPositiveAcceptance(boolean toSet) {
        this.positiveAcceptance = toSet;
	}

    public boolean getPositiveAcceptance() {return positiveAcceptance;}

	public void mutate(Double pM, Double pH) {
        if(Math.random() < pM || true) {
            if(parserRules.size() == 1) {
                // System.out.println("Only 1 rule left in parser " + parserRules.get(0));
            } else if(parserRules.size() == 0) {
                // System.out.println(this.grammarName + " has no parser rules");
            } else {
                int ruleIndex  = randInt(parserRules.size());
                Rule toMutate = parserRules.get(ruleIndex);
                int productionIndex = randInt(toMutate.getTotalProductions());
                if(productionIndex == 0) { //this symbol is now becoming an alternative to another rule
                    int newRuleIndex = randInt(parserRules.size());
                    while(newRuleIndex == ruleIndex) newRuleIndex = randInt(parserRules.size());
                    Rule ruleToExtend = parserRules.get(newRuleIndex);
                    // System.out.println("Adding " + toMutate + " as an alternative to " + ruleToExtend.getName());
                    ruleToExtend.addAlternative(toMutate.getSubRules());
                    parserRules.remove(toMutate);
                } 
                else {
                    int toInsertIndex = randInt(parserRules.size());
                    while(toInsertIndex == ruleIndex) toInsertIndex = randInt(parserRules.size());
                    Rule toInsert = parserRules.get(toInsertIndex);
                    // System.out.println("Setting rule " + productionIndex + " of " + toMutate .getName()+ " to " + toInsert.getName());
                    toMutate.setProduction(productionIndex,toInsert);
                }
            }
        }
    }
    
    public int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public void setScore(double newScore) {score = newScore;}
    public double getScore() {return score;}

	public void crossover(GrammarReader toCrossover) {
        boolean currShortest = this.parserRules.size() < toCrossover.parserRules.size();
        int indexToCut = 1 + randInt(currShortest ? this.parserRules.size() : toCrossover.parserRules.size());
        ArrayList<Rule> toSend = new ArrayList<Rule>();
        ArrayList<Rule> toRecv = new ArrayList<Rule>();
        for (int i = 0; i < indexToCut; i++) {
            toSend.add(new Rule(parserRules.get(i)));
            toRecv.add(new Rule(toCrossover.parserRules.get(i)));
        }
        // System.out.println("Swapping " + toSend + " from " + this.getName() + "\nwith " + toRecv + " from " + toCrossover.getName());
        for (int i = 0; i < indexToCut; i++) {
            parserRules.set(i, toRecv.get(i));
            toCrossover.parserRules.set(i, toSend.get(i));
        }

        cleanupMergeRules();
        toCrossover.cleanupMergeRules();
        
        // System.out.println("Resulting in " + this + "\nand\n" + toCrossover);
    }
    /**
     * Iterate through rules and merge rules with duplicate names
     */
    public void cleanupMergeRules() {
        for (int i = 0; i < parserRules.size(); i++) {
            Rule currRule = parserRules.get(i);
            int toMergeIndex = parserRules.lastIndexOf(currRule);
            if(toMergeIndex != i) {
                // System.out.println(currRule.getName() + " is already present in " + this.getName() + " as " + parserRules.get(toMergeIndex));
                currRule.addAlternative(parserRules.get(toMergeIndex).getSubRules());
                // System.out.println("Merged to " + currRule);
                parserRules.remove(toMergeIndex);
            } else {
                // System.out.println(currRule.getName() + " is not duplicated in " + this.getName());
            }
        }
    }
    
	public void removeUnreachable() {
        Rule startSymbol = parserRules.get(0);
        ArrayList<String> reachableRules = new ArrayList<String>();
        startSymbol.getReachables(parserRules, reachableRules);
        // System.out.println("Reachable rules " + reachableRules);
        parserRules.removeIf(rule -> !reachableRules.contains(rule.getName()));
        // System.out.println("Filtered parserRules " + parserRules);
	}

	public void heuristic(Double pH) {
        int ruleIndex = randInt(parserRules.size());
        Rule mainToChange = parserRules.get(ruleIndex);
        // System.out.println(mainToChange + " has " + mainToChange.getTotalProductions() + " productions");
        int toChangeIndex = randInt(mainToChange.getTotalProductions());
        Rule toChange = mainToChange.getProduction(toChangeIndex);
        double choice = Math.random();
        // System.out.println("Changing rule " + toChangeIndex + " of " + mainToChange.getName());
        if(choice < 0.33) {
            toChange.setIterative(!toChange.isIterative());
        } else if(choice < 0.66) {
            toChange.setOptional(!toChange.isOptional());
        } else {
            toChange.setOptional(!toChange.isOptional());
            toChange.setIterative(!toChange.isIterative());
        }
    }
    
    public void flagForRemoval() {
        this.remove = true;
    }

    public boolean toRemove() {
        return remove;
    }
}