package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/*
Mutation list
add rule
remove rule
add symbol to rule RHS
remove symbol from rule RHS
mutate symbol in rule
heuristic symbol in rule
group symbols
ungroup symbols
*/

public class GrammarReader {

    private File grammarFile;
    private String grammarName;
    private ArrayList<Rule> parserRules  = new ArrayList<Rule>();;
    private ArrayList<Rule> terminalRules;
    private boolean positiveAcceptance = false; //does this grammar accept all positive cases
    private double score = 0.0;
    private boolean remove = false;
    private int age = 0;

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

    /**
     * Creates a deep copy of a grammarReader
     */
    public GrammarReader(GrammarReader toCopy) {
        grammarName = toCopy.grammarName;
        parserRules = new ArrayList<Rule>();
        toCopy.parserRules.forEach(rule -> parserRules.add(new Rule(rule)));
        terminalRules = new ArrayList<Rule>();
        toCopy.terminalRules.forEach(rule -> terminalRules.add(new Rule(rule)));
        this.score = toCopy.score;
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
    /**
     * Concats parserRules and terminals and returns
     */
    public ArrayList<Rule> getAllRules() {
        return new ArrayList<Rule>(Stream.of(parserRules, terminalRules).flatMap(x -> x.stream()).collect(Collectors.toList()));
    }

    /**
     * Reads a grammar from the ANTLR file associated with this
     */
    private void readRules() {
        try (BufferedReader in = new BufferedReader(new FileReader(grammarFile));){
            String input;
            in.readLine();
            while((input = in.readLine()) != null) {
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

    //Calculates the ANTLR grammar form of this
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("grammar " + grammarName + ";\n");
        getAllRules().forEach(Rule -> out.append(Rule + "\n"));
        return out.toString();
    }

    public void printParserRules() {
        StringBuilder out = new StringBuilder();
        out.append(grammarName + "\n");
        parserRules.forEach(Rule -> out.append(Rule + "\n"));
        System.err.println(out);
    }

    public String getName() {return grammarName;}

    ArrayList<Rule> getParserRules() {
        return parserRules;
    }

    public void setTerminalRules(ArrayList<Rule> newRules) {
        this.terminalRules =  newRules;
    }

    /**
     * Generates a new rule and sets it as the start symbol, this rule can references any rule
     * @param ruleName
     * @param RHSLen
     */
    public void generateNewRule(String ruleName, int RHSLen) {
        if(RHSLen == 0) return;
       
       
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {
            int newRulesIndex = ThreadLocalRandom.current().nextInt(allRules.size());
            newRuleText.append(allRules.get(newRulesIndex).getName() + " ");
        }
        newRuleText.append(";");
        Rule toAdd =  new Rule(ruleName,newRuleText.toString());
        for(Rule rule: parserRules) {
            if(rule.getName().equals(ruleName)) {
                rule.addAlternative(toAdd.getSubRules());
                return;
            }
        }
        parserRules.add(0,toAdd);
    }
    /**
     * Wrapper for generateNewRule when a random name can be used
     */
    public void generateNewRule(int RHSLen) {
        generateNewRule(Rule.genName(), RHSLen);
    }

	public void setPositiveAcceptance(boolean toSet) {
        this.positiveAcceptance = toSet;
	}

    public boolean getPositiveAcceptance() {return positiveAcceptance;}

    /**
     * Selects a random parserRule A and sets a random symbol B in A to be another rule C in the grammar
     * this can include selecting the LHS as the thing to be mutated, in this case A has its RHS added as alternative to C and A is removed
     * All references to A are then made references to C
     */
	public void mutate() {
        System.out.println(this);
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
                boolean toExtendNullable = ruleToExtend.nullable(parserRules);
                parserRules.forEach(rule -> rule.replaceReferences(toMutate, ruleToExtend, toExtendNullable));
                parserRules.remove(toMutate);
            } 
            else {
                int toInsertIndex = randInt(parserRules.size());
                while(toInsertIndex == ruleIndex) toInsertIndex = randInt(parserRules.size());
                Rule toInsert = parserRules.get(toInsertIndex);
                // System.out.println("Setting rule " + productionIndex + " of " + toMutate .getName()+ " to " + toInsert.getName());
                toMutate.setProduction(productionIndex,toInsert);
            }
            System.out.println(this);
        }
    }
    
    public int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public void setScore(double newScore) {score = newScore;}
    public double getScore() {return score;}


    /**
     * Performs mernik crossover with another grammar
     * TODO redo this function
     * @param toCrossover
     */
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
            //Finds all indices of the rule that is not the first appearance, starting at the back
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
    /**
     * Removes all rules that cannot be reached from the start symbol
     */
	public void removeUnreachable() {
        Rule startSymbol = parserRules.get(0);
        ArrayList<String> reachableRules = startSymbol.getReachables(parserRules);
        int sizePre = parserRules.size();
        parserRules.removeIf(rule -> !reachableRules.contains(rule.getName()));
        int sizePost = parserRules.size();
        int numRulesToGen = randInt(sizePre-sizePost);
        System.out.println("Generating " + numRulesToGen + " for " + grammarName);
        for (int i = 0; i < numRulesToGen; i++) {
            int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);
            generateNewRule(currRuleLen);
        }
        if(numRulesToGen != 0) System.out.println("Checking if new rules are reachable in " + this);
        if(numRulesToGen != 0) removeUnreachable();
	}
    /**
     * Applies the heuristic mutation, this can change a production to either optional, iterative , both, or none 
     * A ruleis not changed if it is nullable
     * @param pH probability of mutation for each rule
     */
	public void heuristic(Double pH) {
        
        int ruleIndex = randInt(parserRules.size());
        Rule mainToChange = parserRules.get(ruleIndex);
        int toChangeIndex = randInt(mainToChange.getTotalProductions());
        
        if(toChangeIndex == 0)  {
            if(!mainToChange.containsEpsilon()) {
                mainToChange.addAlternative(Rule.EPSILON);
                cleanReferences(mainToChange);
            } else {
                mainToChange.removeEpsilon();
            }
            return;
        }
        Rule toChange = mainToChange.getProduction(toChangeIndex);
        double choice = Math.random();
        if(!toChange.nullable(parserRules)) {
            if(choice < 0.33) {    
                toChange.setIterative(!toChange.isIterative());
            } else if(choice < 0.66) {
                toChange.setOptional(!toChange.isOptional());
            } else {
                toChange.setOptional(!toChange.isOptional());
                toChange.setIterative(!toChange.isIterative());
            }
        }
    }
    // Goes through all rules and removes optional flags refrencing this rule after it is made nullable
    private void cleanReferences(Rule toClean) {
        System.out.println("Cleaning refrences to " + toClean);
        parserRules.forEach(rule -> {
            System.out.println("Checking in " + rule.getName() + " :" + rule.getRuleText());
            if(rule.getRuleText().contains(toClean.getName())) {
                rule.cleanReferences(toClean);
            }
        });
    }

