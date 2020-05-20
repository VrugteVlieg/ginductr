package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
Mutation list
add rule
remove rule
add symbol to rule RHS
remove symbol from rule RHS
group symbols
ungroup symbols
mutate symbol in rule
heuristic symbol in rule


*/

public class GrammarReader {

    private File grammarFile;
    private List<String> fileLines = new LinkedList<String>();
    private String grammarName;
    private ArrayList<Rule> parserRules = new ArrayList<Rule>();
    private ArrayList<Rule> terminalRules;
    private double posScore = 0.0;
    private double negScore = 0.0;
    private boolean remove = false;
    private int age = 0;

    /**
     * Constructs a grammar from a given file
     * 
     * @param filePath
     */
    public GrammarReader(File sourceFile) {
        grammarFile = sourceFile;
        grammarName = grammarFile.getName().split("\\.")[0];
        try (BufferedReader in = new BufferedReader(new FileReader(grammarFile));) {
            String input;
            in.readLine();
            while ((input = in.readLine()) != null) {
                fileLines.add(input);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.terminalRules = new ArrayList<Rule>();
        readRules();
    }

    public GrammarReader(String grammarText) {
        System.err.println("Constructing grammar from\n" + grammarText);
        fileLines  = Arrays.asList(grammarText.split("\n"));
        grammarName = fileLines.get(0).split(" ")[1];
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
        this.posScore = toCopy.posScore;
        this.negScore = toCopy.negScore;
    }

    /**
     * Constructs a grammar with a given set of terminal rules
     * 
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
        return new ArrayList<Rule>(
                Stream.of(parserRules, terminalRules).flatMap(x -> x.stream()).collect(Collectors.toList()));
    }

    /**
     * Reads a grammar from the ANTLR file associated with this
     */
    private void readRules() {
        fileLines.forEach(input -> {
            input = input.replaceAll("[ ]+", " ").trim();
            String RuleName = input.substring(0, input.indexOf(":")).trim();
            String RuleString = input.substring(input.indexOf(":") + 1); // cuts off the ; at the end
            Rule currRule = new Rule(RuleName, RuleString);
            // currRule.addRule(RuleString);
            if (currRule.isTerminal()) {
                terminalRules.add(currRule);
            } else {
                parserRules.add(currRule);
            }
        });
    }

    public String getStartSymbol() {
        return parserRules.get(0).getName();
    }

    // Calculates the ANTLR grammar form of this
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

    public String getName() {
        return grammarName;
    }

    ArrayList<Rule> getParserRules() {
        return parserRules;
    }

    public void setTerminalRules(ArrayList<Rule> newRules) {
        this.terminalRules = newRules;
    }

    /**
     * Generates a new rule and sets it as the start symbol, this rule can
     * references any rule
     * 
     * @param ruleName
     * @param RHSLen
     */
    public void generateNewRule(String ruleName, int RHSLen) {
        if (RHSLen == 0)
            return;

        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {
            int newRulesIndex = ThreadLocalRandom.current().nextInt(allRules.size());
            newRuleText.append(allRules.get(newRulesIndex).getName() + " ");
        }
        newRuleText.append(";");
        
        Rule toAdd = new Rule(ruleName, newRuleText.toString());
        parserRules.add(0, toAdd);
    }

    public Rule generateReturnNewRule(String ruleName, int RHSLen) {
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {
            int newRulesIndex = ThreadLocalRandom.current().nextInt(allRules.size());
            newRuleText.append(allRules.get(newRulesIndex).getName() + " ");
        }
        newRuleText.append(";");
        
        Rule toAdd = new Rule(ruleName, newRuleText.toString());
        return toAdd;
    }

    public Rule generateReturnNewRule(int RHSLen) {
        String ruleName = Rule.genName();
        while(ruleName.equals("program")) ruleName = Rule.genName();
        while(parserRules.stream().map(Rule::getName).map(ruleName::equals).reduce(false, (sub,next) -> sub || next)) ruleName = Rule.genName();
        return generateReturnNewRule(ruleName, RHSLen);
    }




    /**
     * Wrapper for generateNewRule when a random name can be used
     */
    public void generateNewRule(int RHSLen) {
        String ruleName = Rule.genName();
        while(ruleName.equals("program")) ruleName = Rule.genName();
        while(parserRules.stream().map(Rule::getName).map(ruleName::equals).reduce(false, (sub,next) -> sub || next)) ruleName = Rule.genName();
        generateNewRule(ruleName, RHSLen);
    }

    
    /**
     * Selects a random parserRule A and sets a random symbol B in A to be another
     * rule C in the grammar this can include selecting the LHS as the thing to be
     * mutated, in this case A has its RHS added as alternative to C and A is
     * removed All references to A are then made references to C
     */
    public void mutate() {
        if(parserRules.size() == 0) return;
        int ruleIndex = randInt(parserRules.size());
        Rule toMutate = parserRules.get(ruleIndex);
        int productionIndex = randInt(toMutate.getTotalProductions());

        if(parserRules.size() == 1 && productionIndex == 0) productionIndex = 1 + randInt(toMutate.getTotalProductions()-1);
        if (productionIndex == 0) { // this symbol is now becoming an alternative to another rule
            int newRuleIndex = randInt(parserRules.size());
            while (newRuleIndex == ruleIndex) newRuleIndex = randInt(parserRules.size());

            Rule ruleToExtend = parserRules.get(newRuleIndex);
            ruleToExtend.addAlternative(toMutate.getSubRules());

            boolean toExtendNullable = nullable(ruleToExtend.getName());
            parserRules.forEach(rule -> rule.replaceReferences(toMutate, ruleToExtend, toExtendNullable));
            parserRules.remove(toMutate);
        } else {
            int toInsertIndex = randInt(parserRules.size());
            Rule toInsert = parserRules.get(toInsertIndex);
            toMutate.setProduction(productionIndex, toInsert);
            if(containsLeftRecursive()) removeLR();
        }

    }

    public LinkedList<Rule> getCrossoverRuleList() {
        LinkedList<Rule> out = new LinkedList<Rule>();
        int baseIndex = randInt(parserRules.size());
        Rule baseRule = parserRules.get(baseIndex);
        ArrayList<String> reachables = baseRule.getReachables(parserRules);
        out.addAll(parserRules.stream()
                   .filter(rule -> reachables.contains(rule.getName()))
                   .collect(Collectors.toList()));
        return out;
    }


    public void applyCrossover(LinkedList<Rule> toInsert, Rule toReplace, boolean toInsertNullable) {
        parserRules.addAll(toInsert);
        parserRules.remove(toReplace);
        parserRules.forEach(rule -> rule.replaceReferences(toReplace, toInsert.getFirst().makeMinorCopy(), toInsertNullable));
    }

    public void applyCrossover(GrammarReader toCrossover, outputLambda grammarOut) {
        LinkedList<Rule> otherList = toCrossover.getCrossoverRuleList();
        LinkedList<Rule> myList = getCrossoverRuleList();
        grammarOut.output("Swapping " + myList.stream().map(Rule::getName).collect(Collectors.joining(", ")) + " with " 
        + otherList.stream().map(Rule::getName).collect(Collectors.joining(", ")));
        boolean toInsNull = toCrossover.nullable(otherList.getFirst().getName());
        boolean toSendNull = nullable(myList.getFirst().getName());
        toCrossover.applyCrossover(myList, otherList.getFirst(), toInsNull);
        applyCrossover(otherList, myList.getFirst(), toSendNull);
    }

    public int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
    public void setPosScore(double newScore) {
        posScore = newScore;
    }

    public void setNegScore(double newScore) {
        negScore = newScore;
    }

    public double getScore() {
        return (posScore+negScore)/2.0;
    }
    public double getPosScore() {
        return posScore;
    }
    public double getNegScore() {
        return negScore;
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
        if((sizePre - sizePost) > 0) {
            int numRulesToGen = randInt(sizePre - sizePost);
            for (int i = 0; i < numRulesToGen; i++) {
                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE - 1);
                generateNewRule(currRuleLen);
            }
            
            if (numRulesToGen != 0)
                removeUnreachable();
        }
    }

    /**
     * Applies the heuristic mutation, this can change a production to either
     * optional, iterative , both, or none A ruleis not changed if it is nullable
     * if the LHS is selected, it is made nullable if it is not or made mandatory if it is nullable
     */
    public void heuristic() {
        if(parserRules.size() == 0) return;
        int ruleIndex = randInt(parserRules.size());
        Rule mainToChange = parserRules.get(ruleIndex);
        int toChangeIndex = randInt(mainToChange.getTotalProductions());

        if (toChangeIndex == 0) {
            if (!mainToChange.containsEpsilon()) {
                mainToChange.addAlternative(Rule.EPSILON());
                cleanReferences(mainToChange);
            } else {
                mainToChange.removeEpsilon();
                if(mainToChange.getSubRules().size()  == 0) parserRules.remove(ruleIndex);
            }
            return;
        }
        Rule toChange = mainToChange.getProduction(toChangeIndex);
        double choice = Math.random();
        if (!nullable(toChange.getName())) {
            if (choice < 0.33) {
                toChange.setIterative(!toChange.isIterative());
            } else if (choice < 0.66) {
                toChange.setOptional(!toChange.isOptional());
            } else {
                toChange.setOptional(!toChange.isOptional());
                toChange.setIterative(!toChange.isIterative());
            }
        }
    }


    
    public LinkedList<String> constrNullable() {
        LinkedList<String> nullableNames = new LinkedList<String>();
        for (int i = 0; i < parserRules.size(); i++) {
            if(parserRules.get(i).containsEpsilon()) {
                nullableNames.add(parserRules.get(i).getName());
            }
        }
        int startSize = nullableNames.size();
        do {
            startSize = nullableNames.size();
            parserRules.stream()
            .filter(rule -> !nullableNames.contains(rule.getName()))
            .forEach(rule -> {
                rule.nullable(nullableNames);
            });

        } while(startSize != nullableNames.size());
        return nullableNames;
    }

    public boolean nullable(String targetName) {
        return constrNullable().contains(targetName);
    }

    // Goes through all rules and removes optional flags referencing this rule after
    // it is made nullable
    private void cleanReferences(Rule toClean) {
        parserRules.forEach(rule -> {
            if (rule.getRuleText().contains(toClean.getName())) {
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
        parserRules.removeIf(rule -> rule.getName().equals("program"));
    }

    /**
     * Returns a list of rules that are referenced in the grammar but that are
     * undefined
     */
    public List<String> getUndefinedRules() {
        if(parserRules.size() == 0) return new ArrayList<String>();
        
        List<String> undefined = parserRules.stream()
        .flatMap(rule -> rule.getReachables(parserRules).stream())
        .filter(s -> s.contains("Undefined "))
        .distinct().collect(Collectors.toList());
        if(undefined.size() > 0) System.out.println("Undefined rules in " + this + "\n" + undefined.stream().collect(Collectors.joining(", ")));
        return undefined;
    }

    /**
     * Checks if the grammar contains any undefineRules and marks the grammar for removal if there are
     */
    public void fixUndefinedRules() {
        List<String> undefined = getUndefinedRules();
        if (undefined.size() == 0) {
        } else {
            flagForRemoval();
        }

    }

    /**
     * Removes left recursion by rewriting as an equivalent grammar TODO add
     * removing of recursive derivations
     */
    public void removeLR() {
        if (containsLeftRecursive()) {
            // Replaces repeating left recursive symbols with a single instance and removes
            // it
            // term: term factor | term term; -> term: term factor;
            parserRules.forEach(rule -> {
                rule.simplifyRepeatingLR();
                rule.removeSimpleLeftRecursives();
            });
            while(parserRules.stream().map(rule -> rule.getTotalProductions()==1).reduce(false, (curr, next) -> curr || next)) {
                LinkedList<String> toCleanList = new LinkedList<String>();
                for(Rule rule : parserRules) {

                    if(rule.getTotalProductions() == 1) {
                        toCleanList.add(rule.getName());
                    }
                }
                parserRules.removeIf(rule -> toCleanList.contains(rule.getName()));
                toCleanList.forEach(name -> parserRules.forEach(rule -> rule.removeReferences(name)));
            }
            

            removeDirectLeftRecursion();
        }

    }

    /**
     * Rewrites left recursive rules as equivalent rules by introducing new rules
     */
    private void removeDirectLeftRecursion() {
        ArrayList<Rule> rulesToAdd = new ArrayList<Rule>();
        parserRules.forEach(rule -> {
            if (rule.containLeftRecursiveProd()) {
                String ruleName = rule.getName();
                String newRuleName = Rule.genName();
                ArrayList<LinkedList<Rule>> cleanProductions = new ArrayList<LinkedList<Rule>>();
                ArrayList<LinkedList<Rule>> dirtyProductions = new ArrayList<LinkedList<Rule>>();
                rule.getSubRules().forEach(subRule -> {

                    if (subRule.getFirst().equals(Rule.EPSILON()))
                        return;

                    if (!subRule.getFirst().getName().split(" ")[0].equals(ruleName)) {
                        cleanProductions.add(subRule);
                    } else {
                        LinkedList<Rule> toAdd = new LinkedList<Rule>(subRule.subList(1, subRule.size()));
                        Rule ruleToAdd = new Rule(newRuleName, 1);
                        toAdd.add(ruleToAdd);
                        dirtyProductions.add(toAdd);
                    }
                });
                if(cleanProductions.size() == 0) {
                    flagForRemoval();
                    return;
                }

                StringBuilder newRuleText = new StringBuilder();
                dirtyProductions.forEach(subProduction -> {
                    subProduction.forEach(subRule -> {
                        newRuleText.append(subRule + " ");
                    });
                    newRuleText.append("| ");
                });
                newRuleText.append(";");

                // Create the rule, if all the productions were dirty, reuse the same rule name
                // so replace all occurences of newRuleName with original name
                // The productions of the dirtyRule is later copied into the original rule
                Rule dirtyRule = new Rule(newRuleName, newRuleText.toString());

                // If all of the orignal rules were dirty reuse the ruleName for the modified
                // rule other wise introduce a new rule
                rule.getSubRules().clear();
                
                rule.setSubRules(cleanProductions);
                rulesToAdd.add(dirtyRule);
            }
        });
        parserRules.addAll(rulesToAdd);
    }

    /**
     * Returns true if this contains a parserRule that is directly left recursive
     * term: term factor;
     */
    public boolean containsLeftRecursive() {
        for (int i = 0; i < parserRules.size(); i++) {
            if (parserRules.get(i).containLeftRecursiveProd())
                return true;
        }
        return false;
    }

    /**
     * Remove duplicate productions in the rule, rule: prodA | prodA; -> rule:
     * prodA; Should be called after every change to parserRules, mutate, extend,
     * remove left
     */
    public void removeDuplicateProductions() {
        
        parserRules.forEach(rule -> Chelsea.removeDuplicates(rule.getSubRules()));
    }

    public void incAge() {
        if (++age > Constants.MAX_GRAMMAR_AGE)
            flagForRemoval();
    }

    public void resetAge() {
        age = 0;
    }

    public void setName(String name) {
        this.grammarName = name;
    }

    /**
     * Removes a random rule and fixed any refrences that become undefined
     */
    public void removeRule(int ruleIndex) {
        parserRules.remove(ruleIndex);
        fixUndefinedRules();
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

    /**
     * Generates a name for mutant grammar based on this grammar
     */
    public String genMutantName() {
        StringBuilder mutantNameBuilder = new StringBuilder(grammarName + "_");
        // Generates rule name by randomly concatting letters
        for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
            mutantNameBuilder.append((char) ('a' + randInt(26)));
        }
        return mutantNameBuilder.toString();
    }

    /**
     * Checks which rules can have their symbols counts incremented and which can
     * have their symbols counts decremented first LinkedList is incrementable,
     * second is decrementable incrementable means the current symbol count is <
     * MAX_RHS_SIZE
     */
    private LinkedList<LinkedList<Rule>> canChangeSymbolCount() {
        LinkedList<LinkedList<Rule>> out = new LinkedList<LinkedList<Rule>>();
        LinkedList<Rule> canInc = new LinkedList<Rule>();
        LinkedList<Rule> canDec = new LinkedList<Rule>();
        for (Rule rule : parserRules) {
            int symCount = rule.getSymbolCount();
            if (symCount > 1)
                canDec.add(rule);
            if (symCount < Constants.MAX_RHS_SIZE)
                canInc.add(rule);
        }
        out.add(canInc);
        out.add(canDec);
        return out;
    }

    public void changeSymbolCount() {
        LinkedList<LinkedList<Rule>> changeAbles = canChangeSymbolCount();
        LinkedList<Rule> canInc = changeAbles.getFirst();
        LinkedList<Rule> canDec = changeAbles.getLast();
        if (canInc.size() > 0 && Math.random() < Constants.P_ADD_RULE) {
            Rule toAdd = new Rule(parserRules.get(randInt(parserRules.size())));
            Rule toInc = canInc.get(randInt(canInc.size()));
            // System.out.println("Adding " + toAdd.getName() + " to " + toInc);
            toInc.extend(toAdd);
            // System.out.println("new version " + toInc.getRuleText());
        } else if (canDec.size() > 0) {
            Rule toDec = canDec.get(randInt(canDec.size()));
            // System.out.println("Reducing symbol count of " +  toDec);
            toDec.reduce();
        } 
    }

    /**
     * Checks which rules can have their symbols grouped and which can have their
     * symbols ungrouped first LinkedList is canGroup, second is canUnGroup canGroup
     * means there are 2 or more symbols on the RHS, unGroup means there is at least
     * 1 compound rule on the RHS
     */
    private LinkedList<LinkedList<Rule>> canGroup() {
        LinkedList<LinkedList<Rule>> out = new LinkedList<LinkedList<Rule>>();
        LinkedList<Rule> canGroup = new LinkedList<Rule>();
        LinkedList<Rule> canUngroup = new LinkedList<Rule>();
        for (Rule rule : parserRules) {
            if (rule.canGroup())
                canGroup.add(rule);
            if (rule.canUngroup())
                canUngroup.add(rule);
        }
        out.add(canGroup);
        out.add(canUngroup);
        return out;
    }

    public void groupMutate() {
        LinkedList<LinkedList<Rule>> data = canGroup();
        LinkedList<Rule> groupAbles = data.getFirst();
        LinkedList<Rule> ungroupAbles = data.getLast();
        if (groupAbles.size() > 0 && Math.random() < Constants.P_GROUP) {
            // System.out.println("Grouping in " + getName());
            groupAbles.get(randInt(groupAbles.size())).groupProductions();
        } else if (ungroupAbles.size() > 0) {
            // System.out.println("unGrouping in " + getName());
            ungroupAbles.get(randInt(ungroupAbles.size())).unGroupProductions();
        }
    }

    public void changeRuleCount() {
        if (parserRules.size() < Constants.MAX_RULE_COUNT && Math.random() < Constants.P_ADD_RULE) {
            generateNewRule(1 + randInt(Constants.MAX_RHS_SIZE));
        } else if (parserRules.size() > 0) {
            int ruleIndex = randInt(parserRules.size());
            removeRule(ruleIndex);

        } else if (parserRules.size() < Constants.MAX_RULE_COUNT) {
            generateNewRule(1 + randInt(Constants.MAX_RHS_SIZE));
        }
    }



    public LinkedList<GrammarReader> computeMutants(int numMutants, HashMap<Integer,Boolean> checkedGrammars){
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        for (int i = 0; i < numMutants; i++) {
            try {

            
            GrammarReader toAdd = new GrammarReader(this);
            toAdd.grammarName = genMutantName();
            System.out.println(toAdd.grammarName + " : ");

            // System.out.println("Operating on \n" + toAdd);

                
            if (Constants.CHANGE_RULE_COUNT && Math.random() < Constants.P_CHANGE_RULE_COUNT) {
                // System.out.println("Changing rule count of " + toAdd.getName());
                toAdd.changeRuleCount();
                if(toAdd.toRemove()) {
                    System.out.println("Removing\n" + this +  " from the grammarList");
                    i--;
                    continue;
                }
                // System.out.println(toAdd);
            }

            if (Constants.CHANGE_SYMBOL_COUNT && Math.random() < Constants.P_CHANGE_SYMBOL_COUNT) {
                // System.out.println("Changing symbol count of " + toAdd.getName());
                toAdd.changeSymbolCount();
                if(toAdd.toRemove()) {
                    i--;
                    continue;
                }
                // System.out.println(toAdd);
            }

            if (Constants.GROUP && Math.random() < Constants.P_G) {
                // System.out.println("Grouping in " + toAdd);
                toAdd.groupMutate();
                if(toAdd.toRemove()) {
                    i--;
                    continue;
                }
                // System.out.println("Post grouping " + toAdd);
            }

            if(Constants.MUTATE && Math.random() < Constants.P_M) {
                // System.out.println("Mutating " + toAdd);
                toAdd.mutate();
                if(toAdd.toRemove()) {
                    i--;
                    continue;
                }
                // System.out.println("Post mutation \n" + toAdd);
            }

            if(Constants.HEURISTIC && Math.random() < Constants.P_H) {
                // System.out.println("Heuristic on " + toAdd.getName());
                toAdd.heuristic();
                if(toAdd.toRemove()) {
                    i--;
                    continue;
                }
                // System.out.println(toAdd);
            }
            
            if(checkedGrammars.get(toAdd.hash()) != null) {
                App.hashtableHits++;
                // System.out.println(toAdd + " already generated");
                i--;
            } else {
                checkedGrammars.put(toAdd.hash(),true);
                out.add(toAdd);
            }
            // System.out.println(toAdd);
            } catch(Exception e) {
                System.out.println("Exceptio during mutant calc\n" + this);
                e.printStackTrace(System.out);
            }
        }
        return out;
    }

    
    
    public int hash() {
        
        
        // System.out.println("HashString for " + this + "\n" + finalRules.stream().collect(Collectors.joining("\n")));
        return hashString().hashCode();
    }

    public ArrayList<Rule> getTerminalRules() {
        return terminalRules;
    }

    public String hashString() {
        LinkedList<String[]> mappings = new LinkedList<String[]>();
        for (int i = 0; i < parserRules.size(); i++) {
            String[] toAdd = {parserRules.get(i).getName(), "rule_" + i};
            mappings.add(toAdd);
        }

        LinkedList<String> finalRules = new LinkedList<String>();
        parserRules.forEach(rule -> {
            String ruleText = rule.toString();
            for (String[] mapping : mappings) {
                ruleText = ruleText.replaceAll(mapping[0],mapping[1]);
            }
            finalRules.add(ruleText);
        });
        String out = finalRules.stream().collect(Collectors.joining("\n"));
        return out;

    }

    public String hashRuleName(String rulename) {
        LinkedList<String[]> mappings = new LinkedList<String[]>();
        for (int i = 0; i < parserRules.size(); i++) {
            String[] toAdd = {parserRules.get(i).getName(), "rule_" + i};
            mappings.add(toAdd);
        }

        for (String[] mapping : mappings) {
            if(mapping[0].equals(rulename)) return mapping[1];
        }
        return rulename;

    }

	public boolean containsInfLoop() {
        // System.out.println("Checking if " + this + " contains an infLoop");
        if (getUndefinedRules().stream()
            .map(rule -> rule.contains("Undefined "))
            .reduce(false, (prev, next) -> (prev || next))) {
                return true;
            }
        Rule start  = parserRules.get(0);
        String touchedRules = "";
        if(!start.containsInfLoop(parserRules, touchedRules)) {
            // System.out.println(this + " contains no infLoops");
            return false;
        }  else {
            // System.out.println(this + " contains an infLoop");
            return true;

        }
    }
    
    //GUI DEMO CODE

    public void demoChangeRuleCount(outputLambda logOut, outputLambda grammarOut) {
        if (parserRules.size() < Constants.MAX_RULE_COUNT && Math.random() < Constants.P_ADD_RULE) {
            int ruleLen = 1 + randInt(Constants.MAX_RHS_SIZE);
            logOut.output("Generating new rule of length " + ruleLen);
            generateNewRule("rule_"+parserRules.size(),ruleLen);
            grammarOut.output("New rule " + parserRules.get(0) + "\n");
        } else if (parserRules.size() > 1) {
            int ruleIndex = randInt(parserRules.size());
            logOut.output("Removing " + parserRules.get(ruleIndex).getName() + " from " + grammarName);
            grammarOut.output("Removing " + parserRules.get(ruleIndex).getName() + " from " + grammarName);
            removeRule(ruleIndex);
        } else if (parserRules.size() < Constants.MAX_RULE_COUNT) {
            int ruleLen = 1 + randInt(Constants.MAX_RHS_SIZE);
            logOut.output("Last resort generating new rule of length " + + ruleLen);
            generateNewRule("rule_"+parserRules.size(),ruleLen);
            grammarOut.output("New rule " + parserRules.get(0) + "\n");
        }
    }

    public void demoChangeSymbolCount(outputLambda logOut, outputLambda grammarOut) {
        LinkedList<LinkedList<Rule>> changeAbles = canChangeSymbolCount();
        LinkedList<Rule> canInc = changeAbles.getFirst();
        LinkedList<Rule> canDec = changeAbles.getLast();
        if (canInc.size() > 0 && Math.random() < Constants.P_ADD_RULE) {
            Rule toAdd = new Rule(parserRules.get(randInt(parserRules.size())));
            Rule toInc = canInc.get(randInt(canInc.size()));
            grammarOut.output("Adding " + toAdd.getName() + " to " + toInc);
            logOut.output("Adding " + toAdd.getName() + " to " + toInc.getName());
            toInc.extend(toAdd);
            grammarOut.output("New rule " + toInc);
        } else if (canDec.size() > 0) {
            Rule toDec = canDec.get(randInt(canDec.size()));
            grammarOut.output("Reducing symbol count of " +  toDec);
            logOut.output("Reducing symbol count of " +  toDec.getName());
            toDec.reduce();
            grammarOut.output("New rule " +  toDec);
        }  else if(canInc.size() > 0) {
            Rule toAdd = new Rule(parserRules.get(randInt(parserRules.size())));
            Rule toInc = canInc.get(randInt(canInc.size()));
            grammarOut.output("Adding " + toAdd.getName() + " to " + toInc);
            logOut.output("Adding " + toAdd.getName() + " to " + toInc.getName());
            toInc.extend(toAdd);
            grammarOut.output("New rule " + toInc);
        }
    }

    public void demoGroupMutate(outputLambda logOutput, outputLambda grammarOutput) {
        LinkedList<LinkedList<Rule>> data = canGroup();
        LinkedList<Rule> groupAbles = data.getFirst();
        LinkedList<Rule> ungroupAbles = data.getLast();
        if (groupAbles.size() > 0 && Math.random() < Constants.P_GROUP) {
            Rule toGroup = groupAbles.get(randInt(groupAbles.size()));
            logOutput.output("Grouping in " + toGroup.getName());
            grammarOutput.output("Applying group mutation to " + toGroup);
            toGroup.groupProductions();
            grammarOutput.output("New rule " + toGroup);

        } else if (ungroupAbles.size() > 0) {
            Rule toUnGroup  = ungroupAbles.get(randInt(ungroupAbles.size()));
            logOutput.output("Ungrouping in " + toUnGroup.getName());
            grammarOutput.output("Undoing grouping in " + toUnGroup);
            toUnGroup.unGroupProductions();
            grammarOutput.output("New rule " + toUnGroup);
        } else if(groupAbles.size() > 0) {
            Rule toGroup = groupAbles.get(randInt(groupAbles.size()));
            logOutput.output("Grouping in " + toGroup.getName());
            grammarOutput.output("Applying group mutation to " + toGroup);
            toGroup.groupProductions();
            grammarOutput.output("New rule " + toGroup);
        }
    }

    /**
     * Selects a random parserRule A and sets a random symbol B in A to be another
     * rule C in the grammar this can include selecting the LHS as the thing to be
     * mutated, in this case A has its RHS added as alternative to C and A is
     * removed All references to A are then made references to C
     */
    public void demoMutate(outputLambda logOutput, outputLambda grammarOutput) {
        if(parserRules.size() == 0) return;
        int ruleIndex = randInt(parserRules.size());
        Rule toMutate = parserRules.get(ruleIndex);
        int productionIndex = randInt(toMutate.getTotalProductions());
        if(parserRules.size() == 1 && productionIndex == 0) productionIndex = 1 + randInt(toMutate.getTotalProductions()-1);
        if (productionIndex == 0) { // this symbol is now becoming an alternative to another rule
            int newRuleIndex = randInt(parserRules.size());
            while (newRuleIndex == ruleIndex) newRuleIndex = randInt(parserRules.size());
            Rule ruleToExtend = parserRules.get(newRuleIndex);
            logOutput.output("Making " + toMutate.getName() + " an alternative to " + ruleToExtend.getName());
            grammarOutput.output("Adding " + toMutate.getName() + " as an alternative in " + ruleToExtend);
            ruleToExtend.addAlternative(toMutate.getSubRules());
            Chelsea.removeDuplicates(ruleToExtend.getSubRules());
            boolean toExtendNullable = nullable(ruleToExtend.getName());
            parserRules.forEach(rule -> rule.replaceReferences(toMutate, ruleToExtend, toExtendNullable));
            grammarOutput.output("New rule + " + ruleToExtend);
            parserRules.remove(toMutate);
        } else {

            int toInsertIndex = randInt(parserRules.size());
            Rule toInsert = parserRules.get(toInsertIndex);
            logOutput.output("Mutating " + toMutate.getProduction(productionIndex) + " in " + toMutate.getName());
            grammarOutput.output("Setting " + toMutate.getProduction(productionIndex) + " in " + toMutate + " to " + toInsert.getName());
            toMutate.setProduction(productionIndex, toInsert);
            grammarOutput.output("New rule " + toMutate);
            if(containsLeftRecursive()) removeLR();
            if(toRemove())  logOutput.output(grammarName + " is an invalid grammar");
        }

    }

    public void demoHeuristic(outputLambda logOutput, outputLambda grammarOutput) {
        if(parserRules.size() == 0) {
            logOutput.output(grammarName + " has no parser rules, cannot apply heuristic");
            return;
        }
        int ruleIndex = randInt(parserRules.size());
        Rule mainToChange = parserRules.get(ruleIndex);
        int toChangeIndex = randInt(mainToChange.getTotalProductions());
        logOutput.output("Applying heuristic to " + mainToChange.getName());
        if (toChangeIndex == 0) {
            if (!mainToChange.containsEpsilon()) {
                grammarOutput.output("Adding EPSILON as an alternative production in " + mainToChange.getName());
                mainToChange.addAlternative(Rule.EPSILON());
                grammarOutput.output("New rule " + mainToChange);
                cleanReferences(mainToChange);
            } else {
                grammarOutput.output("Removing EPSILON as an alternative production in " + mainToChange.getName());
                mainToChange.removeEpsilon();
                grammarOutput.output("New rule " + mainToChange);
                if(mainToChange.getSubRules().size()  == 0) parserRules.remove(ruleIndex);
            }
            return;
        }
        Rule toChange = mainToChange.getProduction(toChangeIndex);

        double choice = Math.random();
        logOutput.output(toChange.getName() + "  " + nullable(toChange.getName()));
        if (!nullable(toChange.getName())) {
            if (choice < 0.33) {
                grammarOutput.output("Making " + toChange.getName() + " in " +  mainToChange.getName() + " iterative");
                toChange.setIterative(!toChange.isIterative());
            } else if (choice < 0.66) {
                grammarOutput.output("Making " + toChange.getName() + " in " +  mainToChange.getName() + " optional");
                toChange.setOptional(!toChange.isOptional());
            } else {
                grammarOutput.output("Making " + toChange.getName() + " in " +  mainToChange.getName() + " optional and iterative");
                toChange.setOptional(!toChange.isOptional());
                toChange.setIterative(!toChange.isIterative());
            }
            grammarOutput.output("New rule " + mainToChange);
        }
    }

}