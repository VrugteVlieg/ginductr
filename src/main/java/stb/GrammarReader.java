package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;
import java.util.stream.Stream;

public class GrammarReader {

    private File grammarFile;
    private List<String> fileLines = new LinkedList<String>();
    private String grammarName;
    private ArrayList<Rule> parserRules = new ArrayList<Rule>();
    private ArrayList<Rule> terminalRules;
    private List<String[]> mutationConsiderations = new LinkedList<String[]>();
    private double posScore = 0.0;
    private double negScore = 0.0;
    int truePositivesPos = 0;
    int truePositivesNeg = 0;
    int falsePositives = 0;
    int falseNegatives = 0;
    private boolean remove = false;
    

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
        fileLines = Arrays.asList(grammarText.split("\n"));
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
        toCopy.mutationConsiderations.forEach(mutationConsiderations::add);

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
        return Stream.of(parserRules, terminalRules)
                .flatMap(ArrayList::stream)
                .collect(toCollection(ArrayList::new));
    }

    /**
     * Reads a grammar from the ANTLR file associated with this
     */
    private void readRules() {
        fileLines.forEach(input -> {
            input = input.replaceAll("[ ]+", " ").trim();
            System.out.println(input);
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
        out.append("WS  : [ \\n\\r\\t]+ -> skip;");
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
    //TODO add/find abstraction to apply mutation to arbitrary List<Rule>, can be used to improve rule generation exploration
    //TODO: Add variable number of production alternatives
    /**
     * Generates a new rule and sets it as the start symbol, this rule can
     * references any rule
     * 
     * @param ruleName
     * @param RHSLen
     */
    public void generateNewRule(String ruleName, int RHSLen) {
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {
            Rule ruleToAppend = randGet(allRules, true);
            newRuleText.append(ruleToAppend.getName() + " ");
        }
        newRuleText.append(";");

        Rule toAdd = new Rule(ruleName, newRuleText.toString());
        parserRules.add(0, toAdd);
    }

    public void generateNewRule(String ruleName, int index, int RHSLen) {
            parserRules.add(index, generateReturnNewRule(genRuleName(), RHSLen));
    }
    


    //Generates and returns a rule to be used when filling in blanks in a grammar file
    public Rule generateReturnNewRule(String ruleName, int RHSLen) {
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {

            newRuleText.append(randGet(allRules, true).getName() + " ");

        }
        newRuleText.append(";");

        Rule toAdd = new Rule(ruleName, newRuleText.toString());
        return toAdd;
    }

    /**
     * Selects a random parserRule A and sets a random symbol B in A to be another
     * rule C in the grammar this can include selecting the LHS as the thing to be
     * mutated, in this case A has its RHS added as alternative to C and A is
     * removed All references to A are then made references to C
     */
    public void mutate() {
        
        //index of rule to mutate
        System.out.println("Getting rule index for " + grammarName + " on parserRules size " + parserRules.size());
        


        Rule toMutate = randGet(parserRules, true);
        int productionIndex = randInt(toMutate.getTotalProductions());

        //if there is only 1 production rule it cannot be made an alternative to another  rule
        if (parserRules.size() == 1 && productionIndex == 0)
            productionIndex = 1 + randInt(toMutate.getTotalProductions() - 1);

        // this symbol is now becoming an alternative to another rule
        if (productionIndex == 0) { 
            

            Rule targetRule = randGet(parserRules, true);
            
            //Ensures we dont select the same rule twice
            while (targetRule.equals(toMutate)) {
                targetRule = randGet(parserRules, true);
            }

            targetRule.addAlternative(toMutate.getSubRules());

            //References  to toMutate now become references to ruleToExtend, if ruleToExtend is nullable then any references to toMutate that have modifiers should have them removed
            boolean toExtendNullable = nullable(targetRule.getName());
            final Rule finalTarget = targetRule;
            parserRules.forEach(rule -> rule.replaceReferences(toMutate, finalTarget, toExtendNullable));
            parserRules.remove(toMutate);
        } else {
            
            Rule toInsert = randGet(parserRules, true);
            toMutate.setProduction(productionIndex, toInsert);
        }
    }

    /**
     * Selects an element to be used for crossover, it along with all of its reachbles rules are deep copied into a list
     * @return
     */
    public LinkedList<Rule> getCrossoverRuleList() {
        LinkedList<Rule> out = new LinkedList<Rule>();
        Rule baseRule = randGet(parserRules, true);
        ArrayList<String> reachables = baseRule.getReachables(parserRules);
        parserRules.stream()
                    .filter(rule -> reachables.contains(rule.getName()))
                    .map(Rule::new)
                    .forEach(out::add);
        return out;
    }

    public void applyCrossover(LinkedList<Rule> toInsert, Rule toReplace, boolean toInsertNullable) {
        parserRules.addAll(toInsert);
        parserRules.remove(toReplace);
        parserRules.forEach(
                rule -> rule.replaceReferences(toReplace, toInsert.getFirst().makeMinorCopy(), toInsertNullable));
    }

    public void applyCrossover(GrammarReader toCrossover, outputLambda grammarOut) {
        LinkedList<Rule> otherList = toCrossover.getCrossoverRuleList();
        LinkedList<Rule> myList = getCrossoverRuleList();
        Rule toSend = myList.getFirst();
        Rule toRecv = otherList.getFirst();
        // grammarOut.output("Swapping " +
        // myList.stream().map(Rule::getName).collect(Collectors.joining(", ")) + " with
        // "
        // + otherList.stream().map(Rule::getName).collect(Collectors.joining(", ")));
        boolean toInsNull = toCrossover.nullable(toRecv.getName());
        boolean toSendNull = nullable(toSend.getName());
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
        return (posScore + negScore) / 2.0;
    }

    public double getPosScore() {
        return posScore;
    }

    public double getNegScore() {
        return negScore;
    }

    /**
     * calculates which r
     */
    public void removeUnreachable() {
        Rule startSymbol = parserRules.get(0);
        ArrayList<String> reachableRules = startSymbol.getReachables(parserRules);
        List<String> unreachableRules = parserRules.stream()
                                                    .map(Rule::getName)
                                                    .filter(rule -> !reachableRules.contains(rule))
                                                    .collect(toList());
        Rule toMakeReach = getRuleByName(randGet(unreachableRules, false));
        Rule toExtend =  getRuleByName(randGet(reachableRules, true));
        int indexToExtend = 1  + randInt(toExtend.getTotalProductions()-1);
        Rule toMakeAlt  = toExtend.getProduction(indexToExtend);
        toMakeReach.addAlternative(toMakeAlt);
        toExtend.setProduction(indexToExtend, toMakeReach);
    }

    /**
     * Applies the heuristic mutation, this can change a production to either
     * optional, iterative , both, or none A ruleis not changed if it is nullable if
     * the LHS is selected, it is made nullable if it is not or made mandatory if it
     * is nullable
     */
    public void heuristic() {
        if (parserRules.size() == 0)
            return;
        Rule mainToChange = randGet(parserRules, true);
        int toChangeIndex = randInt(mainToChange.getTotalProductions());

        if (toChangeIndex == 0) {
            if (!mainToChange.containsEpsilon()) {
                mainToChange.addEpsilon();
                cleanReferences(mainToChange);
            } else {
                mainToChange.removeEpsilon();
                //If  a rule was only epsilon remove it from parserRules
                if (mainToChange.getSubRules().size() == 0)
                    parserRules.remove(mainToChange);
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
    /**
     * Constructs a list of nullable rules as per compiler slides
     * @return
     */
    public LinkedList<String> constrNullable() {
        LinkedList<String> nullableNames = new LinkedList<String>();
        for (int i = 0; i < parserRules.size(); i++) {
            if (parserRules.get(i).containsEpsilon()) {
                nullableNames.add(parserRules.get(i).getName());
            }
        }
        int startSize = nullableNames.size();
        do {
            startSize = nullableNames.size();
            parserRules.stream().filter(rule -> !nullableNames.contains(rule.getName())).forEach(rule -> {
                rule.nullable(nullableNames);
            });

        } while (startSize != nullableNames.size());
        return nullableNames;
    }
    /**
     * Computes all nullable rules in this grammar and checks if the targetName is in the list
     */
    public boolean nullable(String targetName) {
        return constrNullable().contains(targetName);
    }

    /**
     * Goes through all rules and removes modifiers of rules containing toClean
     * Used after toClean is made nullable
     * @param toClean
     */
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
        if (parserRules.size() == 0)
            return new ArrayList<String>();

        List<String> undefined = parserRules.stream()
                                            .map(rule -> rule.getReachables(parserRules))
                                            .flatMap(ArrayList::stream)
                                            .filter(s -> s.contains("Undefined "))
                                            .distinct()
                                            .collect(toList());
        return undefined;
    }

    /**
     * Checks if the grammar contains any undefineRules and marks the grammar for
     * removal if there are
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
            while (parserRules.stream().map(rule -> rule.getTotalProductions() == 1).reduce(false,
                    (curr, next) -> curr || next)) {
                LinkedList<String> toCleanList = new LinkedList<String>();
                for (Rule rule : parserRules) {

                    if (rule.getTotalProductions() == 1) {
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
                        Rule ruleToAdd = new Rule(newRuleName);
                        toAdd.add(ruleToAdd);
                        dirtyProductions.add(toAdd);
                    }
                });
                if (cleanProductions.size() == 0) {
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
        Rule toInc = randGet(parserRules, true);
        Rule toAdd = new Rule(randGet(allRules, true));
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

    /**
     * Computes which rules can have their RHS incremented and which can be decremented
     * a rule is then randomly selected and either decremented or incremented
     */
    public void changeSymbolCount() {
        LinkedList<LinkedList<Rule>> changeAbles = canChangeSymbolCount();
        LinkedList<Rule> canInc = changeAbles.getFirst();
        LinkedList<Rule> canDec = changeAbles.getLast();

        if (canInc.size() > 0 && Math.random() < Constants.P_ADD_RULE) {
            Rule toAdd = new Rule(randGet(parserRules, true));
            Rule toInc = randGet(canInc, true);
            // System.out.println("Adding " + toAdd.getName() + " to " + toInc);
            toInc.extend(toAdd);
            // System.out.println("new version " + toInc.getRuleText());
        } else if (canDec.size() > 0) {
            Rule toDec = randGet(canDec, true);
            // System.out.println("Reducing symbol count of " + toDec);
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
            randGet(groupAbles, true).groupProductions();
        } else if (ungroupAbles.size() > 0) {
            // System.out.println("unGrouping in " + getName());
            randGet(ungroupAbles, true).unGroupProductions();
        }
    }

    /**
     * Inserts or removes a rule in this grammar, if the grammar is made invalid due to removing a rule it is flagged for removal
     */
    public void changeRuleCount() {
        if (parserRules.size() < Constants.MAX_RULE_COUNT && Math.random() < Constants.P_ADD_RULE) {
            generateNewRule(genRuleName(), 1 + randInt(Constants.MAX_RHS_SIZE));
        } else if (parserRules.size() > 0) {
            int ruleIndex = randInt(parserRules.size());
            removeRule(ruleIndex);

        } else if (parserRules.size() < Constants.MAX_RULE_COUNT) {
            generateNewRule(genRuleName(), 1 + randInt(Constants.MAX_RHS_SIZE));
        }
    }

    public LinkedList<GrammarReader> computeMutants(int numMutants, HashSet<String> checkedGrammars) {
        // System.err.println(getName() + "   "+ mutationConsiderations.stream().map(Arrays::toString).collect(Collectors.joining(",")));
        if (!mutationConsiderations.isEmpty()) {
            return computeSuggestedMutants(checkedGrammars);
        } else {
            System.err.println(this + " has no suggestions");

        }

        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        for (int i = 0; i < numMutants; i++) {

            try {

                GrammarReader toAdd = new GrammarReader(this);
                toAdd.grammarName = genMutantName();
                // System.out.println(toAdd.grammarName + " : ");

                // System.out.println("Operating on \n" + toAdd);

                if (Constants.CHANGE_RULE_COUNT && Math.random() < Constants.P_CHANGE_RULE_COUNT) {
                    // System.out.println("Changing rule count of " + toAdd.getName());
                    toAdd.changeRuleCount();
                    if (toAdd.toRemove()) {
                        // System.out.println("Removing\n" + this + " from the grammarList");
                        i--;
                        continue;
                    }
                    // System.out.println(toAdd);
                }

                if (Constants.CHANGE_SYMBOL_COUNT && Math.random() < Constants.P_CHANGE_SYMBOL_COUNT) {
                    // System.out.println("Changing symbol count of " + toAdd.getName());
                    toAdd.changeSymbolCount();
                    if (toAdd.toRemove()) {
                        i--;
                        continue;
                    }
                    // System.out.println(toAdd);
                }

                if (Constants.GROUP && Math.random() < Constants.P_G) {
                    // System.out.println("Grouping in " + toAdd);
                    toAdd.groupMutate();
                    if (toAdd.toRemove()) {
                        i--;
                        continue;
                    }
                    // System.out.println("Post grouping " + toAdd);
                }

                if (Constants.MUTATE && Math.random() < Constants.calculatePM(posScore, negScore)) {
                    // System.out.println("Mutating " + toAdd);
                    if(toAdd.parserRules.size() > 0) {
                        toAdd.mutate();
                    }
                    if (toAdd.toRemove()) {
                        i--;
                        continue;
                    }
                    // System.out.println("Post mutation \n" + toAdd);
                }

                if (Constants.HEURISTIC && Math.random() < Constants.P_H) {
                    // System.out.println("Heuristic on " + toAdd.getName());
                    toAdd.heuristic();
                    if (toAdd.toRemove()) {
                        i--;
                        continue;
                    }
                    // System.out.println(toAdd);
                }
                
                if (checkedGrammars.contains(toAdd.hashString())) {
                    App.hashtableHits++;
                    // System.out.println(toAdd + " already generated");
                    i--;
                } else {
                    checkedGrammars.add(toAdd.hashString());
                    out.add(toAdd);
                }
                // System.out.println(toAdd);
            } catch (Exception e) {
                System.out.println("Exceptio during mutant calc\n" + this);
                e.printStackTrace(System.out);
                try  {
                    System.in.read();
                } catch(Exception f) {

                }
            }
        }
        return out;
    }


    public ArrayList<Rule> getTerminalRules() {
        return terminalRules;
    }

    public String hashString() {
        LinkedList<String[]> mappings = new LinkedList<String[]>();
        for (int i = 0; i < parserRules.size(); i++) {
            String[] toAdd = { parserRules.get(i).getName(), "rule_" + i };
            mappings.add(toAdd);
        }

        LinkedList<String> finalRules = new LinkedList<String>();
        parserRules.forEach(rule -> {
            String ruleText = rule.toString();
            for (String[] mapping : mappings) {
                ruleText = ruleText.replaceAll(mapping[0], mapping[1]);
            }
            finalRules.add(ruleText);
        });
        String out = finalRules.stream().collect(Collectors.joining("\n"));
        return out;

    }
    /**
     * Computes the hashed name for the given rule name
     * @param rulename
     * @return
     */
    public String hashRuleName(String rulename) {
        LinkedList<String[]> mappings = new LinkedList<String[]>();
        for (int i = 0; i < parserRules.size(); i++) {
            String[] toAdd = { parserRules.get(i).getName(), "rule_" + i };
            mappings.add(toAdd);
        }

        for (String[] mapping : mappings) {
            if (mapping[0].equals(rulename))
                return mapping[1];
        }
        return rulename;

    }

    public boolean containsInfLoop() {
        // System.out.println("Checking if " + this + " contains an infLoop");
        if (getUndefinedRules().stream().anyMatch(rule -> rule.contains("Undefined "))) {
            return true;
        }
        Rule start = parserRules.get(0);
        String touchedRules = "";
        if (!start.containsInfLoop(parserRules, touchedRules)) {
            // System.out.println(this + " contains no infLoops");
            return false;
        } else {
            // System.out.println(this + " contains an infLoop");
            return true;

        }
    }

    // GUI DEMO CODE

    public void demoChangeRuleCount(outputLambda logOut, outputLambda grammarOut) {
        if (parserRules.size() < Constants.MAX_RULE_COUNT && Math.random() < Constants.P_ADD_RULE) {
            int ruleLen = 1 + randInt(Constants.MAX_RHS_SIZE);
            logOut.output("Generating new rule of length " + ruleLen);
            generateNewRule("rule_" + parserRules.size(), ruleLen);
            grammarOut.output("New rule " + parserRules.get(0) + "\n");
        } else if (parserRules.size() > 1) {
            int ruleIndex = randInt(parserRules.size());
            logOut.output("Removing " + parserRules.get(ruleIndex).getName() + " from " + grammarName);
            grammarOut.output("Removing " + parserRules.get(ruleIndex).getName() + " from " + grammarName);
            removeRule(ruleIndex);
        } else if (parserRules.size() < Constants.MAX_RULE_COUNT) {
            int ruleLen = 1 + randInt(Constants.MAX_RHS_SIZE);
            logOut.output("Last resort generating new rule of length " + +ruleLen);
            generateNewRule("rule_" + parserRules.size(), ruleLen);
            grammarOut.output("New rule " + parserRules.get(0) + "\n");
        }
    }

    public void demoChangeSymbolCount(outputLambda logOut, outputLambda grammarOut) {
        LinkedList<LinkedList<Rule>> changeAbles = canChangeSymbolCount();
        LinkedList<Rule> canInc = changeAbles.getFirst();
        LinkedList<Rule> canDec = changeAbles.getLast();
        if (canInc.size() > 0 && Math.random() < Constants.P_ADD_RULE) {
            Rule toAdd = new Rule(randGet(parserRules, true));
            Rule toInc = randGet(canInc, true);
            grammarOut.output("Adding " + toAdd.getName() + " to " + toInc);
            logOut.output("Adding " + toAdd.getName() + " to " + toInc.getName());
            toInc.extend(toAdd);
            grammarOut.output("New rule " + toInc);
        } else if (canDec.size() > 0) {
            Rule toDec = randGet(canDec, true);
            grammarOut.output("Reducing symbol count of " + toDec);
            logOut.output("Reducing symbol count of " + toDec.getName());
            toDec.reduce();
            grammarOut.output("New rule " + toDec);
        } else if (canInc.size() > 0) {
            Rule toAdd = new Rule(randGet(parserRules, true));
            Rule toInc = randGet(canInc, true);
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
            Rule toUnGroup = ungroupAbles.get(randInt(ungroupAbles.size()));
            logOutput.output("Ungrouping in " + toUnGroup.getName());
            grammarOutput.output("Undoing grouping in " + toUnGroup);
            toUnGroup.unGroupProductions();
            grammarOutput.output("New rule " + toUnGroup);
        } else if (groupAbles.size() > 0) {
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
        if (parserRules.size() == 0)
            return;
        int ruleIndex = randInt(parserRules.size());
        Rule toMutate = parserRules.get(ruleIndex);
        int productionIndex = randInt(toMutate.getTotalProductions());
        if (parserRules.size() == 1 && productionIndex == 0)
            productionIndex = 1 + randInt(toMutate.getTotalProductions() - 1);
        if (productionIndex == 0) { // this symbol is now becoming an alternative to another rule
            int newRuleIndex = randInt(parserRules.size());
            while (newRuleIndex == ruleIndex)
                newRuleIndex = randInt(parserRules.size());
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
            grammarOutput.output("Setting " + toMutate.getProduction(productionIndex) + " in " + toMutate + " to "
                    + toInsert.getName());
            toMutate.setProduction(productionIndex, toInsert);
            grammarOutput.output("New rule " + toMutate);
            if (containsLeftRecursive())
                removeLR();
            if (toRemove())
                logOutput.output(grammarName + " is an invalid grammar");
        }

    }

    public void demoHeuristic(outputLambda logOutput, outputLambda grammarOutput) {
        if (parserRules.size() == 0) {
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
                if (mainToChange.getSubRules().size() == 0)
                    parserRules.remove(ruleIndex);
            }
            return;
        }
        Rule toChange = mainToChange.getProduction(toChangeIndex);

        double choice = Math.random();
        logOutput.output(toChange.getName() + "  " + nullable(toChange.getName()));
        if (!nullable(toChange.getName())) {
            if (choice < 0.33) {
                grammarOutput.output("Making " + toChange.getName() + " in " + mainToChange.getName() + " iterative");
                toChange.setIterative(!toChange.isIterative());
            } else if (choice < 0.66) {
                grammarOutput.output("Making " + toChange.getName() + " in " + mainToChange.getName() + " optional");
                toChange.setOptional(!toChange.isOptional());
            } else {
                grammarOutput.output(
                        "Making " + toChange.getName() + " in " + mainToChange.getName() + " optional and iterative");
                toChange.setOptional(!toChange.isOptional());
                toChange.setIterative(!toChange.isIterative());
            }
            grammarOutput.output("New rule " + mainToChange);
        }
    }

    public void setMutationConsideration(List<String> newList) {
        App.runGrammarOutput.output("Setting mutations list for " + getName() + newList + "\n");
        mutationConsiderations = newList.stream().limit(3).map(str -> str.split(":")).collect(Collectors.toList());
    }

    public List<String[]> getMutationConsideration() {
        return mutationConsiderations;
    }

    public Rule getRuleByName(String name) {
        System.err.println("Searching for " + name + " in " + this);

        return parserRules.stream().filter(rule -> rule.getName().equals(name)).findFirst().get();
    }

    public LinkedList<GrammarReader> computeSuggestedMutants(HashSet<String> checkedGrammars) {
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();

        App.runGrammarOutput.output("clear");
        App.runGrammarOutput.output("Computing suggested mutants for " + this + "\n"
                + mutationConsiderations.stream().map(Arrays::toString).collect(Collectors.joining("\n")));
        mutationConsiderations.stream().forEach(strArr -> {
            LinkedList<GrammarReader> currAdditions = new LinkedList<>();
            App.runGrammarOutput.output("\nUsing key " + Arrays.toString(strArr));
            System.err.println(Arrays.toString(strArr));
            System.err.println(strArr[0]);
            System.err.println(strArr[1]);
            String ruleName = strArr[0];
            int prodIndex = Integer.parseInt(strArr[1]) - 1;
            // Randomised new productions
            for (int i = 0; i < 3; i++) {
                GrammarReader grammarToAdd = new GrammarReader(this);
                LinkedList<Rule> newProd = new LinkedList<Rule>();
                int prodLen = randInt(5);
                for (int j = 0; j < prodLen; j++) {
                    // Rule ruleToAdd =
                    // parserRules.get(randInt(parserRules.size())).makeMinorCopy();
                    Rule ruleToAdd = randGet(getAllRules(), true).makeMinorCopy();
                    newProd.add(ruleToAdd);
                }
                App.runGrammarOutput.output("new prod [" + newProd.stream().map(Rule::getName).collect(Collectors.joining("  ")) + "]");
                
                if (prodLen == 0) {
                    grammarToAdd.getRuleByName(ruleName).getSubRules().remove(prodIndex);

                } else {
                    grammarToAdd.getRuleByName(ruleName).getSubRules().set(prodIndex, newProd);
                }
                if (checkedGrammars.contains(grammarToAdd.hashString())) {
                    App.hashtableHits++;
                    // System.out.println(toAdd + " already generated");
                    i--;
                } else {
                    // App.runGrammarOutput.output("clear");
                    // App.runGrammarOutput.output("Alternate grammars generated from \n" + this +
                    // "\n" +
                    // out.stream().map(GrammarReader::toString).collect(Collectors.joining("\n\n")));
                    checkedGrammars.add(grammarToAdd.hashString());
                    currAdditions.add(grammarToAdd);
                }
            }

            // Grouping operation, this should be skipped if the target prod cannot be
            // group/ungrouped when it is only 1 rule
            Rule origRule = getRuleByName(ruleName);
            if (origRule.canGroupProd(prodIndex)) {
                for (int i = 0; i < 3; i++) {
                    GrammarReader grammarToAdd = new GrammarReader(this);
                    grammarToAdd.setName(grammarToAdd.genMutantName());
                    Rule targetRule = grammarToAdd.getRuleByName(ruleName);
                    LinkedList<Rule> prodToGroup = targetRule.getSubRules().get(prodIndex);
                    int startIndex = prodToGroup.size() == 2 ? 0 : randInt(prodToGroup.size() - 2);
                    int endIndex = startIndex + randInt(prodToGroup.size() - startIndex - 1) + 1;
                    List<Rule> toRemove = prodToGroup.subList(startIndex, endIndex + 1);
                    Rule newRule = new Rule(toRemove);
                    for (int j = startIndex; j < endIndex; j++) {
                        prodToGroup.remove(startIndex);
                    }
                    prodToGroup.set(startIndex, newRule);
                    if(!grammarToAdd.nullable(newRule.getName()) && Math.random() < 0.5) {
                        double choice = Math.random();
                        if (choice < 0.33) {
                            newRule.setIterative(!newRule.isIterative());
                        } else if (choice < 0.66) {
                            newRule.setOptional(!newRule.isOptional());
                        } else {
                            newRule.setOptional(!newRule.isOptional());
                            newRule.setIterative(!newRule.isIterative());
                        }
                    }
                    checkedGrammars.add(grammarToAdd.hashString());
                    currAdditions.add(grammarToAdd);
                }
            }
            if (origRule.canUngroupProd(prodIndex)) {
                for (int i = 0; i < 3; i++) {
                    GrammarReader grammarToAdd = new GrammarReader(this);
                    grammarToAdd.setName(grammarToAdd.genMutantName());
                    Rule targetRule = grammarToAdd.getRuleByName(ruleName);
                    List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);
                    List<Integer> ungroupAbleIndices = new LinkedList<>();
                    for (int j = 0; j < targetProd.size(); j++) {
                        if(!targetProd.get(j).isSingular()) ungroupAbleIndices.add(j);
                    }
                    
                    int toUngroupIndex = randGet(ungroupAbleIndices, true);
                    Rule toUngroup = targetProd.get(toUngroupIndex);
                    targetProd.addAll(toUngroupIndex, toUngroup.getSubRules().get(0));

                    currAdditions.add(grammarToAdd);
                }
            }

            // Heuristics
            for (int i = 0; i < 3; i++) {
                GrammarReader grammarToAdd = new GrammarReader(this);
                grammarToAdd.setName(grammarToAdd.genMutantName());
                Rule targetRule = grammarToAdd.getRuleByName(ruleName);
                List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);
                if (targetProd.get(0).equals(Rule.EPSILON)) {
                    targetRule.removeEpsilon();
                } else {
                    int[] prodCounts = new int[targetRule.getSubRules().size()];
                    for (int j = 0; j < prodCounts.length; j++) {
                        prodCounts[j] = targetRule.getTotalProductions(j)
                                + Arrays.stream(prodCounts).limit(Math.max(j - 1,0)).sum();
                    }
                    int lowerProdBound = 1 + (prodIndex == 0 ? 0 : prodCounts[prodIndex - 1]);
                    int upperProdBound = 1 + prodCounts[prodIndex];
                    int range = upperProdBound - lowerProdBound + 1;
                    int targetIndex = randInt(range);
                    if(targetIndex == 0) { //Add EPSILON as an alternative to the rule
                        targetRule.addEpsilon();
                    } else {
                        Rule toChange = targetRule.getProduction(targetIndex);
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
                }
                currAdditions.add(grammarToAdd);
            }


            
            //new non-terminals
            for (int i = 0; i < 3; i++) {
                GrammarReader toAdd = new GrammarReader(this);
                int newRuleIndex = 1 + randInt(parserRules.size()-1);
                toAdd.generateNewRule(genRuleName(), newRuleIndex, randInt(Constants.MAX_RHS_SIZE));
                Rule newRule = toAdd.parserRules.get(newRuleIndex);
                Rule targetRule = toAdd.getRuleByName(ruleName);
                targetRule.setProduction(randInt(targetRule.getTotalProductions()), newRule);
                currAdditions.add(toAdd);
            }

            //modify prods
            for (int i = 0; i < 3; i++) {
                GrammarReader toAdd = new GrammarReader(this);
                Rule targetRule = toAdd.getRuleByName(ruleName);
                List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);
                List<Rule> newProd = targetProd.stream().map(rule -> new Rule(rule)).collect(Collectors.toList());
                int toChangeIndex = randInt(newProd.size());
                List<Rule> allRules = toAdd.getAllRules();
                Rule newAlternative = randGet(allRules, true);
                String newRuleText = newProd.get(toChangeIndex).getName() + " | " + newAlternative.getName() + ";";
                Rule newRule = new Rule(newRuleText);
                toAdd.parserRules.add(newRule);
                newProd.set(toChangeIndex, newRule.makeMinorCopy());
                currAdditions.add(toAdd);
            }

            out.addAll(currAdditions);
            StringBuilder toShow = new StringBuilder("Suggestions for " + Arrays.toString(strArr) + " default: " + origRule +  "\n");
            currAdditions.forEach(gram -> toShow.append(gram.getRuleByName(ruleName).toString() + "\n"));
            App.runGrammarOutput.output(toShow.toString());
            try {
                System.in.read();
            } catch(Exception e) {

            }

        });


        return out;
        
    }

    public String genRuleName() {
        StringBuilder ruleNameBuilder = new StringBuilder();
        //Generates rule name by randomly concatting letters
        while(ruleNameBuilder.length() == 0) {
            for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
                ruleNameBuilder.append((char)('a' + randInt(26)));
            }
            //Check if the generated ruleName matches an existing parserRule, only parserRules are checked as lowercase letters are used exclusively
            boolean duplicateRuleName = parserRules.stream()
                                        .map(Rule::getName)
                                        .anyMatch(ruleNameBuilder.toString()::equals);

            //clears the name builder if it is the reserved word program or a duplicate of an existing ruleName
            if(ruleNameBuilder.toString().equals("program") || duplicateRuleName) ruleNameBuilder.setLength(0);
        }
        return ruleNameBuilder.toString();
    }

    public double COMPUTE_RECALL() {
        double truePos = truePositivesNeg+truePositivesPos;
        return truePos/(truePos + falseNegatives);
    }

    public double COMPUTE_PRECISION() {
        double truePos = truePositivesNeg+truePositivesPos;
        return truePos/(truePos + falsePositives);
    }

    public double COMPUTE_F2() {
        double prec = COMPUTE_PRECISION();
        double recall = COMPUTE_RECALL();
        return (5*prec*recall)/(4*prec+recall);
    }
    /**
     * Attempts to apply the grouping mutation to toGroup
     * If it can not be grouped leaves as is
     * TODO expand this to apply mutation at arbitrary depths ruleA (ruleB ruleC ruleD), you need to be able to group like ruleA (ruleB (ruleC ruleD)) 
     * @param toGroup
     */
    public void applyGrouping(List<Rule> toGroup) {
        if(toGroup.size() < 3) return;
        int startIndex = randInt(toGroup.size()-1);
        int endIndex = randInt(toGroup.size() - startIndex);
        List<Rule> selectedRules = toGroup.subList(startIndex, endIndex);
        Rule newRule = new Rule(selectedRules);
        toGroup.removeAll(selectedRules);
        toGroup.add(startIndex, newRule);
    }

    /**
     * Applied heuristic mutation to a random rule in a list
     * @param toHeur
     */
    public void applyHeuristic(List<Rule> toHeur) {
        Rule toSelect = randGet(toHeur, true);

        while(nullable(toSelect.getName())){
            toSelect = randGet(toHeur, true);
        }    

        double choice = Math.random();
        if(choice < 0.33) {
            toSelect.setIterative(!toSelect.isIterative());
        } else if(choice < 0.66) {
            toSelect.setOptional(!toSelect.isOptional());
        } else {
            toSelect.setIterative(!toSelect.isIterative());
            toSelect.setOptional(!toSelect.isOptional());
        }
    }

    /**
     * samples a random element from a list and returns it
     * @param <E>
     * @param input
     * @param replace sample with replacement or without replacement
     * @return
     */
    public <E> E randGet(List<E> input, boolean replace) {
        if(replace) {
            return input.get(randInt(input.size()));
        } else {
            return input.remove(randInt(input.size()));
        }
    }



}