    public void flagForRemoval() {
        this.remove = true;
    }

    public boolean toRemove() {
        return remove;
    }

    
	public void injectEOF() {
        Rule wrapperRule = new Rule("program", getStartSymbol() + " EOF;");
        parserRules.add(0, wrapperRule);
    }
    
    public void stripEOF() {
        parserRules.remove(0);
    }

    /**
     * Selects a random rule and applies the grouping mutation to it
     */
    public void groupProductions() {
        if(!canGroup()) return;

        Rule ruleToGroup = parserRules.get(randInt(parserRules.size()));
        while(!ruleToGroup.canGroup()) {
            ruleToGroup = parserRules.get(randInt(parserRules.size()));
        }
        ruleToGroup.groupProductions();
    }

    private boolean canGroup() {
        for(Rule rule : parserRules) {
            if(rule.canGroup()) return true;
        }
        return false;
    }

    /**
     * Selects a random rule that contains grouped symbols and performs the ungroup mutation on it 
     */
    public void unGroupProductions() {
        if(!canUngroup()) return;

        Rule ruleToUnGroup = parserRules.get(randInt(parserRules.size()));
        
        while(!ruleToUnGroup.canUngroup()) {
            ruleToUnGroup = parserRules.get(randInt(parserRules.size()));
        }
        ruleToUnGroup.unGroupProductions();
    }


    private boolean canUngroup() {
        for(Rule rule : parserRules) {
            if(rule.getRuleText().contains("(")) return true;
        }
        return false;
    }

    /**
     * Returns a list of rules that are referenced in the grammar but that are undefined
     */
    public ArrayList<String> getUndefinedRules() {
        ArrayList<String> undefined = parserRules.get(0).getReachables(parserRules);
        undefined.removeIf(name -> !name.contains("Undefined "));
        ArrayList<String> out = new ArrayList<String>();
        undefined.forEach(rawText -> out.add(rawText.replace("Undefined ","")));
        return out;
    }

