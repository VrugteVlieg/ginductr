package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;
import java.util.stream.Stream;




public class GrammarReader implements Comparable<GrammarReader> {
    
    public static final String RANDOM_MUTATION = "RANDOM_MUTATION";
    public static final String HEUR_MUTATION = "HEUR_MUTATION";
    public static final String GROUP_MUTATION = "_GROUP_MUTATION";
    public static final String UNGROUP_MUTATION = "UNGROUP_MUTATION";
    public static final String NEWNT_MUTATION = "NEWNT_MUTATION";
    public static final String MODEXSTNEWNT_MUTATION = "MOD_MUTATION";
    
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
     * Creates a deep copy of a grammarReader, does not copy mutation suggestions
     */
    public GrammarReader(GrammarReader toCopy) {
        grammarName = toCopy.grammarName;
        parserRules = new ArrayList<Rule>();
        toCopy.parserRules.forEach(rule -> parserRules.add(new Rule(rule)));
        terminalRules = new ArrayList<Rule>();
        toCopy.terminalRules.forEach(rule -> terminalRules.add(new Rule(rule)));
        
        // toCopy.mutationConsiderations.forEach(mutationConsiderations::add);

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
     * Grammar cannot be made invalid with new rule
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


    /**
     * Generates a new non terminal and sets a random rule in targerProd to the new non-terminal
     * if the non-terminal would have been of length 1 we do not generate a new rule and instead we just set a random rule to another random rule
     * @param targetProd
     */
    public void genNewNT(List<Rule> targetProd) {
        int newRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE-1);
        int numSelectables = getTotalSelectables(targetProd);
        int replacementIndex = randInt(numSelectables);
        Rule toReplace = getSelectable(targetProd, replacementIndex);
        Rule toInsert;

        if(newRuleLen == 1) {
            //if the new rule would have had len 1 it would be of the form newRule: ruleA;
            //Instead of making a new rule just insert ruleA where the newRule would have gone
            List<Rule> allRules = getAllRules();
            
            do {
                toInsert = randGet(allRules, true);
            } while(toInsert.equals(toReplace));

        } else {
            int newRuleIndex = 1 + randInt(parserRules.size()-1);
            generateNewRule(genRuleName(), newRuleIndex, newRuleLen);
            toInsert = parserRules.get(newRuleIndex);
        }

        setSelectable(targetProd, replacementIndex, toInsert);
        setName(getName() + "_NEWNT");
        setName(genMutantName());
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
        int productionIndex = randInt(toMutate.getTotalSelectables());

        //if there is only 1 production rule it cannot be made an alternative to another  rule
        if (parserRules.size() == 1 && productionIndex == 0)
            productionIndex = 1 + randInt(toMutate.getTotalSelectables() - 1);

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
            toMutate.setSelectable(productionIndex, toInsert);
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
        if(bound == 0) return 0;
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
        if(unreachableRules.size() == 0) return;
        while(unreachableRules.size() > 0) {
            Rule toMakeReach = getRuleByName(randGet(unreachableRules, false));
            Rule toExtend =  getRuleByName(randGet(reachableRules, true));
            int indexToExtend = 1  + randInt(toExtend.getTotalSelectables()-1);
            Rule toMakeAlt  = toExtend.getSelectable(indexToExtend);
            toMakeReach.addAlternative(toMakeAlt);
            toExtend.setSelectable(indexToExtend, toMakeReach);
        }
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
        int toChangeIndex = randInt(mainToChange.getTotalSelectables());

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
        Rule toChange = mainToChange.getSelectable(toChangeIndex);
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
        undefined.forEach(undef -> 
            parserRules.forEach(rule -> {
                if(rule.toString().contains(undef)) {
                    rule.removeReferences(undef);
                }
            })
        );
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
            while (parserRules.stream().anyMatch(rule -> rule.getTotalSelectables() == 1)) {
                LinkedList<String> toCleanList = new LinkedList<String>();
                for (Rule rule : parserRules) {

                    if (rule.getTotalSelectables() == 1) {
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
    private LinkedList<List<Rule>> canChangeSymbolCount() {
        LinkedList<List<Rule>> out = new LinkedList<List<Rule>>();
        
        //Can increment
        out.add(parserRules.stream()
            .filter(rule -> rule.getSymbolCount() < Constants.MAX_RHS_SIZE)
            .collect(toList()));

        //Can Decrement
        out.add(parserRules.stream()
            .filter(rule -> rule.getSymbolCount() > 1)
            .collect(toList()));
        
        return out;
    }

    /**
     * Computes which rules can have their RHS incremented and which can be decremented
     * a rule is then randomly selected and either decremented or incremented
     */
    public void changeSymbolCount(List<Rule> targetProd) {
        int numSelectables = getTotalSelectables(targetProd);
        int index = randInt(numSelectables);
        if(Math.random() < Constants.P_ADD_SYMBOL) {
            insertSelectable(targetProd, index, randGet(parserRules, true));
        } else if(targetProd.size() > 1 || index > 0) {
            //If there is only 1 rule in the prod removing it would break the grammar
            //if the index is not 0 however then the rule must be a composite rule and we proceed
            removeSelectable(targetProd, index);

        }       
    }
    
    public void groupMutate(List<Rule> prod) {

        /**
         * this first check makes us favor grouping at higher levels
         * in general this should be a better solution than equally weighting all levels 
         */
        boolean prodSize = prod.size() > 1;
        if(prodSize) {
            int startIndex = randInt(prod.size()-1);
            int endIndex = startIndex + 1 + randInt(prod.size()-startIndex-1);
            List<Rule> toGroup = new LinkedList<>();
            prod.subList(startIndex, endIndex).forEach(toGroup::add);
            Rule toInsert = new Rule(toGroup);
            prod.removeAll(toGroup);
            prod.add(startIndex, toInsert);
        } else {
            Rule soleRule = prod.get(0);
            List<Rule> innerRules = soleRule.getSubRules().get(0);
            groupMutate(innerRules);
        }
    }

    /**
     * Applied ungrouping mutation to some list of rules
     * ungrouping goes to any depth
     * 
     * @param prod
     */
    public void ungroupMutate(List<Rule> prod) {
        int totalSelectables = getTotalSelectables(prod);
        List<Integer> ungroupableIndices = new LinkedList<Integer>();
        for (Integer i = 0; i < totalSelectables; i++) {
            if(!getSelectable(prod, i).isSingular()) {
                ungroupableIndices.add(i);
            }
        }                                   
        int toUngroupIndex = randGet(ungroupableIndices, true);
        List<Rule> targetSubRules = getSelectable(prod, toUngroupIndex).getSubRules().get(0);
        Collections.reverse(targetSubRules);
        targetSubRules.forEach(rule -> insertSelectable(prod, toUngroupIndex, rule));

        

    }

    /**
     * Generates new NT and inserts into targetProd replacing another rule
     * or
     * Removes a rule that 
     */
    public void changeRuleCount(List<Rule> targetProd) {

        if (parserRules.size() < Constants.MAX_RULE_COUNT && Math.random() < Constants.P_ADD_RULE) {
            genNewNT(targetProd);
        } else if (parserRules.size() > 1) {
            int toRemoveIndex = randInt(getTotalSelectables(targetProd));
            Rule toRemove = randGet(parserRules, false);
            parserRules.forEach(rule -> rule.removeReferences(toRemove.getName()));

            removeSelectable(targetProd, toRemoveIndex);
        } else if (parserRules.size() < Constants.MAX_RULE_COUNT) {
            genNewNT(targetProd);
        }

    }

    public LinkedList<GrammarReader> computeMutants() {
        HashSet<String> checkedGrammars = App.generatedGrammars;
        System.err.println(getName() + "   "+ mutationConsiderations.stream().map(Arrays::toString).collect(Collectors.joining(",")));
        if (!mutationConsiderations.isEmpty()) {
            return computeSuggestedMutants(checkedGrammars);
        } else {
            System.err.println(this + " has no suggestions");

        }

        int numMutants = Constants.MUTANTS_PER_BASE;
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();
        for (int i = 0; i < numMutants; i++) {

            try {

                GrammarReader toAdd = new GrammarReader(this);
                toAdd.grammarName = genMutantName();
                // System.out.println(toAdd.grammarName + " : ");

                // System.out.println("Operating on \n" + toAdd);

                if (Constants.CHANGE_RULE_COUNT && Math.random() < Constants.P_CHANGE_RULE_COUNT) {
                    // System.out.println("Changing rule count of " + toAdd.getName());
                    // toAdd.changeRuleCount();
                    
                }

                if (Constants.CHANGE_SYMBOL_COUNT && Math.random() < Constants.P_CHANGE_SYMBOL_COUNT) {
                    // System.out.println("Changing symbol count of " + toAdd.getName());
                    // toAdd.changeSymbolCount();
                }

                if (Constants.GROUP && Math.random() < Constants.P_G) {
                    // System.out.println("Grouping in " + toAdd);
                    // toAdd.groupBMutate();
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
        System.err.println("Checking if " + this + " contains an infLoop");
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
        LinkedList<List<Rule>> changeAbles = canChangeSymbolCount();
        List<Rule> canInc = changeAbles.getFirst();
        List<Rule> canDec = changeAbles.getLast();
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
        // LinkedList<LinkedList<Rule>> data = canGroup(randGet(randGet(parserRules, true).getSubRules(), true));
        // LinkedList<Rule> groupAbles = data.getFirst();
        // LinkedList<Rule> ungroupAbles = data.getLast();
        // if (groupAbles.size() > 0 && Math.random() < Constants.P_GROUP) {
        //     Rule toGroup = groupAbles.get(randInt(groupAbles.size()));
        //     logOutput.output("Grouping in " + toGroup.getName());
        //     grammarOutput.output("Applying group mutation to " + toGroup);
        //     toGroup.groupProductions();
        //     grammarOutput.output("New rule " + toGroup);

        // } else if (ungroupAbles.size() > 0) {
        //     Rule toUnGroup = ungroupAbles.get(randInt(ungroupAbles.size()));
        //     logOutput.output("Ungrouping in " + toUnGroup.getName());
        //     grammarOutput.output("Undoing grouping in " + toUnGroup);
        //     toUnGroup.unGroupProductions();
        //     grammarOutput.output("New rule " + toUnGroup);
        // } else if (groupAbles.size() > 0) {
        //     Rule toGroup = groupAbles.get(randInt(groupAbles.size()));
        //     logOutput.output("Grouping in " + toGroup.getName());
        //     grammarOutput.output("Applying group mutation to " + toGroup);
        //     toGroup.groupProductions();
        //     grammarOutput.output("New rule " + toGroup);
        // }
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
        int productionIndex = randInt(toMutate.getTotalSelectables());
        if (parserRules.size() == 1 && productionIndex == 0)
            productionIndex = 1 + randInt(toMutate.getTotalSelectables() - 1);
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
            logOutput.output("Mutating " + toMutate.getSelectable(productionIndex) + " in " + toMutate.getName());
            grammarOutput.output("Setting " + toMutate.getSelectable(productionIndex) + " in " + toMutate + " to "
                    + toInsert.getName());
            toMutate.setSelectable(productionIndex, toInsert);
            grammarOutput.output("New rule " + toMutate);
            if (containsLeftRecursive())
                removeLR();
            if (toRemove())
                logOutput.output(grammarName + " is an invalid grammar");
        }

    }

    public void demoHeuristic(outputLambda logOutput, outputLambda grammarOutput) {
        // if (parserRules.size() == 0) {
        //     logOutput.output(grammarName + " has no parser rules, cannot apply heuristic");
        //     return;
        // }
        // int ruleIndex = randInt(parserRules.size());
        // Rule mainToChange = parserRules.get(ruleIndex);
        // int toChangeIndex = randInt(mainToChange.getTotalSelectables());
        // logOutput.output("Applying heuristic to " + mainToChange.getName());
        // if (toChangeIndex == 0) {
        //     if (!mainToChange.containsEpsilon()) {
        //         grammarOutput.output("Adding EPSILON as an alternative production in " + mainToChange.getName());
        //         mainToChange.addAlternative(Rule.EPSILON());
        //         grammarOutput.output("New rule " + mainToChange);
        //         cleanReferences(mainToChange);
        //     } else {
        //         grammarOutput.output("Removing EPSILON as an alternative production in " + mainToChange.getName());
        //         mainToChange.removeEpsilon();
        //         grammarOutput.output("New rule " + mainToChange);
        //         if (mainToChange.getSubRules().size() == 0)
        //             parserRules.remove(ruleIndex);
        //     }
        //     return;
        // }
        // Rule toChange = mainToChange.getProduction(toChangeIndex);

        // double choice = Math.random();
        // logOutput.output(toChange.getName() + "  " + nullable(toChange.getName()));
        // if (!nullable(toChange.getName())) {
        //     if (choice < 0.33) {
        //         grammarOutput.output("Making " + toChange.getName() + " in " + mainToChange.getName() + " iterative");
        //         toChange.setIterative(!toChange.isIterative());
        //     } else if (choice < 0.66) {
        //         grammarOutput.output("Making " + toChange.getName() + " in " + mainToChange.getName() + " optional");
        //         toChange.setOptional(!toChange.isOptional());
        //     } else {
        //         grammarOutput.output(
        //                 "Making " + toChange.getName() + " in " + mainToChange.getName() + " optional and iterative");
        //         toChange.setOptional(!toChange.isOptional());
        //         toChange.setIterative(!toChange.isIterative());
        //     }
        //     grammarOutput.output("New rule " + mainToChange);
        // }
    }

    public void setMutationConsideration(List<String> newList) {
        App.runGrammarOutput.output("Setting mutations list for "  + this + "\n" + newList + "\n");
        mutationConsiderations = newList.stream().limit(3).map(str -> str.split(":")).collect(Collectors.toList());
    }

    public List<String[]> getMutationConsideration() {
        return mutationConsiderations;
    }

    public Rule getRuleByName(String name) {
        // System.err.println("Searching for " + name + " in " + this);

        return parserRules.stream().filter(rule -> rule.getName().equals(name)).findFirst().get();
    }

    public LinkedList<GrammarReader> computeSuggestedMutants(HashSet<String> checkedGrammars) {
        LinkedList<GrammarReader> out = new LinkedList<GrammarReader>();

        App.runGrammarOutput.output("clear");
        // App.runGrammarOutput.output("Computing suggested mutants for " + this + "\n"
        //         + mutationConsiderations.stream().map(Arrays::toString).collect(Collectors.joining("\n")));
                
        mutationConsiderations.stream().forEach(strArr -> {
            App.runGrammarOutput.output("\nUsing key " + Arrays.toString(strArr));
            String targetRuleName = strArr[0];
            int prodIndex =  Integer.parseInt(strArr[1]) - 1;
            int numMutants = randInt(5);

            for (int mutantNum = 0; mutantNum <  numMutants; mutantNum++) {
                try {

                    GrammarReader toAdd = new GrammarReader(this);
                    Rule targetRule = toAdd.getRuleByName(targetRuleName);
                    List<Rule> targetProd  = targetRule.getSubRules().get(prodIndex);

                    
                    //newNT mutation, this favors expanding the grammar, should be balanced with contracting rule
                    if (Constants.CHANGE_RULE_COUNT && Math.random() < Constants.P_CHANGE_RULE_COUNT) {
                        toAdd.genNewNT(targetProd);
                    }
    
                    if (Constants.CHANGE_SYMBOL_COUNT && Math.random() < Constants.P_CHANGE_SYMBOL_COUNT) {
                        // System.out.println("Changing symbol count of " + toAdd.getName());
                        toAdd.changeSymbolCount(targetProd);
                    }
    
                    if (Constants.GROUP && Math.random() < Constants.P_G) {
                        // System.out.println("Grouping in " + toAdd);
                        if(Math.random() < Constants.P_GROUP && canGroup(targetProd)) {
                            toAdd.groupMutate(targetProd);
                        } else if (canUngroup(targetProd)) {
                            toAdd.ungroupMutate(targetProd);
                        }
                    }
    
                    if (Constants.MUTATE && Math.random() < Constants.calculatePM(posScore, negScore)) {
                        // System.out.println("Mutating " + toAdd);
                        if(Math.random() < 0.3) {
                            //wholly new production
                            int prodSize = 1 + randInt(4);
                            Rule prevRule = null;
                            LinkedList<Rule> newProd = new LinkedList<Rule>();
                            List<Rule> allRules = getAllRules();
                            for (int i = 0; i < prodSize; i++) {
                                Rule newRule;
                                do {
                                    newRule = randGet(allRules, true);
                                } while(prevRule != null && newRule == prevRule);
                                prevRule = newRule;
                                newProd.add(newRule);
                            }
                            targetRule.getSubRules().set(prodIndex, newProd);


                        } else {
                            toAdd.symbolMutation(targetProd);
                        }
                        // System.out.println("Post mutation \n" + toAdd);
                    }
    
                    if (Constants.HEURISTIC && Math.random() < Constants.P_H) {
                        // System.out.println("Heuristic on " + toAdd.getName());
                        toAdd.applyHeuristic(targetProd);
                        // System.out.println(toAdd);
                    }

                    
                    if (checkedGrammars.contains(toAdd.hashString())) {
                        App.hashtableHits++;
                        // System.out.println(toAdd + " already generated");
                        mutantNum--;
                    } else {
                        checkedGrammars.add(toAdd.hashString());
                        out.add(toAdd);
                    }
                    // System.out.println(toAdd);
                } catch (Exception e) {
                    System.out.println("Exception during mutant calc\n" + this);
                    e.printStackTrace(System.out);
                    try  {
                        System.in.read();
                    } catch(Exception f) {
    
                    }
                }   
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
    public void applyHeuristic(List<Rule> prod) {
        int totalSelectables = getTotalSelectables(prod);

        Rule toSelect;

        do {
            toSelect = getSelectable(prod, randInt(totalSelectables));
        } while(nullable(toSelect.getName()));

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


    public void symbolMutation(List<Rule> prod) {
        int toMutateIndex = randInt(getTotalSelectables(prod));
        Rule toReplace = getSelectable(prod, toMutateIndex);
        Rule toInsert;

        do {
            toInsert = randGet(getAllRules(), true);

        } while(toInsert.equals(toReplace));

        setSelectable(prod, toMutateIndex, toInsert);
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

    @Override
    public int compareTo(GrammarReader other) {
        return Double.compare(getScore(), other.getScore());
    }

    private String prettyPrintRules(List<Rule> rules) {
        return rules.stream().map(Rule::toString).collect(Collectors.joining("\n"));
    }


    public String getLastMutation() {
        String[] splitArr = getName().split("_");
        return splitArr[splitArr.length-2];
    }

    public static int getTotalSelectables(List<Rule> prod) {
        return prod.stream()
                .mapToInt(Rule::getTotalSelectables)
                .sum();
    }

    public static int getTotalSelectables(Rule mainRule) {
        //we use a base value of 1 so that the LHS of the rule can be selected as well
        return 1 + mainRule.getSubRules().stream()
                .mapToInt(GrammarReader::getTotalSelectables)
                .sum();
    }

    public void setSelectable(List<Rule> prod, int index, Rule toSet) {
        toSet =  toSet.equals(Rule.EPSILON) ? Rule.EPSILON() : toSet.makeMinorCopy();
        for (int i = 0; i < prod.size(); i++) {
            if(index == 0) {
                prod.set(i, toSet);
                return;
            } else {

                Rule currRule = prod.get(i);
                int currRuleProds = currRule.getTotalSelectables();

                if(index < currRuleProds) {
                    currRule.setSelectable(index, toSet);
                    return;
                } else {
                    index -= currRuleProds;
                }
            }
        }
    }

    public static void insertSelectable(List<Rule> prod, int index, Rule toInsert) {

        toInsert = toInsert.equals(Rule.EPSILON) ? Rule.EPSILON() : toInsert.makeMinorCopy();
        for (int i = 0; i < prod.size(); i++) {

            if(index == 0) {
                prod.add(i, toInsert);
                return;
            } else {
                Rule currRule = prod.get(i);
                int currRuleProds = currRule.getTotalSelectables();

                if( index < currRuleProds) {
                    currRule.insertSelectable(index, toInsert);
                    return;
                } else {
                    index -= currRuleProds;
                }
            }
        }
    }

    /**
     * Removes the rule at index in prod
     * index should be calculated with randInt(getTotalSelectables(prod))
     * @param prod
     * @param index
     */
    public static void removeSelectable(List<Rule> prod, int index) {
        for (int i = 0; i < prod.size(); i++) {
            if(index == 0) {
                prod.remove(i);
                return;
            } else {

                Rule rule = prod.get(i);
                int currRuleProds = rule.getTotalSelectables();

                if(index < currRuleProds) {
                    rule.removeSelectable(index);
                    return;
                } else {
                    index -= currRuleProds;
                }
            }
        }
    }

    public static Rule getSelectable(List<Rule> prod, int index) {
        for (int i = 0; i < prod.size(); i++) {
            if(index == 0) {
                return prod.get(i);
            } else {
                Rule rule = prod.get(i);
                int currRuleProds = rule.getTotalSelectables();

                if(index < currRuleProds) {
                    List<Rule> currSelectables = rule.getSubRules().get(0);
                    return getSelectable(currSelectables, index);
                } else {
                    index -= currRuleProds;
                }
            }
        }
        return null;
    }


    /**
     * Determines if the grouping mutation can be applied to some list of rules
     * if there are more than 2 rules or if the sole rule is composite we can group, otherwise false;
     * @param prod
     * @return
     */
    public static boolean canGroup(List<Rule> prod) {
        if(prod.size() > 1) return true;

        return !prod.get(0).isSingular();
    }

    /**
     * Determines if the ungrouping mutation can be applied to some list of rules
     * only return false if all of the rules are singular
     * @param prod
     * @return
     */
    public static boolean canUngroup(List<Rule> prod){
        return !prod.stream().allMatch(Rule::isSingular);

    }



}