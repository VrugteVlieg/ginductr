package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toCollection;
import static java.lang.String.format;
import static stb.App.logMetric;
import static stb.App.recordMetric;
import static java.util.function.Predicate.not;

public class Gram implements Comparable<Gram> {

    public static final String RANDOM_MUTATION = "R";
    public static final String HEUR_MUTATION = "H";
    public static final String GROUP_MUTATION = "G";
    public static final String UNGROUP_MUTATION = "U";
    public static final String NEWNT_MUTATION = "NT";
    public static final String MODEXSTPROD_MUTATION = "M";
    public static final String NEWPROD_MUTATION = "NP";
    public static final String SYMCOUNT_MUTATION = "S";

    public static int NUM_SUGGESTED_MUTANTS = 5;
    public static int NUM_LOGS = 0;
    public static int NUM_LR = 0;
    public static int NUM_MUTS = 0;
    public static Predicate<Gram> passesAnyTest = gram -> gram.getScore() > 0 && !gram.toRemove();
    public static Predicate<Gram> passesPosTest = gram -> gram.getPosScore() > 0 && !gram.toRemove();

    public int genNum = 0;
    public static int currGramNum = 0;
    public static int totalBaseGrams = 0;

    private File grammarFile;
    private List<String> fileLines = new LinkedList<String>();
    private String grammarName;
    private ArrayList<Rule> parserRules = new ArrayList<Rule>();
    private ArrayList<Rule> terminalRules;
    private List<String[]> mutationConsiderations = new LinkedList<String[]>();
    private double posScore = 0.0;
    private double negScore = 0.0;
    int truePositivesPos = 0;
    private double posScoreDelta = 0.0;
    private double negScoreDelta = 0.0;
    int truePositivesNeg = 0;
    int falsePositives = 0;
    int falseNegatives = 0;
    private boolean remove = false;
    public Boolean[] passPosArr;
    public Boolean[] passNegArr;
    List<String> mutHist = new LinkedList<>();
    StringBuilder currMut = new StringBuilder();

    /**
     * Constructs a grammar from a given file
     * 
     * @param filePath
     */
    public Gram(File sourceFile) {

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

    public Gram(String grammarText) {
        System.err.println("Constructing grammar from\n" + grammarText);
        fileLines = Arrays.asList(grammarText.split("\n"));
        grammarName = fileLines.get(0).split(" ")[1];
        this.terminalRules = new ArrayList<Rule>();
        readRules();
    }

    /**
     * Creates a deep copy of a grammarReader, does not copy mutation suggestions
     */
    public Gram(Gram toCopy) {
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
    public Gram(String name, ArrayList<Rule> terminalRules) {
        grammarName = name;
        this.terminalRules = new ArrayList<Rule>(terminalRules);
    }

    /**
     * Concats parserRules and terminals and returns
     */
    public ArrayList<Rule> getAllRules() {
        return Stream.of(parserRules, terminalRules).flatMap(ArrayList::stream).collect(toCollection(ArrayList::new));
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
        return parserRules.get(0).name;
    }

    // Calculates the ANTLR grammar form of this
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("grammar " + grammarName + ";\n");
        getAllRules().forEach(Rule -> out.append(Rule + "\n"));
        out.append("WS  : [ \\n\\r\\t]+ -> skip;");
        return out.toString();
    }

    public String getName() {
        return grammarName;
    }

    ArrayList<Rule> getParserRules() {
        return parserRules;
    }

    // TODO: Add variable number of production alternatives
    /**
     * Generates a new rule and sets it as the start symbol, this rule can
     * references any rule
     * 
     * Grammar cannot be made invalid with new rule
     * 
     * @param ruleName
     * @param RHSLen
     */
    public void generateNewRule(String ruleName, int RHSLen, int ruleIndex) {
        ArrayList<Rule> allRules = getAllRules();
        StringBuilder newRuleText = new StringBuilder();
        for (int i = 0; i < RHSLen; i++) {
            Rule ruleToAppend = randGet(allRules, true);
            newRuleText.append(ruleToAppend.name + " ");
        }
        if(Math.random() < 0.05) newRuleText.append("| ");
        newRuleText.append(";");

        Rule toAdd = new Rule(ruleName, newRuleText.toString());
        parserRules.add(ruleIndex, toAdd);
    }

    public void generateNewRule(String ruleName, int RHSLen) {
        generateNewRule(ruleName, RHSLen, 0);
    }

    public LinkedList<Rule> generateNewProd() {
        // wholly new production
        int prodSize = 1 + randInt(4);
        LinkedList<Rule> newProd = new LinkedList<Rule>();
        List<Rule> allRules = getAllRules();
        for (int i = 0; i < prodSize; i++) {
            Rule newRule = randGet(allRules, true);
            appendSelectable(newProd, newRule);
        }
        return newProd;
    }

    /**
     * Generates a new non terminal and sets a random rule in targerProd to the new
     * non-terminal if the non-terminal would have been of length 1 we do not
     * generate a new rule and instead we just set a random rule to another random
     * rule
     * 
     * @param targetProd
     */
    public void genNewNT(List<Rule> targetProd) {
        currMut = new StringBuilder("Applying genNewNT to" + stringProd(targetProd) + "\n");
        int newRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE - 1);
        int numSelectables = getTotalSelectables(targetProd);
        int replacementIndex = randInt(numSelectables);
        Rule toReplace = getSelectable(targetProd, replacementIndex);
        Rule toInsert;
        currMut.append(String.format("New rule has len %d\nReplacing %s in %s\n", newRuleLen, toReplace,
                stringProd(targetProd)));

        if (newRuleLen == 1) {
            // if the new rule would have had len 1 it would be of the form newRule: ruleA;
            // Instead of making a new rule just insert ruleA where the newRule would have
            // gone
            List<Rule> allRules = getAllRules();
            toInsert = randGet(allRules, true);
            while (toInsert.equals(toReplace)) {
                toInsert = randGet(allRules, true);
            }
            currMut.append("New rule would have had len 1 so inserting " + toInsert + "\n");
        } else if (parserRules.size() == Constants.MAX_RULE_COUNT) {
            return;
        } else {
            int newRuleIndex = 1 + randInt(parserRules.size() - 1);
            generateNewRule(genRuleName(), newRuleLen, newRuleIndex);
            toInsert = parserRules.get(newRuleIndex);
            currMut.append("Generated new rule " + toInsert + "\n");
        }

        setSelectable(targetProd, replacementIndex, toInsert);
        setName(getName() + "_" + NEWNT_MUTATION);
        currMut.append("\n\nResults in " + this + "\n\n");
        mutHist.add(currMut.toString());

    }

    // Generates and returns a rule to be used when filling in blanks in a grammar
    // file
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

        // index of rule to mutate
        System.out.println("Getting rule index for " + grammarName + " on parserRules size " + parserRules.size());

        Rule toMutate = randGet(parserRules, true);
        int productionIndex = randInt(toMutate.getTotalSelectables());

        // if there is only 1 production rule it cannot be made an alternative to
        // another rule
        if (parserRules.size() == 1 && productionIndex == 0)
            productionIndex = 1 + randInt(toMutate.getTotalSelectables() - 1);

