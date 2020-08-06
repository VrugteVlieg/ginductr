package stb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Rule {
    private String name;
    private ArrayList<LinkedList<Rule>> subRules = new ArrayList<LinkedList<Rule>>();
    private boolean terminal;
    private boolean optional;
    private boolean iterative;
    private boolean mainRule;
    private boolean singular;
    private String ruleText;

    public static final Rule EPSILON = new Rule(" ");

    public static Rule EPSILON() {
        Rule out = new Rule(" ");


        return out;
    }

    /**
     * Creates a deep-copy of a rule
     * 
     * @param toCopy
     */
    public Rule(Rule toCopy) {
        try {
            this.name = toCopy.name;
            this.subRules = new ArrayList<LinkedList<Rule>>();
            for (LinkedList<Rule> prod : toCopy.subRules) {
                LinkedList<Rule> newList = new LinkedList<Rule>();
                prod.forEach(rule -> {
                    newList.add(new Rule(rule));
                });
                subRules.add(newList);
            }
            this.terminal = toCopy.terminal;
            this.optional = toCopy.optional;
            this.iterative = toCopy.iterative;
            this.mainRule = toCopy.mainRule;
            this.singular = toCopy.singular;
            this.ruleText = toCopy.ruleText;
        } catch (NullPointerException e) {
            e.printStackTrace(System.out);
        }
    }

    /**
     * Creates a rule that collects a list of sub rules used when grouping rules
     * together
     * 
     * @param rules
     */
    public Rule(List<Rule> rules) {
        this.subRules = new ArrayList<LinkedList<Rule>>();
        LinkedList<Rule> myRules = new LinkedList<Rule>();
        rules.forEach(rule -> myRules.add(rule));
        subRules.add(myRules);
        terminal = false;
        mainRule = false;
        iterative = false;
        optional = false;
        StringBuilder nameBuilder = new StringBuilder();
        rules.forEach(rule -> nameBuilder.append(rule.getName() + " "));
        name = nameBuilder.toString().trim();
        ruleText = getName();

    }

    /**
     * Creates a minor rule, these are rules inside of major rules
     * 
     * @param ruleText
     */
    public Rule(String ruleText) {
        if (!ruleText.equals(" ")) {
            ruleText = ruleText.trim();
            singular = !ruleText.contains(" ");
            subRules = new ArrayList<LinkedList<Rule>>();
        } else {
            this.name = ruleText;
            singular = true;
        }
        this.name = ruleText;
        this.ruleText = ruleText;
        printOut("New minor rule " + ruleText);
        this.mainRule = false;

        optional = ruleText.charAt(ruleText.length() - 1) == '?' || ruleText.charAt(ruleText.length() - 1) == '*';
        iterative = ruleText.charAt(ruleText.length() - 1) == '+' || ruleText.charAt(ruleText.length() - 1) == '*';

        // Strips away the additional information associated with the rule so the
        // subrules can be parsed
        if (optional || iterative) {
            ruleText = ruleText.substring(0, ruleText.length() - 1);
        }
        if (ruleText.charAt(0) == '(') {
            ruleText = ruleText.substring(1, ruleText.length() - 1);
        }
        if (!ruleText.equals(" "))
            this.name = ruleText.trim();

        if (singular)
            terminal = Character.isUpperCase(name.charAt(0));
        if (!singular) {
            terminal = false;
            addRuleLookahead(ruleText);
        }
    }

    /**
     * Creates major rule from text, this is a rule which is present in thee EBNF
     * with a LHS
     * 
     * @param name     LHS of the rule
     * @param ruleText RHS of the rule
     */
    public Rule(String name, String ruleText) {
        ruleText = ruleText.trim();
        this.ruleText = ruleText;
        name = name.trim();
        this.name = name;
        singular = true;
        printOut("New major rule " + name + " : " + ruleText);
        mainRule = true;
        terminal = Character.isUpperCase(name.charAt(0));
        optional = name.charAt(name.length() - 1) == '?' || name.charAt(name.length() - 1) == '*';
        iterative = name.charAt(name.length() - 1) == '+' || name.charAt(name.length() - 1) == '*';
        addRuleLookahead(ruleText);
    }

    /**
     * Parses rule according to the ANTLR format used in g4 files to build a rule
     * object
     * 
     * @param rule
     */
    public void addRuleLookahead(String rule) {
        int index = 0;
        if (!mainRule)
            rule = rule + ";"; // adds ; to the end of the rule text to make parsing easier
        LinkedList<Rule> currProduction = new LinkedList<Rule>();
        StringBuilder currString = new StringBuilder();
        boolean StringLiteral = false;
        int brackets = 0;
        while (index < rule.length()) {
            printOut(rule.charAt(index) + " -> " + currString.toString().replaceAll(" ", "_SPACE_"));
            switch (rule.charAt(index)) {
                case ' ':
                    if (currString.length() != 0 && !StringLiteral && brackets == 0) {
                        printOut("Adding rule " + currString.toString());
                        currProduction.add(new Rule(currString.toString()));
                        currString = new StringBuilder();
                    } else if (StringLiteral || brackets != 0 || rule.charAt(index + 1) == ';') {
                        currString.append(" ");
                    }
                    break;

                case '|':
                    if (StringLiteral) {
                        currString.append("|");
                    } else {
                        subRules.add(currProduction);
                        currProduction = new LinkedList<Rule>();
                    }
                    break;

                case ';':

                    if (StringLiteral) {
                        currString.append(";");
                    } else {
                        if (currString.length() != 0) {
                            if (currString.toString().equals(" ")) {
                                printOut("Adding EPSILON");
                                currProduction.add(EPSILON());
                            } else {
                                currProduction.add(new Rule(currString.toString()));
                            }
                            currString = new StringBuilder();
                        }
                        subRules.add(currProduction);
                    }
                    break;

                case '\'':
                    StringLiteral = !StringLiteral;
                    currString.append("\'");
                    break;

                case '(':
                    if (!StringLiteral)
                        brackets++;
                    currString.append("(");
                    break;

                case ')':
                    if (!StringLiteral)
                        brackets--;
                    currString.append(")");
                    break;
                    
                default:
                    currString.append(rule.charAt(index) + "");
                    break;
            }
            index++;
        }
    }

    /**
     * @return The RHS of the rule
     */
    public String getRuleText() {
        StringBuilder out = new StringBuilder();
        subRules.forEach(prod -> {
            prod.forEach(rule -> {
                out.append(rule + " ");
                // if(!rule.equals(EPSILON)) out.append(rule + " ");
            });
            out.append("| ");
        });
        if (out.lastIndexOf("| ") != -1)
            out.delete(out.length() - 2, out.length());
        out.append(";");
        return out.toString();
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) {
            return true;
        }

        if (!(o instanceof Rule)) {
            return false;
        }

        Rule c = (Rule) o;
        return this.name.equals(c.name);

    }

    public String toString() {
        if (!mainRule) {
            return getName();
        }
        StringBuilder output = new StringBuilder();
        output.append(getName() + " : ");

        output.append(getRuleText());

        return output.toString();
    }

    /**
     * Computes the display name of this rule, appends optional/iterative symbol and
     * wraps subrules in () if this is a compound rule
     * 
     * @return
     */
    String getName() {
        if (name.equals(" "))
            return Constants.DEBUG ? "EPSILON" : " "; // this is nullToken
        StringBuilder out = new StringBuilder();
        if (isSingular() || mainRule) {
            out.append(name);
        } else {
            for (LinkedList<Rule> prod : subRules) {
                for (int i = 0; i < prod.size(); i++) {
                    if (i != prod.size() - 1) {
                        out.append(prod.get(i) + " ");
                    } else {
                        out.append(prod.get(i).toString());
                    }
                }
            }
            if (out.toString().contains(" ")) {
                out.insert(0, "(");
                out.append(")");
            }
        }

        if (optional && iterative) {
            out.append("*");
        } else if (optional) {
            out.append("?");
        } else if (iterative) {
            out.append("+");
        }
        return out.toString();
    }

    ArrayList<LinkedList<Rule>> getSubRules() {
        return subRules;
    }

    private void printOut(String toPrint) {
        // if(Constants.DEBUG)System.out.println(" ".repeat(depth) + "Context " + name +
        // ": " + toPrint);
    }
    //TODO: these setting functions should roll to apply recursively to subrules
    public void setIterative(boolean setTo) {
        StringBuilder out = new StringBuilder(getName());
        iterative = setTo;
        out.append(" -> " + getName());
        printOut(out.toString());
    }

    public boolean isIterative() {
        return iterative;
    }

    public boolean isOptional() {
        return optional;
    }

    public boolean isSingular() {
        // System.out.println(subRules.stream().map(list -> list.size() + " ").collect(Collectors.joining("_")));
        // return subRules.stream().anyMatch(list -> list.size() > 1);
        return singular;
    }

    public void setOptional(boolean setTo) {
        this.optional = setTo;
    }

    public boolean isTerminal() {
        return this.terminal;
    }

    /**
     * Sets a given production to the specified rules indices for compound rules
     * defined to include the compound rule as well as its constituent rules so for
     * term: (factor expr); 0: term 1: (factor expr) 2: factor 3: expr
     * 
     * @param index
     * @param toSet
     */
    public void setSelectable(int index, Rule toSet) {

        Rule test = toSet.equals(EPSILON) ? EPSILON() : new Rule(toSet);
        if (test.mainRule) {
            test.mainRule = false;
            test.singular = true;
        }
        int counter = 0;
        int subSetIndex = 0;
        for (LinkedList<Rule> subSet : subRules) {
            int subRuleIndex = 0;
            for (Rule subRule : subSet) {
                int counterPre = counter;
                if (counterPre == index - 1) {
                    subRules.get(subSetIndex).set(subRuleIndex, test);
                    // if(!mainRule)resetName();
                    return;
                } else {
                    counter += subRule.getTotalSelectables();
                    if (counter >= index) { // the rule to change was in the the last subrule
                        int indexInRule = index - counterPre - 1; // position withing the rule
                        subRule.setSelectable(indexInRule, test);

                        printOut(subRules.toString());
                    }
                }
                subRuleIndex++;
            }
            subSetIndex++;
        }
    }

    /**
     * Returns the production at a specific index in the rule indices for compound
     * rules defined to include the compound rule as well as its constituent rules
     * so for term: (factor expr); 0: term 1: (factor expr) 2: factor 3: expr
     * 
     * @param index
     * @param toSet
     */
    public Rule getSelectable(int index) {
        if (index == 0)
            return this;
        int counter = 0;
        int subSetIndex = 0;
        for (LinkedList<Rule> subSet : subRules) {
            int subRuleIndex = 0;
            for (Rule subRule : subSet) {
                int counterPre = counter;
                if (counterPre == index - 1) {
                    return subRules.get(subSetIndex).get(subRuleIndex);
                } else {
                    counter += subRule.getTotalSelectables();
                    if (counter >= index) { // the rule to change was in the the last subrule
                        int indexInRule = index - counterPre - 1; // position withing the rule
                        return subRule.getSelectable(indexInRule);
                    }
                }
                subRuleIndex++;
            }
            subSetIndex++;
        }

        return null;
    }

    /**
     * Returns the total number productions that can be changed by a mutation
     * Expands out bracketed rules to allow selection of either the whole rule or
     * any of its subrules
     */
    public int getTotalSelectables() {
        if (isSingular() && !mainRule) {
            return 1;
        }
        int out = 1; // always start at 1 so the rule itself can be selected as well

        for (LinkedList<Rule> subSet : subRules) {
            for (Rule subRule : subSet) {
                out += subRule.getTotalSelectables();
            }
        }
        if(out == 2) return 1;
        return out;
    }

    /**
     * 
     */

    public int getTotalSelectables(int prodIndex) {
        int out = 0;
        if (isSingular() && !mainRule) {
            return out;
        }

        for (Rule subRule : subRules.get(prodIndex)) {
            out += subRule.getTotalSelectables();
        }

        return out;
    }

    /**
     * Adds the subRules of another rule to this rule
     * 
     * @param toAdd
     */
    public void addAlternative(ArrayList<LinkedList<Rule>> toAdd) {
        this.subRules.addAll(toAdd);
    }

    /**
     * Adds a single rule as an alternative production to this rule
     */
    public void addAlternative(Rule toAdd) {
        LinkedList<Rule> wrappedToAdd = new LinkedList<Rule>();
        wrappedToAdd.add(toAdd);
        this.subRules.add(wrappedToAdd);
    }

    /**
     * Compute the rule names that can be reached from this rule, recursively calls getReachables on rules that are composite
     */
    public List<String> getReachables() {
        List<String> out = new ArrayList<String>();
        if(this.equals(EPSILON)) return out;
        if(getTotalSelectables() == 1) {
            out.add(getName());
            return out;
        }

        subRules.stream()
            .flatMap(LinkedList::stream)
            .distinct()
            .map(Rule::getReachables)
            .forEach(out::addAll);

        return out;
    }

    public void getReachables(ArrayList<Rule> parserRules, ArrayList<String> reachables) {
        if (reachables.contains(getName()) || terminal)
            return;
        if (isSingular() && !terminal && !parserRules.contains(this)) {
            reachables.add("Undefined " + getName());
            return;
        }
        if (mainRule) {
            reachables.add(getName());
            subRules.forEach(prod -> {
                prod.forEach(rule -> {
                    rule.getReachables(parserRules, reachables);
                });
            });
        } else if (isSingular() && !mainRule) {
            int parserIndex = parserRules.indexOf(this);
            if (parserIndex == -1)
                System.out.println(getName() + " not present in " + prettyPrintRules(parserRules));
            parserRules.get(parserIndex).getReachables(parserRules, reachables);
            return;
        } else if (!isSingular()) {
            subRules.forEach(prod -> {
                prod.forEach(rule -> {
                    rule.getReachables(parserRules, reachables);
                });
            });
        }
    }



    
    public ArrayList<String> getReachables(ArrayList<Rule> parserRules) {
        ArrayList<String> reachables = new ArrayList<String>();
        getReachables(parserRules, reachables);
        return reachables;
    }

    public boolean nullable(LinkedList<String> nullableNames) {
        if (terminal)
            return false;

        if (nullableNames.contains(getName()))
            return true;

        if (isSingular() && !nullableNames.contains(getName()))
            return false;

        if (mainRule) {
            if (subRules.stream().anyMatch(prod -> prod.stream().allMatch(rule -> rule.nullable(nullableNames)))) {
                nullableNames.add(getName());
                return true;
            } else {
                return false;
            }
        } else if (!isSingular()) { // grouped rule
            return subRules.get(0).stream().allMatch(rule -> rule.nullable(nullableNames));
        } else {
            return true;
        }
    }

    public boolean containsEpsilon() {
        // System.err.println("Searching for Epsilon in " + this + " " + this.subRules.size());
        return subRules.stream().map(LinkedList::getFirst).map(EPSILON::equals).reduce(false,
                (prev, next) -> prev || next);
    }

    public void setSubRules(ArrayList<LinkedList<Rule>> toSet) {
        subRules.clear();
        toSet.forEach(prod -> {
            LinkedList<Rule> newProd = new LinkedList<Rule>();
            prod.forEach(rule -> newProd.add(rule));
            subRules.add(newProd);
        });
    }

    /**
     * Removes rules that consist entirely of left recursive productions term: term
     * | factor; -> term: factor;
     */
    public void removeSimpleLeftRecursives() {
        subRules.removeIf(subRule -> (subRule.size() == 1 && subRule.getFirst().getName().equals(getName())));
    }

    /**
     * Replaces repeated left recursive productions with a single one to be clean up
     * by other functions term: term term factor | term term; -> term: term factor |
     * term;
     */
    public void simplifyRepeatingLR() {
        // System.out.println("simplifyRepeatingLR " + subRules);
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> currRule = subRules.get(i);
            if (currRule.getFirst().getName().equals(getName())) {
                while (currRule.size() > 1 && currRule.get(1).getName().equals(getName()))
                    currRule.removeFirst();
            }
        }
        subRules.removeIf(list -> list.size() == 0);
    }

    public boolean containLeftRecursiveProd() {
        for (LinkedList<Rule> prod : subRules) {
            try {
                if (prod.getFirst().getName().equals(getName())) {
                    // System.out.println(this.getName() + " contains a left recursive production "
                    // + prod);
                    return true;
                }
            } catch (NoSuchElementException e) {
                System.err.println(prod + " in " + this);
            }
        }
        // System.out.println(this.getName() + " does not contains a left recursive
        // production ");
        return false;
    }

    public void removeEpsilon() {
        subRules.removeIf(prod -> prod.getFirst().equals(EPSILON));
    }

    public void addEpsilon() {
        LinkedList<Rule> toAdd = new LinkedList<>();
        toAdd.add(EPSILON());
        subRules.add(toAdd);
    }

    /**
     * Attempts to group rules on the RHS of this rule, returns true if successful
     * else returns false this can fail if the rule does not contain a sub
     * production with more than 1 symbol
     */
    public void groupProductions() {
        // if (!canGroup())
        //     return;

        LinkedList<Rule> prodToGroup = randGet(subRules);
        while (prodToGroup.size() < 2)
            prodToGroup = randGet(subRules);
        int startIndex = prodToGroup.size() == 2 ? 0 : randInt(prodToGroup.size() - 2);
        int endIndex = startIndex + randInt(prodToGroup.size() - startIndex - 1) + 1;
        List<Rule> toRemove = prodToGroup.subList(startIndex, endIndex + 1);
        Rule newRule = new Rule(toRemove);
        for (int i = startIndex; i < endIndex; i++) {
            prodToGroup.remove(startIndex);
        }

        prodToGroup.set(startIndex, newRule);
    }

    /**
     * Attempts to expand out a bracketd rule into its constituent rules
     */
    public void unGroupProductions() {
        if (this.equals(EPSILON))
            return;
        // Compute expandable indices
        LinkedList<Rule> expandables = new LinkedList<Rule>();
        LinkedList<int[]> indices = new LinkedList<int[]>();
        int prodIndex = 0;
        for (LinkedList<Rule> prods : subRules) {
            int ruleIndex = 0;
            for (Rule rule : prods) {
                if (rule.name.contains(" ") && !rule.equals(EPSILON)) {
                    expandables.add(rule);
                    int[] index = { prodIndex, ruleIndex };
                    indices.add(index);
                }
                ruleIndex++;
            }
            prodIndex++;
        }
        int toExpandIndex = randInt(expandables.size());
        Rule toExpand = expandables.get(toExpandIndex);
        int[] index = indices.get(toExpandIndex);

        // 50% of the time we try to expand a subrule, if there are no expandable
        // subrules ungroup will return false and the not will become true
        if (!(Math.random() < 0.5 && toExpand.canUngroup())) {
            List<Rule> toInsert = toExpand.subRules.get(0);
            LinkedList<Rule> prod = subRules.get(index[0]);
            prod.remove(index[1]);
            Collections.reverse(toInsert);
            toInsert.forEach(rule -> prod.add(index[1], rule));
            subRules.set(index[0], prod);
        }
    }

    public boolean canUngroup() {
        if (isSingular())
            return false;
        for (LinkedList<Rule> prods : subRules) {
            for (Rule rule : prods) {
                if (!rule.isSingular())
                    return true;
            }
        }
        return false;
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * Changes all occurances of toClean in this rule to be mandatory and
     * non-iterative, used after a parserRule has epsilon added as alternative
     * 
     * @param toClean
     */
    public void cleanReferences(Rule toClean) {
        if (isSingular() && this.equals(toClean)) {
            setOptional(false);
            setIterative(false);
            return;
        }
        subRules.forEach(prod -> {
            prod.forEach(rule -> {
                if (rule.getName().contains(toClean.getName())) {
                    rule.setIterative(false);
                    rule.setOptional(false);
                    rule.cleanReferences(toClean);
                }
            });
        });
    }

    /**
     * Returns rule name or EPSILON if the rule is epsilon
     * 
     * @return
     */
    public String getDebugName() {
        if (this.equals(EPSILON))
            return "EPSILON";
        return getName();
    }

    /**
     * Inserts toAdd at a random position on the RHS of this rule Changes toAdd to
     * be a minor rule if it is not
     */
    public void extend(Rule toAdd) {
        if (toAdd.mainRule) {
            toAdd.mainRule = false;
            toAdd.singular = true;
        }

        LinkedList<Rule> prod = randGet(subRules);
        // If the selected production was epsilon leave it and add a new production
        if (prod.getFirst().equals(EPSILON)) {
            LinkedList<Rule> newProd = new LinkedList<Rule>();
            newProd.add(toAdd);
            subRules.add(newProd);
            return;
        }
        int ruleIndex = randInt(prod.size());
        Rule toShift = prod.get(ruleIndex);

        if (!toShift.isSingular()) {
            toShift.extend(toAdd);
        } else {
            prod.add(ruleIndex, toAdd);
        }
    }

    /**
     * Generates a new ruleName
     * 
     * @return
     */
    public static String genName() {
        StringBuilder ruleNameBuilder = new StringBuilder();
        // Generates rule name by randomly concatting letters
        for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
            ruleNameBuilder.append((char) ('a' + randInt(26)));
        }
        return ruleNameBuilder.toString();
    }

    /**
     * Replaces all references to oldRule with newRule, optional/iterative is
     * maintained unless newRule is nullable
     * 
     * @param newRuleNullable is newRule nullable in the context of the calling
     *                        grammar
     */
    public void replaceReferences(Rule oldRule, Rule newRule, boolean newRuleNullable) {
        if (isSingular() && !mainRule)
            return;
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> prod = subRules.get(i);
            for (int j = 0; j < prod.size(); j++) {
                Rule currRule = prod.get(j);
                if (currRule.equals(oldRule)) {
                    prod.set(j, newRule.makeMinorCopy());
                    prod.get(j).setIterative(currRule.iterative && !newRuleNullable);
                    prod.get(j).setOptional(currRule.optional && !newRuleNullable);
                } else {
                    currRule.replaceReferences(oldRule, newRule, newRuleNullable);
                }
            }
        }
    }

    /**
     * removes all references to toRemove
     */
    public void removeReferences(String toRemoveName) {
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> prod = subRules.get(i);
            prod.removeIf(rule -> rule.name.equals(toRemoveName));
            prod.forEach(rule -> {
                if (!rule.isSingular())
                    rule.removeReferences(toRemoveName);
            });
        }
        subRules.removeIf(prod -> prod.size() == 0);
    }

    /***
     * Computes the number of top level rules on the RHS of this
     * rule: (A B C) | A B; would count as 3
     * @return
     */
    public int getSymbolCount() {
        int out = 0;
        for (LinkedList<Rule> prod : subRules) {
            out += prod.size();
        }
        return out;
    }

    public boolean containsInfLoop(ArrayList<Rule> parserRules, String touchedRules) {
        if (containsEpsilon())
            return false;
        if (touchedRules.contains(getName()) && !containsEpsilon())
            return true;
        if (mainRule)
            touchedRules = touchedRules + "," + getName();
        for (LinkedList<Rule> prod : subRules) {
            for (Rule rule : prod) {
                if (rule.isSingular()) {
                    if (!rule.terminal) {
                        Rule mainVer = parserRules.get(parserRules.indexOf(rule));
                        if (mainVer.containsInfLoop(parserRules, touchedRules))
                            return true;
                    }
                } else {
                    if (rule.containsInfLoop(parserRules, touchedRules))
                        return true;
                }
            }
        }

        return false;
    }

    private String prettyPrintRules(List<Rule> rules) {
        return rules.stream().map(Rule::toString).collect(Collectors.joining("\n"));
    }

    public Rule makeMinorCopy() {
        Rule out = new Rule(this);
        out.mainRule = false;
        return out;
    }

    public boolean canGroupProd(int prodIndex) {
        return subRules.get(prodIndex).size() > 1;
    }

    public boolean canUngroupProd(int prodIndex) {
        for (Rule rule : subRules.get(prodIndex)) {
            if (!rule.isSingular())
                return true;
        }
        return false;
    }

    public <E> E randGet(List<E> input) {
        return input.get(randInt(input.size()));
    }


    public String stringSelectables() {
        System.err.println("Calling string selectables on " + this + (singular ? " singular " : "") + (mainRule ? " main " : "") + " selectables: " + getTotalSelectables());
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < getTotalSelectables(); i++) {
            Rule currRule = getSelectable(i);
            out.append(String.format("(%d: %s) ", i, currRule.toString()));
            
        }
        return out.toString();
    }

}