    /**
     * Introduces a new non-terminal for each undefined reference in the grammar
     */
	public void fixUndefinedRules() {
        ArrayList<String> undefined = getUndefinedRules();
        if(undefined.size() == 0) { 
            // System.out.println(getName() + " contains no undefined rules");
        } else {
            // System.out.println("Repairing " + getName() + " with undefined rules " + undefined);
            undefined.forEach(undefText -> {
                String ruleName = undefText.replace("Undefined ", "");
                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);
                generateNewRule(ruleName, currRuleLen);
            });
        }


    }
    
    /**
     * Removes left recursion by rewriting as an equivalent grammar
     * TODO add removing of recursive derivations
     */
    public void removeLR() {
        if(containsLeftRecursive()) {
            //Replaces repeating left recursive symbols with a single instance and removes it
            // term: term factor | term term; -> term: term factor;
            parserRules.forEach(rule -> {
                rule.simplifyRepeatingLR();
                rule.removeSimpleLeftRecursives();
            });
            
            removeDirectLeftRecursion();
        }

    }
    /**
     * Rewrites left recursive rules as equivalent rules by introducing new rules
     */
    private void removeDirectLeftRecursion() {  
        ArrayList<Rule> rulesToAdd = new ArrayList<Rule>();
        parserRules.forEach(rule -> {
            if(rule.containLeftRecursiveProd()) {
                String ruleName = rule.getName();
                String newRuleName = Rule.genName();
                ArrayList<LinkedList<Rule>> cleanProductions = new ArrayList<LinkedList<Rule>>();
                ArrayList<LinkedList<Rule>> dirtyProductions = new ArrayList<LinkedList<Rule>>();
                rule.getSubRules().forEach(subRule -> {
                    
                    if(subRule.getFirst().equals(Rule.EPSILON)) return;

                    if(!subRule.getFirst().getName().split(" ")[0].equals(ruleName)) {
                        cleanProductions.add(subRule);
                    } else {
                        LinkedList<Rule> toAdd = new LinkedList<Rule>(subRule.subList(1, subRule.size()));
                        Rule ruleToAdd = new Rule(newRuleName,1);
                        toAdd.add(ruleToAdd);
                        dirtyProductions.add(toAdd);
                    }
                });

                StringBuilder newRuleText = new StringBuilder();
                dirtyProductions.forEach(subProduction -> {
                    subProduction.forEach(subRule -> {
                        newRuleText.append(subRule + " ");
                    });
                    newRuleText.append("| ");
                });
                newRuleText.append(";");
                
                //Create the rule, if all the productions were dirty, reuse the same rule name so replace all occurences of newRuleName with original name
                //The productions of the dirtyRule is later copied into the original rule
                Rule dirtyRule;
                if(cleanProductions.size() == 0) {
                    dirtyRule = new Rule(newRuleName,newRuleText.toString().replaceAll(newRuleName, rule.getName()));
                } else {
                    dirtyRule = new Rule(newRuleName,newRuleText.toString());
                }
                
                //If all of the orignal rules were dirty reuse the ruleName for the modified rule other wise introduce a new rule
                rule.getSubRules().clear();
                if  (cleanProductions.size() == 0) {
                    rule.setSubRules(dirtyRule.getSubRules());
                } else {
                    rule.setSubRules(cleanProductions);
                    rulesToAdd.add(dirtyRule);
                }
            }
        });
        parserRules.addAll(rulesToAdd);
    }

    /**
     * Returns true if this contains a parserRule that is directly left recursive term: term factor;
     */
    public boolean containsLeftRecursive() {
        for (int i = 0; i < parserRules.size(); i++) {
            if(parserRules.get(i).containLeftRecursiveProd()) return true;
        }
        return false;
    }
    /**
     * Remove duplicate productions in the rule, rule: prodA | prodA; -> rule: prodA;
     * Should be called after every change to parserRules, mutate, extend, remove left 
     */
    public void removeDuplicateProductions() {
        parserRules.forEach(rule -> Chelsea.removeDuplicates(rule.getSubRules()));
    }

    public void incAge() {
        if(++age > Constants.MAX_GRAMMAR_AGE) flagForRemoval();
    }

    public void resetAge() {
        age = 0;
    }

    public void setName(String name) {
        this.grammarName = name;
    }

    //Removes a random rule and fixed any refrences that become undefined
    public void removeRule() {
        if(parserRules.size() == 0) return;
        int ruleIndex = randInt(parserRules.size());
        parserRules.remove(ruleIndex);
        fixUndefinedRules();
    }

    //Adds a new randomm rule to the grammar, this new rule becomes the start symbol so no new unreachables can be introduced
    public void addNewRule() {
        if(parserRules.size() == Constants.MAX_PROD_SIZE) return;
        generateNewRule(1 + randInt(Constants.MAX_RHS_SIZE-1));
    }

    /**
     * Adds a random symbol to the RHS of a parser rule
     */
    public void incRuleSize() {
        ArrayList<Rule> allRules = getAllRules();
        Rule toInc = parserRules.get(randInt(parserRules.size()));
        Rule toAdd = new Rule(allRules.get(randInt(allRules.size())));
        toInc.extend(toAdd);
    }




    public LinkedList<GrammarReader> computeMutants() {

        
    }

}