        // this symbol is now becoming an alternative to another rule
        if (productionIndex == 0) {

            Rule targetRule = randGet(parserRules, true);

            // Ensures we dont select the same rule twice
            while (targetRule.equals(toMutate)) {
                targetRule = randGet(parserRules, true);
            }

            targetRule.addAlternative(toMutate.getSubRules());

            // References to toMutate now become references to ruleToExtend, if ruleToExtend
            // is nullable then any references to toMutate that have modifiers should have
            // them removed
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
     * Selects an element to be used for crossover, it along with all of its
     * reachbles rules are deep copied into a list
     * 
     * @return
     */
    private LinkedList<Rule> getCrossoverRuleList() {
        LinkedList<Rule> out = new LinkedList<Rule>();
        Rule baseRule = randGet(parserRules, true);
        ArrayList<String> reachables = baseRule.getReachables(parserRules);
        parserRules.stream().filter(rule -> reachables.contains(rule.getName())).map(Rule::new).forEach(out::add);
        return out;
    }

    private void applyCrossover(LinkedList<Rule> toInsert, Rule toReplace, boolean toInsertNullable) {
        parserRules.addAll(toInsert);
        parserRules.remove(toReplace);
        parserRules.forEach(
                rule -> rule.replaceReferences(toReplace, toInsert.getFirst().makeMinorCopy(), toInsertNullable));
    }

    private void applyCrossover(Gram toCrossover) {
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

    private void applyLoggedCrossover(Gram toCrossover, StringBuilder out) {
        LinkedList<Rule> otherList = toCrossover.getCrossoverRuleList();
        LinkedList<Rule> myList = getCrossoverRuleList();
        Rule toSend = myList.getFirst();
        Rule toRecv = otherList.getFirst();
        out.append("\nSwapping " + myList.stream().map(Rule::getName).collect(Collectors.joining(", ")) + " with "
                + otherList.stream().map(Rule::getName).collect(Collectors.joining(", ")) + "\n");
        boolean toInsNull = toCrossover.nullable(toRecv.getName());
        boolean toSendNull = nullable(toSend.getName());
        toCrossover.applyCrossover(myList, otherList.getFirst(), toInsNull);
        applyCrossover(otherList, myList.getFirst(), toSendNull);
    }

    public static void Crossover(Gram g0, Gram g1) {
        g0.applyCrossover(g1);
    }

    public static void loggedCrossover(Gram g0, Gram g1, StringBuilder out) {
        g0.applyLoggedCrossover(g1, out);
    }

    public static int randInt(int bound) {
        if (bound == 0)
            return 0;
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public void setPosScore(double newScore) {
        posScoreDelta = newScore - posScore;

        posScore = newScore;
    }

    public void setNegScore(double newScore) {
        negScoreDelta = newScore - negScore;
        negScore = newScore;
    }

    public double getPosScoreDelta() {
        return posScoreDelta;
    }

    public double getNegScoreDelta() {
        return negScoreDelta;
    }

    public double getScoreDelta() {
        return (posScoreDelta + negScoreDelta) / 2;
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
        List<String> unreachableRules = parserRules.stream().map(Rule::getName)
                .filter(rule -> !reachableRules.contains(rule)).collect(toCollection(ArrayList::new));
        if (unreachableRules.size() == 0)
            return;
        while (unreachableRules.size() > 0) {
            Rule toMakeReach = getRuleByName(randGet(unreachableRules, false));
            Rule toExtend = getRuleByName(randGet(reachableRules, true));
            int indexToExtend = 1 + randInt(toExtend.getTotalSelectables() - 1);
            Rule toMakeAlt = toExtend.getSelectable(indexToExtend);
            toMakeReach.addAlternative(toMakeAlt);
            toExtend.setSelectable(indexToExtend, toMakeReach);
        }
    }

    /**
     * Makes all rules in the grammar reachable, useful for ensuring that all
     * terminal rules are involved in some rule
     */
    public void removeUnreachableBoogaloo() {
        // System.err.println("---------\nApplying removeUnBoog to \n" + this + "\n");
        List<String> reachables = parserRules.get(0).getReachables(parserRules);
        // System.err.println("reachables: " + reachables.stream().collect(Collectors.joining(", ")));

        // System.err.println("This gram1\n" + this);
        List<String> unreachables = getAllRules().stream().
                map(Rule::getName)
                .filter(not(reachables::contains))
                .collect(toCollection(ArrayList::new));

        // System.err.println("Unreachables: " + unreachables.stream().collect(Collectors.joining(", ")));
        // System.err.println("This gram2\n" + this);
        
        List<Rule> reachableParserRules = reachables.stream().map(this::getRuleByName).filter(parserRules::contains)
        .collect(toCollection(ArrayList::new));
        
        /**
         * Process 1 compute which rules can be reached/ not reached from start symbol 2
         * compute which parserRules can be reached 3 for every unreachable rule ruleA,
         * choose a reachable rule ruleB 4 choose a symbol on the RHS of reachable ruleB
         * 5a if the unreachable rule was a parserRule, let symbA be an alternative to
         * ruleA, replace symbA in ruleB with ruleA 5b if the unreachable rule was a
         * lexerRule, create a new rule RuleC: ruleA | symbA;, replace symbA in ruleB
         * with ruleC
         */
        while (unreachables.size() > 0) {
            // System.err.println("yeeeeeeet");
            Rule toMakeReachable = getRuleByName(randGet(unreachables, false));
            // System.err.println("Making " + toMakeReachable.getName() + " reachable");
            Rule toExtend = randGet(reachableParserRules, true);
            // System.err.println("Extending " + toExtend);
            int indexToExtend = 1 + randInt(toExtend.getTotalSelectables() - 1);
            boolean makeNewRule = Math.random() < 0.4; // if we are making a new rule or simply adding the terminal as
            Rule toMakeAlt = toExtend.getSelectable(indexToExtend);
            // an alternative to an existing reachable
            if (toMakeReachable.isTerminal()) {
                /**
                 * If we are trying to make a terminal rule RuleA reachable we make a new rule
                 * ruleB: RuleA | toMakeAlt
                 */
                if (makeNewRule) {
                    String newRuleName = genRuleName();
                    String ruleText = toMakeReachable.getName() + " | " + toMakeAlt.getName() + " ;";
                    Rule newRule = new Rule(newRuleName, ruleText);
                    // System.err.println("Making " + toMakeReachable.getName() + " reachble adding new Rule " + newRule
                    //         + "\nreplacing\n" + toMakeAlt.getName() + "  in " + toExtend);
                    parserRules.add(newRule);
                    toMakeReachable = newRule;
                    toExtend.setSelectable(indexToExtend, toMakeReachable);
                } else {
                    // System.err.println(
                    //         "Making " + toMakeReachable.getName() + "  by adding it as an alternative to\n" + toExtend);
                    toExtend.addAlternative(toMakeReachable.makeMinorCopy());
                }
            } else {
                if (makeNewRule || true) {
                    toMakeReachable.addAlternative(toMakeAlt);
                    toExtend.setSelectable(indexToExtend, toMakeReachable);
                    // System.err.println("Making " + toMakeReachable.getName() + " reachable by making subbing in "
                    //         + toMakeAlt.getName() + " in " + toExtend);
                } else {
                    toExtend.getSubRules().addAll(toMakeReachable.getSubRules());
                    parserRules.remove(toMakeReachable);
                    
                    System.err.println(
                            "Making " + toMakeReachable.getName() + " reachable by adding all prods to  " + toExtend);
                    for(Rule rule : parserRules) {
                        rule.replaceReferences(toMakeReachable, toExtend, nullable(toExtend));
                    }
                }
            }
            // System.err.println("Leads to " + this + '\n' + "Remaining unreachables " + unreachables);
            // System.err.println("awe" + unreachables.size());
            // try {
            // System.in.read();
            // } catch(Exception e) {}
        }
        // System.err.println("Retuning from removeUnreach");

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
            mainToChange.printParent = (String whatever) -> flagForRemoval();
            if (!mainToChange.containsEpsilon()) {
                mainToChange.addEpsilon();
                cleanReferences(mainToChange);
            } else {
                mainToChange.removeEpsilon();
                // If a rule was only epsilon remove it from parserRules
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
     * 
     * @return
     */
    public LinkedList<String> constrNullable() {
        for (Rule rule : parserRules)
            rule.printParent = (String what) -> flagForRemoval();
        LinkedList<String> nullableNames = new LinkedList<String>();
        parserRules.stream().filter(Rule::containsEpsilon).map(rule -> rule.name).forEach(nullableNames::add);

        int startSize = nullableNames.size();
        do {
            startSize = nullableNames.size();
            parserRules.stream().forEach(rule -> {
                rule.nullable(nullableNames);
            });
            System.err.println("awe " + startSize  + " " + nullableNames);
        } while (startSize != nullableNames.size());
        return nullableNames;
    }

    /**
     * Computes all nullable rules in this grammar and checks if the targetName is
     * in the list TODO should be updated to run allMatch on rule of the form (ruleA
     * ruleB ruleC)
     */
    public boolean nullable(String targetName) {
        return constrNullable().contains(targetName);
    }

    /**
     * Goes through all rules and removes modifiers of rules containing toClean Used
     * after toClean is made nullable
     * 
     * @param toClean
     */
    private void cleanReferences(Rule toClean) {
        parserRules.forEach(rule -> {
            if (rule.getRuleText().contains(toClean.getName())) {
                rule.cleanReferences(toClean);
            }
        });
    }

    public void cleanReferences(int index) {
        cleanReferences(parserRules.get(index));
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

        List<String> undefined = parserRules.stream().map(rule -> rule.getReachables(parserRules))
                .flatMap(ArrayList::stream).filter(s -> s.contains("Undefined ")).distinct()
                .collect(toCollection(ArrayList::new));
        return undefined;
    }

    /**
     * Checks if the grammar contains any undefineRules and marks the grammar for
     * removal if there are
     */
    public void fixUndefinedRules() {
        List<String> undefined = getUndefinedRules();
        undefined.forEach(undef -> parserRules.forEach(rule -> {
            if (rule.toString().contains(undef)) {
                rule.removeReferences(undef);
            }
        }));
    }

    /**
     * Removes left recursion by rewriting as an equivalent grammar TODO add
     * removing of recursive derivations
     */
    // public void removeLR() {
    // if (containsLeftRecursive()) {
    // // Replaces repeating left recursive symbols with a single instance and
    // removes
    // // it
    // // term: term factor | term term; -> term: term factor;
    // parserRules.forEach(rule -> {
    // rule.simplifyRepeatingLR();
    // rule.removeSimpleLeftRecursives();
    // });
    // while (parserRules.stream().anyMatch(rule -> rule.getTotalSelectables() ==
    // 1)) {
    // LinkedList<String> toCleanList = new LinkedList<String>();
    // for (Rule rule : parserRules) {

    // if (rule.getTotalSelectables() == 1) {
    // toCleanList.add(rule.getName());
    // }
    // }
    // parserRules.removeIf(rule -> toCleanList.contains(rule.getName()));
    // toCleanList.forEach(name -> parserRules.forEach(rule ->
    // rule.removeReferences(name)));
    // }

    // removeDirectLeftRecursion();
    // }

    // }

    /**
     * Rewrites left recursive rules as equivalent rules by introducing new rules
     */
    // private void removeDirectLeftRecursion() {
    // ArrayList<Rule> rulesToAdd = new ArrayList<Rule>();
    // parserRules.forEach(rule -> {
    // if (rule.containLeftRecursiveProd()) {
    // String ruleName = rule.getName();
    // String newRuleName = Rule.genName();
    // ArrayList<LinkedList<Rule>> cleanProductions = new
    // ArrayList<LinkedList<Rule>>();
    // ArrayList<LinkedList<Rule>> dirtyProductions = new
    // ArrayList<LinkedList<Rule>>();
    // rule.getSubRules().forEach(subRule -> {

    // if (subRule.getFirst().equals(Rule.EPSILON()))
    // return;

    // if (!subRule.getFirst().getName().split(" ")[0].equals(ruleName)) {
    // cleanProductions.add(subRule);
    // } else {
    // LinkedList<Rule> toAdd = new LinkedList<Rule>(subRule.subList(1,
    // subRule.size()));
    // Rule ruleToAdd = new Rule(newRuleName);
    // toAdd.add(ruleToAdd);
    // dirtyProductions.add(toAdd);
    // }
    // });
    // if (cleanProductions.size() == 0) {
    // flagForRemoval();
    // return;
    // }

    // StringBuilder newRuleText = new StringBuilder();
    // dirtyProductions.forEach(subProduction -> {
    // subProduction.forEach(subRule -> {
    // newRuleText.append(subRule + " ");
    // });
    // newRuleText.append("| ");
    // });
    // newRuleText.append(";");

    // // Create the rule, if all the productions were dirty, reuse the same rule
    // name
    // // so replace all occurences of newRuleName with original name
    // // The productions of the dirtyRule is later copied into the original rule
    // Rule dirtyRule = new Rule(newRuleName, newRuleText.toString());

    // // If all of the orignal rules were dirty reuse the ruleName for the modified
    // // rule other wise introduce a new rule
    // rule.getSubRules().clear();

    // rule.setSubRules(cleanProductions);
    // rulesToAdd.add(dirtyRule);
    // }
    // });
    // parserRules.addAll(rulesToAdd);
    // }

    /**
     * Returns true if this contains a parserRule that is directly left recursive
     * term: term factor;
     */
    // public boolean containsLeftRecursive() {
    // for (int i = 0; i < parserRules.size(); i++) {
    // if (parserRules.get(i).containLeftRecursiveProd())
    // return true;
    // }
    // return false;
    // }

    public boolean containsImmediateLR() {
        return parserRules.stream().anyMatch(Rule::containsImmediateLR);
    }

    public boolean containsImmediateLRDeriv() {
        List<String> nullables = constrNullable();
        List<Rule> start = new ArrayList<>();
        List<Rule> visitedRules = new ArrayList<>();
        for (Rule rule : parserRules) {
            // System.err.println("Checking LRDERIV of " + rule);
            start.add(rule);
            if (containsImmediateLRDeriv(start, visitedRules, nullables))
                return true;
            start.clear();
            visitedRules.clear();
        }
        return false;
    }

    private boolean containsImmediateLRDeriv(List<Rule> startRules, List<Rule> visitedRules, List<String> nullables) {
        // If we have already visited this rule then we are in a loop
        // System.err.println("contains LRDERIV " + startRules.stream().map(Rule::toString).collect(Collectors.joining(", ")));
        startRules.removeIf(rule -> rule.isTerminal() || rule.name.equals(" "));
        if (startRules.size() == 0)
            return false;
        if (visitedRules.stream().anyMatch(startRules::contains))
            ;
        if (visitedRules.stream().anyMatch(startRules::contains))
            return true;
        for (Rule rule : startRules) {
            visitedRules.add(rule);
            Rule targetRule = getRuleByName(rule.name);
            List<Rule> newStarts = targetRule.getFirstSingWOptional(nullables);
            if (containsImmediateLRDeriv(newStarts, visitedRules, nullables)) {
                // System.err.println(targetRule.getName() + " triggers LRDeriv visited rules "
                // + visitedRules.stream().map(Rule::getName).collect(Collectors.joining(",
                // ")));
                return true;
            }
            visitedRules.remove(rule);
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
     * Generates a name for mutant grammar based on this grammar
     */
    public String genMutantName() {
        StringBuilder mutantNameBuilder = new StringBuilder("grammar" + "_");
        // Generates rule name by randomly concatting letters
        for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
            mutantNameBuilder.append((char) ('a' + randInt(26)));
        }
        return mutantNameBuilder.toString();
    }

    /**
     * Computes which rules can have their RHS incremented and which can be
     * decremented a rule is then randomly selected and either decremented or
     * incremented
     */
    public void changeSymbolCount(List<Rule> targetProd) {
        currMut = new StringBuilder("Applying changeSymbolCount to " + stringProd(targetProd) + "\n\n");
        int numSelectables = getTotalSelectables(targetProd);
        int index = randInt(numSelectables);
        if (Math.random() < Constants.P_ADD_SYMBOL) {
            Rule toInsert = randGet(parserRules, true);
            currMut.append("Inserting " + toInsert.getName() + " at index " + index + "\n");
            insertSelectable(targetProd, index, randGet(parserRules, true));

            setName(getName() + "_" + SYMCOUNT_MUTATION);
        } else if (targetProd.size() > 1 || index > 0) {
            // If there is only 1 rule in the prod removing it would break the grammar
            // if the index is not 0 however then the rule must be a composite rule and we
            // proceed
            currMut.append("Removing " + getSelectable(targetProd, index) + " at index " + index);
            removeSelectable(targetProd, index);

        }
        currMut.append("\n\n" + "Results in " + this + "\n\n");
        mutHist.add(currMut.toString());
    }

    public void groupMutate(List<Rule> prod) {

        /**
         * this first check makes us favor grouping at higher levels in general this
         * should be a better solution than equally weighting all levels
         */
        currMut = new StringBuilder(format("Applying grouping to %s\n\n", stringProd(prod)));
        try {

            boolean prodSize = prod.size() > 1;
            if (prodSize) {
                int startIndex = randInt(prod.size() - 1);
                int endIndex = startIndex + 1 + randInt(prod.size() - startIndex - 1);
                String origProd = stringProd(prod);
                List<Rule> toGroup = new LinkedList<>();
                prod.subList(startIndex, endIndex + 1).forEach(toGroup::add);
                Rule toInsert = new Rule(toGroup);

                IntStream.rangeClosed(startIndex, endIndex).forEach(val -> prod.remove(startIndex));
                currMut.append("Removing indices " + startIndex + " - " + endIndex + " " + toInsert + " from "
                        + origProd + "\n");
                currMut.append("Inserting  " + toInsert + " at " + startIndex + " in " + stringProd(prod) + "\n");
                // If we grouped untill the end of the prod, e.g | ruleA (ruleB ruleC) | then
                // start index would be 2, after removing ruleB and ruleC however, the prod is
                // only len1 so we cannot insert at index2 we need to append
                if (startIndex == prod.size() + 1) {
                    prod.add(toInsert);
                } else {
                    prod.add(startIndex, toInsert);
                }
            } else {
                currMut.append("Getting the sole rule in " + stringProd(prod) + " in " + this);
                Rule soleRule = prod.get(0);
                List<Rule> innerRules = soleRule.getSubRules().get(0);
                groupMutate(innerRules);
            }
        } catch (Exception e) {
            e.printStackTrace();
            currMut.append(e.toString());
        } finally {
            currMut.append("\n\n" + "Results in " + this + "\n\n");
            mutHist.add(currMut.toString());
        }
    }

    /**
     * Applied ungrouping mutation to some list of rules ungrouping goes to any
     * depth
     * 
     * @param prod
     */
    public void ungroupMutate(List<Rule> prod) {
        StringBuilder currMut = new StringBuilder(format("Applying ungroup to %s\n", stringProd(prod)));
        int totalSelectables = getTotalSelectables(prod);
        List<Integer> ungroupableIndices = new LinkedList<Integer>();
        for (Integer i = 0; i < totalSelectables; i++) {
            if (!getSelectable(prod, i).isSingular()) {
                ungroupableIndices.add(i);
            }
        }

        currMut.append(format("Ungroupable indices: %s\n", ungroupableIndices.toString()));

        int toUngroupIndex = randGet(ungroupableIndices, true);
        List<Rule> targetSubRules = getSelectable(prod, toUngroupIndex).getSubRules().get(0);
        currMut.append(format("Expanding %s at index %d", stringProd(targetSubRules), toUngroupIndex));
        Collections.reverse(targetSubRules);
        // System.err.println("Pre ungroup" + prod);
        targetSubRules.forEach(rule -> insertSelectable(prod, toUngroupIndex, rule));
        // System.err.println("Post ungroup " + prod);
        removeSelectable(prod, toUngroupIndex + targetSubRules.size());
        // System.err.println("Post removal " + prod);
        currMut.append("\n\n" + "Results in " + this + "\n\n");
        mutHist.add(currMut.toString());

    }

    public boolean nullable(List<Rule> prod) {
        List<String> nullables = constrNullable();
        return prod.stream().allMatch(rule -> rule.nullable(nullables));
    }

    /**
     * Generates new NT and inserts into targetProd replacing another rule or
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
     * 
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
        // System.err.println("Checking if " + this + " contains an infLoop");
        if (getUndefinedRules().stream().anyMatch(rule -> rule.contains("Undefined "))) {
            return true;
        }
        Rule start = parserRules.get(0);
        String touchedRules = "";
        try {
            if (!start.containsInfLoop(parserRules, touchedRules)) {
                // System.out.println(this + " contains no infLoops");
                return false;
            } else {
                // System.out.println(this + " contains an infLoop");
                return true;

            }
        } catch (Exception e) {
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

    public void demoChangeSymbolCount(Optional<String> suggestion) {
        Rule targetRule;
        int prodIndex;

        if (suggestion.isPresent()) {
            App.loAppendText("Applying changeSymbCount mutation to " + getName() + " suggested location "
                    + suggestion.get() + '\n');
            String[] data = suggestion.get().split(":");
            targetRule = getRuleByName(data[0]);
            prodIndex = Integer.parseInt(data[1]) - 1;
        } else {
            App.loAppendText("Applying changeSymbCount mutation to " + getName() + " with no suggestions\n");
            targetRule = randGet(parserRules, true);
            prodIndex = randInt(targetRule.getSubRules().size());
        }
        List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);

        int numSelectables = getTotalSelectables(targetProd);
        int index = randInt(numSelectables);

        StringBuilder compare = new StringBuilder("\n" + targetRule.toString() + "\n||\nV\n");
        if (Math.random() < Constants.P_ADD_SYMBOL) {

            Rule toInsert = randGet(parserRules, true);
            App.goAppendText(
                    String.format("\nInserting %s at position %d in %s", toInsert.getName(), index, targetProd));
            insertSelectable(targetProd, index, toInsert);
            setName(getName() + "_" + SYMCOUNT_MUTATION);
        } else if (targetProd.size() > 1 || index > 0) {
            // If there is only 1 rule in the prod removing it would break the grammar
            // if the index is not 0 however then the rule must be a composite rule and we
            // proceed
            Rule toRemove = getSelectable(targetProd, index);
            App.goAppendText(String.format("\nRemoving %s in %s", toRemove, targetProd));
            removeSelectable(targetProd, index);

            setName(getName() + "_" + SYMCOUNT_MUTATION);
        } else {
            Rule toInsert = randGet(parserRules, true);
            App.goAppendText(
                    String.format("\nInserting %s at position %d in %s", toInsert.getName(), index, targetProd));
            insertSelectable(targetProd, index, toInsert);
            setName(getName() + "_" + SYMCOUNT_MUTATION);
        }

        compare.append(targetRule + "\n");
        App.goAppendText(compare.toString());

    }

    public void demoGroupMutate(Optional<String> suggestion) {

        List<List<Rule>> groupables = new LinkedList<>();
        List<List<Rule>> ungroupables = new LinkedList<>();

        if (suggestion.isPresent()) {
            App.loAppendText(
                    "Applying grouping mutation to " + getName() + " suggested location " + suggestion.get() + '\n');
            String[] data = suggestion.get().split(":");
            Rule targetRule = getRuleByName(data[0]);
            int prodIndex = Integer.parseInt(data[1]) - 1;
            List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);
            if (canGroup(targetProd))
                groupables.add(targetProd);
            if (canUngroup(targetProd))
                ungroupables.add(targetProd);

        } else {
            groupables = parserRules.stream().map(Rule::getSubRules).flatMap(ArrayList::stream).filter(Gram::canGroup)
                    .collect(toList());

            ungroupables = parserRules.stream().map(Rule::getSubRules).flatMap(ArrayList::stream)
                    .filter(Gram::canUngroup).collect(toList());
        }

        // System.err.println("Groupables: " + groupables);

        if (groupables.isEmpty()) {
            if (ungroupables.isEmpty()) {
                App.goAppendText("\nThere are no productions that can be grouped or ungrouped\n");

            } else {
                List<Rule> targetProd = randGet(ungroupables, true);
                App.goAppendText("\nUngrouping in " + targetProd + '\n');
                ungroupMutate(targetProd);

            }

        } else if (ungroupables.isEmpty()) {
            List<Rule> targetProd = randGet(groupables, true);
            App.goAppendText("\nGrouping in " + targetProd + '\n');
            groupMutate(targetProd);

        } else {

            if (Math.random() < 0.5) {
                List<Rule> targetProd = randGet(groupables, true);
                App.goAppendText("\nGrouping in " + targetProd + '\n');
                groupMutate(targetProd);
            } else {
                List<Rule> targetProd = randGet(ungroupables, true);
                App.goAppendText("\nUngrouping in " + targetProd + '\n');
                ungroupMutate(targetProd);
            }
        }

    }

    /**
     * Selects a random parserRule A and sets a random symbol B in A to be another
     * rule C in the grammar this can include selecting the LHS as the thing to be
     * mutated, in this case A has its RHS added as alternative to C and A is
     * removed All references to A are then made references to C
     */
    public void demoMutate(outputLambda logOutput, outputLambda grammarOutput) {

    }

    public void demoHeuristic(Optional<String> suggestion) {
        List<String> nullables = constrNullable();
        // App.goAppendText("Nullables " + nullables + "\n\n");
        List<Rule> prod;
        if (suggestion.isPresent()) {
            App.loAppendText(
                    "Applying heuristic mutation to " + getName() + " suggested location " + suggestion.get() + '\n');
            String[] data = suggestion.get().split(":");
            Rule targetRule = getRuleByName(data[0]);
            int prodIndex = Integer.parseInt(data[1]) - 1;
            prod = targetRule.getSubRules().get(prodIndex);
            if (!canHeur(prod)) {
                App.goAppendText(suggestion.get() + " does not contain a rule that can be heuristicked");
                return;
            }

        } else {

            List<List<Rule>> heurables = parserRules.stream().map(Rule::getSubRules).flatMap(ArrayList::stream)
                    .filter(this::canHeur).collect(toCollection(ArrayList::new));

            if (heurables.isEmpty()) {
                App.goAppendText(getName() + " has not productions that can be heuristicked");
                return;
            }
            prod = randGet(heurables, true);
            // App.goAppendText("Heurables " + heurables + "\n\n");
        }

        // System.err.println("heuristic Calling getSelectable " + toSelectIndex + " " +
        // stringProd(prod));

        int totalSelectables = getTotalSelectables(prod);
        while (true) {
            int toSelectIndex = randInt(totalSelectables);

            Rule toSelect = getSelectable(prod, toSelectIndex);
            if (nullables.contains(toSelect.getName()))
                continue;
            App.goAppendText("\nApplying heuristic to " + toSelect + " in " + prod);
            double choice = Math.random();
            if (choice < 0.33) {
                toSelect.setIterative(!toSelect.isIterative());
            } else if (choice < 0.66) {
                toSelect.setOptional(!toSelect.isOptional());
            } else {
                toSelect.setIterative(!toSelect.isIterative());
                toSelect.setOptional(!toSelect.isOptional());
            }
            break;
        }

        setName(getName() + "_" + HEUR_MUTATION);

    }

    public void demoNewNT(Optional<String> suggestion) {

        Rule targetRule;
        int prodIndex;

        if (suggestion.isPresent()) {
            App.loAppendText(
                    "Applying newNT mutation to " + getName() + " suggested location " + suggestion.get() + '\n');
            String[] data = suggestion.get().split(":");
            targetRule = getRuleByName(data[0]);
            prodIndex = Integer.parseInt(data[1]) - 1;
        } else {
            App.loAppendText("Applying newNT mutation to " + getName() + " with no suggestions\n");
            targetRule = randGet(parserRules, true);
            prodIndex = randInt(targetRule.getSubRules().size());
        }

        List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);

        // Code from genNewNT
        int newRuleLen = 2 + randInt(Constants.MAX_RHS_SIZE - 2);
        int numSelectables = getTotalSelectables(targetProd);
        int replacementIndex = randInt(numSelectables);
        Rule toReplace = getSelectable(targetProd, replacementIndex);
        int newRuleIndex = 1 + randInt(parserRules.size() - 1);

        generateNewRule(genRuleName(), newRuleLen, newRuleIndex);
        Rule toInsert = parserRules.get(newRuleIndex);
        App.goAppendText(String.format("\nGenerated new rule\n%s\nTo replace %s in %s\n", toInsert,
                toReplace.getDebugName(), targetRule));

        StringBuilder compare = new StringBuilder("\n" + targetRule.toString() + "\n||\nV\n");

        setSelectable(targetProd, replacementIndex, toInsert);
        compare.append(targetRule.toString() + "\n");
        App.goAppendText(compare.toString());
        setName(getName() + "_" + NEWNT_MUTATION);

    }

    public void setMutationConsideration(List<String> newList) {
        App.rgoSetText("Setting mutations list for " + this);
        mutationConsiderations.clear();
        mutationConsiderations
                .addAll(newList.stream().limit(3).map(str -> str.split(":")).collect(Collectors.toList()));
    }

    public List<String[]> getMutationConsideration() {
        return mutationConsiderations;
    }

    public Rule getRuleByName(String name) {
        // System.err.println("Searching for " + name + " in " + this);

        return getAllRules().stream().filter(rule -> rule.name.equals(name)).findFirst().get();
    }

    public static LinkedList<Gram> computeMutants(Gram currBase) {
        LinkedList<Gram> out = new LinkedList<Gram>();

        // App.rgoSetText("Computing mutants for " + this);
        currBase.mutationConsiderations.stream().forEach(strArr -> {
            // StringBuilder currMutants = new StringBuilder("\nUsing key " +
            // Arrays.toString(strArr));
            String targetRuleName = strArr[0];
            int prodIndex = Integer.parseInt(strArr[1]) - 1;

            for (int mutantNum = 0; mutantNum < NUM_SUGGESTED_MUTANTS; mutantNum++) {
                try {
                    Gram toAdd = new Gram(currBase);
                    toAdd.setName(toAdd.genMutantName());
                    toAdd.initMutHist(strArr);

                    Rule targetRule = toAdd.getRuleByName(targetRuleName);
                    if(Math.random() < 0.02 ) {
                        if(targetRule.containsEpsilon()) {
                            targetRule.removeEpsilon();
                        } else {
                            targetRule.addEpsilon();
                        }
                    } 
                    List<Rule> targetProd = targetRule.getSubRules().get(prodIndex);

                    // newNT mutation, this favors expanding the grammar, should be balanced with
                    // contracting rule
                    if (Constants.CHANGE_RULE_COUNT && Math.random() < Constants.P_CHANGE_RULE_COUNT) {
                        // if(Math.random() < Constants.P_ADD_RULE) {

                        toAdd.genNewNT(targetProd);
                        // } else {
                        // toAdd.removeRule(targetProd);
                        // }
                    }

                    if (Constants.CHANGE_SYMBOL_COUNT && Math.random() < Constants.P_CHANGE_SYMBOL_COUNT) {
                        // System.out.println("Changing symbol count of " + toAdd.getName());
                        toAdd.changeSymbolCount(targetProd);
                    }

                    if (Constants.GROUP && Math.random() < Constants.P_G) {
                        // System.out.println("Grouping in " + toAdd);
                        if (Math.random() < Constants.P_GROUP && canGroup(targetProd)) {
                            // System.err.println(stringProd(targetProd) + " is considered groupable");
                            toAdd.groupMutate(targetProd);
                        } else if (canUngroup(targetProd)) {
                            toAdd.ungroupMutate(targetProd);
                        }
                    }

                    if (Constants.MUTATE && Math.random() < Constants.P_M) {
                        // System.out.println("Mutating " + toAdd);
                        int numSelectables = getTotalSelectables(targetProd) + 1;
                        int toMutIndex = randInt(numSelectables);
                        if (toMutIndex == 0) {
                            toAdd.currMut = new StringBuilder("Generating a new prod\n");
                            targetRule.getSubRules().set(prodIndex, toAdd.generateNewProd());
                            toAdd.setName(toAdd.getName() + "_" + NEWPROD_MUTATION);
                            toAdd.currMut.append(format("Results in %s\n", toAdd.toString()));
                            toAdd.mutHist.add(toAdd.currMut.toString());
                        } else {
                            toAdd.symbolMutation(targetProd);
                        }
                        // System.out.println("Post mutation \n" + toAdd);
                    }

                    if (Constants.HEURISTIC && Math.random() < Constants.P_H) {
                        // System.out.println("Heuristic on " + toAdd.getName());
                        // boolean nullGoingIn =
                        List<String> initialNullables = toAdd.constrNullable();
                        toAdd.applyHeuristic(targetProd);
                        if (toAdd.nullable(targetRuleName)) {
                            toAdd.currMut.append(targetRuleName + " is now nullable cleaning references");
                            toAdd.cleanReferences(targetRule);
                            toAdd.currMut.append("Results in " + toAdd.toString());
                        }

                        List<String> newNullables = toAdd.constrNullable();
                        newNullables.removeAll(initialNullables);

                        // If we have more nullable rules after applying heuristic then we should go
                        // through all rules and remove closures
                        newNullables.stream().map(toAdd::getRuleByName).forEach(toAdd::cleanReferences);
                        if (newNullables.size() > 0)
                            toAdd.cleanAllEmptyClosures();

                        toAdd.mutHist.add(toAdd.currMut.toString());

                        // System.out.println(toAdd);
                    }

                    // if (Math.random() < 0.5) {
                    //     int mostLikelyIndex = mostLikelyIndex(targetRule);
                    // }

                    toAdd.removeDuplicateProductions();
                    toAdd.mutHist.add("Final Gram " + toAdd.toString());
                    // toAdd.removeUnreachableBoogaloo();
                    NUM_MUTS++;
                    boolean LrDeriv = toAdd.containsImmediateLRDeriv();
                    boolean nullClosure = toAdd.containsNullClosure();
                    if (LrDeriv)
                        NUM_LR++;

                    if (toAdd.toRemove() || toAdd.toString().contains("++")) {
                        toAdd.logGrammar(true);
                        if (toAdd.toString().contains("++"))
                            toAdd.mutHist.add("\nplusses " + stringProd(targetProd));
                        // if(NUM_LOGS++ == 10) System.exit(1);
                    }
                    boolean alreadyChecked = App.gramAlreadyChecked(toAdd);
                    if (alreadyChecked || LrDeriv || nullClosure) {
                        // if(NUM_LOGS++ < 20) {
                        // System.err.println("Reseting mutatnt comp " + (LrDeriv ? "LRDeriv" : "") +
                        // (alreadyChecked ? "Already Checked" : ""));
                        // System.err.println(toAdd.toString());
                        // }
                        continue;
                    } else {
                        // currMutants.append("\n" + mutantNum + ": " + toAdd.getName());
                        out.add(toAdd);
                    }
                    // System.out.println(toAdd);
                } catch (Exception e) {
                    System.err.println("Exception during mutant calc\n" + currBase);
                    e.printStackTrace();
                    // try {
                    // System.out.println("Press enter to continue");
                    // System.in.read();
                    // } catch(Exception f) {

                    // }
                }
            }
        });
        System.err.println("Done comping for " + ++currGramNum + "/" + totalBaseGrams);
        return out;

    }

    public String genRuleName() {
        StringBuilder ruleNameBuilder = new StringBuilder();
        // Generates rule name by randomly concatting letters
        while (ruleNameBuilder.length() == 0) {
            for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
                ruleNameBuilder.append((char) ('a' + randInt(26)));
            }
            // Check if the generated ruleName matches an existing parserRule, only
            // parserRules are checked as lowercase letters are used exclusively
            boolean duplicateRuleName = parserRules.stream().map(Rule::getName)
                    .anyMatch(ruleNameBuilder.toString()::equals);

            // clears the name builder if it is the reserved word program or a duplicate of
            // an existing ruleName
            if (ruleNameBuilder.toString().equals("program") || duplicateRuleName)
                ruleNameBuilder.setLength(0);
        }
        return ruleNameBuilder.toString();
    }

    public double COMPUTE_RECALL() {
        double truePos = truePositivesNeg + truePositivesPos;
        return truePos / (truePos + falseNegatives);
    }

    public double COMPUTE_PRECISION() {
        double truePos = truePositivesNeg + truePositivesPos;
        return truePos / (truePos + falsePositives);
    }

    public double COMPUTE_F2() {
        double prec = COMPUTE_PRECISION();
        double recall = COMPUTE_RECALL();
        return (5 * prec * recall) / (4 * prec + recall);
    }

    /**
     * Applied heuristic mutation to a random rule in a list
     * 
     * @param toHeur
     */
    public void applyHeuristic(List<Rule> prod) {
        int totalSelectables = getTotalSelectables(prod);
        int toSelectIndex = randInt(totalSelectables);
        // System.err.println("heuristic Calling getSelectable " + toSelectIndex + " " +
        // stringProd(prod));

        Rule toSelect = getSelectable(prod, toSelectIndex);
        int counter = 0;
        while (nullable(toSelect.getName()) && counter++ < 5) {
            toSelect = getSelectable(prod, randInt(totalSelectables));
        }
        if (counter == 5)
            return;

        double choice = Math.random();
        currMut = new StringBuilder(
                format("Applying heuristic choice %f to %s in %s", choice, toSelect.toString(), stringProd(prod)));
        if (choice < 0.33) {
            toSelect.setIterative(!toSelect.isIterative());
        } else if (choice < 0.66) {
            toSelect.setOptional(!toSelect.isOptional());
        } else {
            toSelect.setIterative(!toSelect.isIterative());
            toSelect.setOptional(!toSelect.isOptional());
        }
        currMut.append(format("\n\nResults in %s\n\n", this.toString()));
        mutHist.add(currMut.toString());
    }

    /**
     * Modifies an existing prod by randomly replacing one of the rules with another
     * rule
     * 
     * @param prod
     */
    public void symbolMutation(List<Rule> prod) {
        currMut = new StringBuilder(format("Applying symbol mutation to %s\n", stringProd(prod)));
        int toMutateIndex = randInt(getTotalSelectables(prod));
        Rule toReplace = getSelectable(prod, toMutateIndex);
        Rule toInsert = randGet(getAllRules(), true);

        int earlyExit = 0;
        while (toInsert.equals(toReplace) && earlyExit++ < 5)
            toInsert = randGet(getAllRules(), true);

        currMut.append(format("Replacing %s by %s", toReplace.getName(), toInsert.getName()));

        setSelectable(prod, toMutateIndex, toInsert);
        setName(getName() + "_" + MODEXSTPROD_MUTATION);
        currMut.append(format("\n\nResults in %s\n\n", this));
        mutHist.add(currMut.toString());
    }

    /**
     * samples a random element from a list and returns it
     * 
     * @param <E>
     * @param input
     * @param replace sample with replacement or without replacement
     * @return
     */
    public <E> E randGet(List<E> input, boolean replace) {
        if (replace) {
            return input.get(randInt(input.size()));
        } else {
            return input.remove(randInt(input.size()));
        }
    }

    @Override
    public int compareTo(Gram other) {
        return Double.compare(getScore(), other.getScore());
    }

    public String prettyPrintRules(List<Rule> rules) {
        return rules.stream().map(Rule::toString).collect(Collectors.joining("\n"));
    }

    public String getLastMutation() {
        String[] splitArr = getName().split("_");
        return splitArr[splitArr.length - 2];
    }

    public static int getTotalSelectables(List<Rule> prod) {

        int out = prod.stream().mapToInt(Rule::getTotalSelectables).sum();
        return out;
    }

    public static int getTotalSelectables(Rule mainRule) {
        // we use a base value of 1 so that the LHS of the rule can be selected as well
        return 1 + mainRule.getSubRules().stream().mapToInt(Gram::getTotalSelectables).sum();
    }

    public void setSelectable(List<Rule> prod, int index, Rule toSet) {
        int startIndex = index;
        toSet = toSet.equals(Rule.EPSILON) ? Rule.EPSILON() : toSet.makeMinorCopy();
        for (int i = 0; i < prod.size(); i++) {
            if (index == 0) {
                Rule target = prod.get(i);
                // System.out.println(String.format("setSelectable(%d, %s, %s) replaces
                // %s",startIndex, toSet, stringProd(prod), target));
                prod.set(i, toSet);
                return;
            } else {

                Rule currRule = prod.get(i);
                int currRuleProds = currRule.getTotalSelectables();

                if (index < currRuleProds) {
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

            if (index == 0) {
                prod.add(i, toInsert);
                return;
            } else {
                Rule currRule = prod.get(i);
                int currRuleProds = currRule.getTotalSelectables();

                if (index < currRuleProds) {
                    List<Rule> currRules = currRule.getSubRules().get(0);
                    insertSelectable(currRules, index - 1, toInsert);
                    return;
                } else {
                    index -= currRuleProds;
                }
            }
        }
    }

    public static void appendSelectable(List<Rule> prod, Rule toInsert) {
        toInsert = toInsert.equals(Rule.EPSILON) ? Rule.EPSILON() : toInsert.makeMinorCopy();
        prod.add(toInsert);

    }

    /**
     * Removes the rule at index in prod index should be calculated with
     * randInt(getTotalSelectables(prod))
     * 
     * @param prod
     * @param index
     */
    public static void removeSelectable(List<Rule> prod, int index) {
        int startIndex = index;
        for (int i = 0; i < prod.size(); i++) {
            if (index == 0) {
                // System.err.println("removeSelectable " + index + stringProd(prod) + " removes
                // " + prod.get(i));
                prod.remove(i);
                return;
            } else {

                Rule rule = prod.get(i);
                int currRuleProds = rule.getTotalSelectables();

                if (index < currRuleProds) {
                    List<Rule> currProd = rule.getSubRules().get(0);
                    removeSelectable(currProd, index - 1);
                    if (rule.getTotalSelectables() == 2)
                        rule.setSingular(true);
                    return;
                } else {
                    index -= currRuleProds;
                }
            }
        }
    }

    public static Rule getSelectable(List<Rule> prod, int index) {
        int startIndex = index;
        for (int i = 0; i < prod.size(); i++) {
            if (index == 0) {
                Rule out = prod.get(i);
                // System.err.println("getSelectable " + startIndex + stringProd(prod) + "
                // returns " + out);
                return prod.get(i);
            } else {
                Rule rule = prod.get(i);
                int currRuleProds = rule.getTotalSelectables();

                if (index < currRuleProds) {
                    List<Rule> currSelectables = rule.getSubRules().get(0);
                    return getSelectable(currSelectables, index - 1);
                } else {
                    index -= currRuleProds;
                }
            }
        }
        System.err.println("getSelectable " + startIndex + stringProd(prod) + " returns null");
        return null;
    }

    /**
     * Determines if the grouping mutation can be applied to some list of rules if
     * there are more than 2 rules or if the sole rule is composite it must have at
     * least 2subrules so we can group, otherwise false;
     * 
     * @param prod
     * @return
     */
    public static boolean canGroup(List<Rule> prod) {
        boolean out = prod.size() > 1;

        if (out) {
            // System.err.println(stringProd(prod) + " has more than 1 element and can be
            // grouped");
            return out;
        }
        boolean fallback = prod.get(0).getTotalSelectables() == 2;
        if (fallback)
            prod.get(0).setSingular(true);
        // System.err.println("Fallback for " + stringProd(prod) + " is " + fallback);
        // System.err.println("Returning " + !prod.get(0).isSingular());
        return fallback;
    }

    /**
     * Determines if the heuristic mutation can be applied to prod true if there is
     * a rule in prod that is not nullable
     * 
     * @param prod
     * @return
     */
    public boolean canHeur(List<Rule> prod) {
        List<String> nullables = constrNullable();
        return !prod.stream().allMatch(rule -> (nullables.contains(rule.getName()) || rule.equals(Rule.EPSILON)));

    }

    /**
     * Determines if a grammar contains a production that can be grouped first the
     * nullable symbols are calculated and all productions are checked to see if
     * they contain a rule not in the nullable list
     */
    public static boolean canHeur(Gram myGram) {
        List<String> nullables = myGram.constrNullable();
        return !myGram.parserRules.stream().map(Rule::getSubRules).flatMap(ArrayList::stream).allMatch(myGram::canHeur);

    }

    /**
     * Determines if the ungrouping mutation can be applied to some list of rules
     * only return false if all of the rules are singular
     * 
     * @param prod
     * @return
     */
    public static boolean canUngroup(List<Rule> prod) {
        return !prod.stream().allMatch(Rule::isSingular);

    }

    public static String stringProd(List<Rule> prod) {
        return "len: " + prod.size() + "| " + prod.stream().map(rule -> {
            return rule.toString() + " ("
                    + (rule.isSingular() ? "S" : rule.getTotalSelectables() + " [" + rule.stringSelectables() + "]")
                    + ") ";
        }).collect(Collectors.joining(" ")) + " |";
    }

    public void logGrammar(boolean logMutHist) {
        try (FileWriter out = new FileWriter(new File(Constants.LOG_GRAMMAR_PATH + grammarName + ".g4"))) {
            out.write(hashString() + "\n");
            out.write(prettyPrintRules(terminalRules));
            out.write("\n" + mutHist.stream().collect(Collectors.joining("\n")));
        } catch (Exception e) {

        }
    }

    public String fullHashString() {
        return hashString() + '\n' + prettyPrintRules(terminalRules);
    }

    /**
     * Computes which rules can be reached from the start rule
     * 
     * @return List of rule names that are reachable
     */
    public List<String> getReachables() {
        List<String> out = new ArrayList<>();
        String startSymbol = getStartSymbol();
        out.add(startSymbol);
        HashMap<String, List<String>> reachables = new HashMap<String, List<String>>();
        getAllRules().forEach(rule -> reachables.put(rule.getName(), rule.getReachables()));
        List<String> workList = reachables.get(startSymbol);
        while (!workList.isEmpty()) {
            workList.removeIf(out::contains);
            List<String> newWorkers = new ArrayList<String>();
            try {
                workList.stream().peek(out::add).map(reachables::get).flatMap(List::stream)
                        .filter(ruleName -> !(out.contains(ruleName) || workList.contains(ruleName)))
                        .peek(name -> System.err.println("Adding " + name + " to work list")).forEach(newWorkers::add);
            } catch (Exception e) {
                System.err.println(workList);
                System.err.println(newWorkers);
                e.printStackTrace();
                System.exit(1);
            }

            workList.addAll(newWorkers);
        }

        return out;
    }

    public List<String> getRulesReferenced(List<Rule> prod) {
        List<String> out = new LinkedList<>();
        prod.forEach(rule -> {
            if (rule.isSingular() && !out.contains(rule.getName())) {
                out.add(rule.getName());
            } else {
                out.addAll(getRulesReferenced(rule.getSubRules().get(0)));
            }
        });
        return out;
    }

    public int compPosPassSimil(Gram g0, Gram g1) {
        return Double.compare(posPassSimilarity(g0.passPosArr), posPassSimilarity(g1.passPosArr));
    }

    public int compNegPassSimil(Gram g0, Gram g1) {
        return Double.compare(negPassSimilarity(g0.passPosArr), negPassSimilarity(g1.passPosArr));
    }

    public int compAllPassSimil(Gram g0, Gram g1) {
        return Double.compare(allPassSimilarity(g0.passPosArr, g0.passNegArr),
                allPassSimilarity(g1.passPosArr, g1.passNegArr));
    }

    public static int CompPosScoreDelta(Gram g0, Gram g1) {
        return Double.compare(g0.posScoreDelta, g1.posScoreDelta);

    }

    public static int CompNegScoreDelta(Gram g0, Gram g1) {
        return Double.compare(g0.negScoreDelta, g1.negScoreDelta);

    }

    public static int CompAllScoreDelta(Gram g0, Gram g1) {
        return Double.compare(g0.getScore(), g1.getScore());
    }

    public void setTestSizes(int size, int size2) {
        passPosArr = new Boolean[size];
        passNegArr = new Boolean[size2];
    }

    public void setNegPass(Boolean[] vals) {
        passNegArr = vals;
    }

    public void setPosPass(Boolean[] vals) {
        passPosArr = vals;
    }

    public double posPassSimilarity(Boolean[] otherPos) {
        double out = 0.0;
        for (int i = 0; i < otherPos.length; i++) {
            if (otherPos[i] == passPosArr[i])
                out++;
        }
        out /= otherPos.length;
        return out;
    }

    public double negPassSimilarity(Boolean[] otherNeg) {
        double out = 0.0;
        for (int i = 0; i < otherNeg.length; i++) {
            if (otherNeg[i] == passNegArr[i])
                out++;
        }
        out /= otherNeg.length;
        return out;
    }

    public double allPassSimilarity(Boolean[] otherPos, Boolean[] otherNeg) {
        return (posPassSimilarity(otherPos) + negPassSimilarity(otherNeg)) / 2.0;
    }

    public void waitForInput(String message) {
        System.err.println(message);
        try {
            System.in.read();

        } catch (Exception e) {

        }
    }

    public List<String> getAllSuggestions() {
        List<String> all = new ArrayList<>();
        parserRules.forEach(rule -> {
            String currRule = rule.getName();
            int numProds = rule.getSubRules().size();
            for (int i = 1; i < numProds + 1; i++) {
                all.add(currRule + ":" + i);
            }
        });

        return all;

    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Gram)) {
            return false;
        }

        Gram c = (Gram) o;
        return hashString().equals(c.hashString());
    }

    @Override
    public int hashCode() {
        return hashString().hashCode();
    }

    public static Predicate<Gram> postTestFilter() {
        if (Constants.ANY_PASS_POS) {
            return passesAnyTest;
        } else {
            return passesPosTest;
        }
    }

    public long numPassPos() {
        return Arrays.stream(passPosArr).filter(a -> a).count();
    }

    public long numPassNeg() {
        return Arrays.stream(passNegArr).filter(a -> !a).count();
    }

    public void genFakeSuggestions() {
        List<String> toPass = new LinkedList<String>();
        for (int i = 0; i < NUM_SUGGESTED_MUTANTS; i++) {
            System.err.println("Computing mutant " + i + "/" + NUM_SUGGESTED_MUTANTS);
            Rule targetRule = randGet(parserRules, true);
            String toAdd = targetRule.getName() + ":" + (1 + randInt(targetRule.getSubRules().size()));
            // while(toPass.contains(toAdd))toAdd = targetRule.getName() + ":" +
            // (1+randInt(targetRule.getSubRules().size()));
            toPass.add(toAdd);
        }
        setMutationConsideration(toPass);

    }

    public void initMutHist(String[] mutSuggestion) {
        mutHist.clear();
        mutHist.add("Start of mutations for \n" + this + "\nScore going in:" + getScore() + "\n" + " Using key "
                + Arrays.toString(mutSuggestion));
    }

    // Removes closures from rules that are nullable
    public void cleanEmptyClosure(List<Rule> targetProd) {
        List<String> nullables = constrNullable();
        getAllSelectables(targetProd).stream().filter(rule -> !rule.isSingular()).forEach(rule -> {
            if (rule.nullable(nullables) && (rule.isIterative() || rule.isOptional())) {
                mutHist.add(rule + " is nullable and a closure\n");
                rule.setIterative(false);
                rule.setOptional(false);
            }
        });
    }

    public void cleanEmptyClosure(List<Rule> targetProd, List<String> nullables) {
        getAllSelectables(targetProd).stream().filter(rule -> !rule.isSingular()).forEach(rule -> {
            if (rule.nullable(nullables) && (rule.isIterative() || rule.isOptional())) {
                mutHist.add(rule + " is nullable and a closure\n");
                rule.setIterative(false);
                rule.setOptional(false);
            }
        });
    }

    public void cleanAllEmptyClosures() {
        List<String> nullables = constrNullable();
        parserRules.forEach(rule -> rule.getSubRules().forEach(prod -> cleanEmptyClosure(prod, nullables)));
    }

    public static List<Rule> getAllSelectables(List<Rule> targetProd) {
        return targetProd.stream().map(Rule::getAllSelectables).flatMap(List::stream)
                .collect(toCollection(ArrayList::new));
    }

    public static List<Rule> getAllFirstSingWOptional(Rule targetRule, List<String> nullables) {
        return targetRule.getFirstSingWOptional(nullables);
    }

    // public int mostLikelyIndex(Rule targetRule) {
    //     HashMap <String, Integer> hits = new HashMap<String, Integer>();
    //     parserRules.forEach(rule -> hits.put(rule.getName(), 0));
    //     List<List<Rule>> allProds = parserRules.stream().map(Rule::getSubRules)
    //                                 .flatMap(ArrayList<LinkedList<Rule>>::stream)
    //                                 .collect(toList());

    //     int maxLen = allProds.stream().map(List::size).max();
    //     int[] hits = new int[maxLen];
    //     List<List<String>> namedProds = allProds.stream().map(rules -> rules.stream().map(rule -> rule.name).collect(toList())).collect(toList());
    //     namedProds.forEach(prod -> prod.stream().forEach(name -> hits[prod.indexOf(name)]++));
        
    //             // .map(prod -> {
    //             //     prod.stream().map(rule -> rule.name)
    //             //     .forEach(name -> hits.replace(name, hits.get(name)+1));
    //             //     return 0;
    //             // });

    //     return hits.keySet().stream().map(key -> key + " " + hits.get(key)).
    //     max(Comparator.comparing(pair -> Integer.parseInt(pair.split(" ")[1])));
        
    // }

    public <T> List<String> zip(List<T> in) {
        ArrayList<String>  out = new ArrayList<>();
        for (int i = 0; i < in.size(); i++) {
            out.add(in.get(i) + "," + i);
        }
        return out;
    }

    public boolean nullable(Rule toCheck) {
        List<String> nullables = constrNullable();
        if(toCheck.isSingular()) return nullables.contains(toCheck.name);
        return toCheck.getSubRules().stream().allMatch(this::nullable);
    }
    public boolean nullable(Rule toCheck, List<String> nullables) {
        if(toCheck.isSingular()) return nullables.contains(toCheck.name);
        return toCheck.getSubRules().stream().allMatch(this::nullable);
    }

    public boolean containsNullClosure() {
        List<String> nullables = constrNullable();

        List<Rule> list = parserRules.stream().map(Rule::getSubRules).flatMap(ArrayList<LinkedList<Rule>>::stream)
                        .map(Gram::getAllSelectables).flatMap(List<Rule>::stream)
                        .filter(rule -> rule.isOptional() || rule.isIterative())
                        .filter(rule -> nullable(rule, nullables))
                        .collect(toList());
                        
        // System.err.println("Nullables " +  list.stream().map(Rule::toString).collect(Collectors.joining("\n")));
        
        boolean out = list.size() > 0;

        // System.err.println(this + "\ncontains " + (out ? " a nullable closure" : " no nullable closures"));
        return out;
    }


}