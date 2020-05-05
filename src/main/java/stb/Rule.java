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
    private int depth = 0;
    private String ruleText;

    public static final Rule EPSILON =  new Rule(" ",1);
    

    /**
     * Creates a deep-copy of a rule
     * @param toCopy
     */
    public Rule(Rule toCopy) {
        this.name = toCopy.name;

        this.subRules = new ArrayList<LinkedList<Rule>>();
        for(LinkedList<Rule> prod : toCopy.subRules) {
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
        this.depth = toCopy.depth;
        this.ruleText = toCopy.ruleText;
    }
    /**
     * Creates a rule that collects a list of sub rules
     * used when grouping rules together
     * @param rules
     */
    public Rule(List<Rule> rules, int depth) {
        this.subRules = new ArrayList<LinkedList<Rule>>();
        LinkedList<Rule> myRules = new LinkedList<Rule>();
        rules.forEach(rule -> myRules.add(rule));
        subRules.add(myRules);
        terminal = false;
        mainRule = false;
        iterative = false;
        optional = false;
        this.depth = depth;
        StringBuilder nameBuilder = new StringBuilder();
        rules.forEach(rule -> nameBuilder.append(rule.getName() + " "));
        name = nameBuilder.toString().trim();
        ruleText = getName();

    }

    /**
     * Creates a minor rule, these are rules inside of major rules
     * @param ruleText
     * @param depth how many levels deep is this rule, used to format printOut
     */
    public Rule(String ruleText, int depth) {
        if(!ruleText.equals(" "))  {
            ruleText = ruleText.trim();
            singular = !ruleText.contains(" ");
            subRules = new ArrayList<LinkedList<Rule>>();
        } else {
            LinkedList<Rule> start = new LinkedList<Rule>();
            start.add(EPSILON);
            subRules.add(start);
            System.out.println("Adding EPSILON in "+ ruleText);
            singular = true;
        }
        this.name = ruleText;
        this.ruleText = ruleText;
        printOut("New minor rule " + ruleText);
        this.mainRule = false;
        this.depth = depth;

        // System.out.println(singular ? "it is singular " : " it is not singular");
        optional = ruleText.charAt(ruleText.length()-1) == '?' || ruleText.charAt(ruleText.length()-1) == '*';
        iterative = ruleText.charAt(ruleText.length()-1) == '+' || ruleText.charAt(ruleText.length()-1) == '*';
        
        //Strips away the additional information associated with the rule so the subrules can be parsed
        if(optional || iterative) {
            ruleText = ruleText.substring(0, ruleText.length()-1);
        }
        if(ruleText.charAt(0) == '(') {
            ruleText = ruleText.substring(1, ruleText.length()-1);
        } 
        if(!ruleText.equals(" ")) this.name = ruleText.trim();
        
        
        if(singular) terminal = Character.isUpperCase(name.charAt(0)) || this.name.equals(" "); 
        if(!singular) {
            terminal = false;
            addRuleLookahead(ruleText);
        }
    }

    /**
     * Creates major rule from text, this is a rule which is present in thee EBNF with a LHS
     * @param name LHS of the rule
     * @param ruleText RHS of the rule
     */
    public Rule(String name, String ruleText) {
        ruleText = ruleText.trim();
        this.ruleText = ruleText;
        name = name.trim();
        this.name = name;
        //This used to be true, might create a cascading effect somewhere
        singular = false;
        printOut("New major rule " + name + " : " + ruleText);
        mainRule = true;
        terminal = Character.isUpperCase(name.charAt(0));
        optional = name.charAt(name.length()-1) == '?' || name.charAt(name.length()-1) == '*';
        iterative = name.charAt(name.length()-1) == '+' || name.charAt(name.length()-1) == '*';
        addRuleLookahead(ruleText);
    }

    /**
     * Parses rule according to the ANTLR format used in g4 files to build a rule object
     * @param rule
     */
     public void addRuleLookahead(String rule) {
         int index = 0;
         if(!mainRule) rule = rule+";"; //adds ; to the end of the rule text to make parsing easier
         LinkedList<Rule> currProduction = new LinkedList<Rule>();
         StringBuilder currString = new StringBuilder();
         boolean StringLiteral = false;
         int brackets = 0;
         while(index < rule.length()) {
            printOut(rule.charAt(index) + " -> " + currString.toString().replaceAll(" ", "_SPACE_"));
            switch(rule.charAt(index)) {
               case ' ':
                    if(currString.length() != 0 && !StringLiteral && brackets==0) {
                        printOut("Adding rule " + currString.toString());
                        currProduction.add(new Rule(currString.toString(),depth+1));
                        currString = new StringBuilder(); 
                    } else if(StringLiteral || brackets!=0 || rule.charAt(index+1) == ';'){
                        currString.append(" ");
                    } 
                    break;
               
               case '|':
                    if(StringLiteral) {
                        currString.append("|");
                    } else {
                        subRules.add(currProduction);
                        currProduction = new LinkedList<Rule>();
                    }
                    break;

                case ';':
                    if(currString.length() != 0) {
                        if(currString.toString().equals(" ")) {
                            printOut("Adding EPSILON");
                            currProduction.add(EPSILON);
                        } else {
                            currProduction.add(new Rule(currString.toString(),depth+1));
                        }
                        currString = new StringBuilder(); 
                    } 
                    subRules.add(currProduction);
                    break;

               case '\'':
                    StringLiteral = !StringLiteral;
                    currString.append("\'");
                    break;
               case '(':
                    if(!StringLiteral)brackets++;
                    currString.append("(");
                    break;
               case ')':
                    if(!StringLiteral)brackets--;
                    currString.append(")");
                    break;
               default:
                   currString.append(rule.charAt(index) + "");
                   break;
            }
            index++;
         }
         printOut("Final subRules " + subRules.toString());
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
        if(out.lastIndexOf("| ") != -1) out.delete(out.length()-2, out.length());
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
        if(!mainRule) {
            return getName();
        }
        StringBuilder output = new StringBuilder();
        output.append(getName() + " : ");
        
       output.append(getRuleText());
       
        return output.toString();
    }
    /**
     * Computes the displat name of this rule, appends optional/iterative symbol and wraps subrules in () if this is a compound rule
     * @return
     */
    String getName() {
        if(name.equals(" ")) return Constants.DEBUG ? "EPSILON" : " "; //this is nullToken 
        StringBuilder out = new StringBuilder(name);
        if(name.contains(" ")) {
            out.insert(0, "(");
            out.append(")");
        }
        if(optional && iterative){
            out.append("*");
        } else if(optional) {
            out.append("?");
        } else if(iterative) {
            out.append("+");
        }
        return out.toString();
    }
    ArrayList<LinkedList<Rule>> getSubRules() {return subRules;}

    private void printOut(String toPrint) {
        if(Constants.DEBUG)System.out.println(" ".repeat(depth) + "Context " + name + ": " + toPrint);
    }

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
        return singular;
    }



    public void setOptional(boolean setTo) {
        this.optional = setTo;
    }
    public boolean isTerminal() {
        return this.terminal;
    }

    /**
     * Sets a given production to the specified rules
     * indices for compound rules defined to include the compound rule as well as its constituent rules
     * so for term: (factor expr);
     * 0: term
     * 1: (factor expr)
     * 2: factor
     * 3: expr
     * @param index
     * @param toSet
     */
    public void setProduction(int index, Rule toSet) {
        System.out.println("Making a copy of "  + toSet);
        Rule test = new Rule(toSet);
        test.mainRule = false;
        int counter = 0;
        int subSetIndex = 0;
        for(LinkedList<Rule> subSet : subRules) {
            int subRuleIndex = 0;
            for(Rule subRule : subSet) {
                int counterPre = counter;
                if(counterPre == index-1) {
                    subRules.get(subSetIndex).set(subRuleIndex, test);
                    if(!mainRule)resetName();
                    return;
                } else {
                    counter += subRule.getTotalProductions();
                    if(counter >= index) { //the rule to change was in the the last subrule
                        int indexInRule = index - counterPre - 1; //position withing the rule
                        subRule.setProduction(indexInRule, test);
                        
                        printOut(subRules.toString());
                    }
                }
                subRuleIndex++;
            }
            subSetIndex++;
        }
    }
     /**
     * Returns the production at a specific index in the rule
     * indices for compound rules defined to include the compound rule as well as its constituent rules
     * so for term: (factor expr);
     * 0: term
     * 1: (factor expr)
     * 2: factor
     * 3: expr
     * @param index
     * @param toSet
     */
    public Rule getProduction(int index) {
        if(index == 0) return this;
        int counter = 0;
        int subSetIndex = 0;
        // if(mainRule)System.out.println("Calling get production " + index + " on " + this);
        for(LinkedList<Rule> subSet : subRules) {
            int subRuleIndex = 0;
            for(Rule subRule : subSet) {
                int counterPre = counter;
                if(counterPre == index-1) {
                    // printOut("Setting " + subRules.get(subSetIndex).get(subRuleIndex) + " to " + test.name);
                    return subRules.get(subSetIndex).get(subRuleIndex);
                } else {
                    counter += subRule.getTotalProductions();
                    if(counter >= index) { //the rule to change was in the the last subrule
                        int indexInRule = index - counterPre - 1; //position withing the rule
                        return subRule.getProduction(indexInRule);
                    }
                }
                subRuleIndex++;
            }
            subSetIndex++;
        }

        return new Rule("Default", "Default");

    }
    /**
     * Recalculates the name of the rule, used after the content of the subRules Arraylist gets changed for minor rules
     * //TODO figure out if this should actually exist or not
     */
    public void resetName() {
        StringBuilder out = new StringBuilder();
        subRules.forEach(subProd -> {
            subProd.forEach(rule -> {
                out.append(rule + " ");
            });
        });
        printOut("Name set to " + out);
        this.name = out.toString().trim();
        this.ruleText = this.toString();
    }

    /**
     *  Returns the total number productions that can be changed by a mutation
     *  Expands out bracketed rules to allow selection of either the whole rule or any of its subrules
     */ 
	public int getTotalProductions() {
        int out = 1; //always start at 1 so the rule itself can be selected as well
        if(singular && !mainRule) {
            return out;
        }
		for(LinkedList<Rule> subSet : subRules) {
            for(Rule subRule : subSet) {
                out += subRule.getTotalProductions();
            }
        }
        return out;
    }
    
    /**
     * Adds the subRules of another rule to this rule 
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
     * Calculates which rules in parserRules can be reached from this rules, stores names of reachable rules in reachables
     * @param parserRules 
     * @param reachables
     */
	public void getReachables(ArrayList<Rule> parserRules, ArrayList<String> reachables) {
        if(reachables.contains(getName())) return;
        if(singular && !terminal && !parserRules.contains(this)) {
            //This is a singular rule that is not part of parserRules nor is it terminal so it is undefined
            reachables.add("Undefined " + name);
        }   else if(mainRule && !terminal) {
                //This is a main rule, add its name to reachable and check what can be reach from its productions
                reachables.add(getName());
                subRules.forEach(subRule -> {
                    subRule.forEach(production -> {
                        if(parserRules.contains(production)) {
                            parserRules.get(parserRules.indexOf(production)).getReachables(parserRules, reachables);
                        } else {
                            production.getReachables(parserRules, reachables);
                        }
                    });
                });
            } else {
                //This is a bracketed rule (Addop term) and each subRule is now being evalled
                subRules.forEach(subRule -> {
                    subRule.forEach(production -> {
                        if(parserRules.contains(production)) {
                            parserRules.get(parserRules.indexOf(production)).getReachables(parserRules, reachables);
                        } else {
                            production.getReachables(parserRules, reachables);
                        }
                    });
                });
            }
    }

    public ArrayList<String> getReachables(ArrayList<Rule> parserRules) {
        ArrayList<String> reachables =  new ArrayList<String>();
        getReachables(parserRules,reachables);
        return reachables;
    }


    /**
     * Determines if this rule is nullable in the context of a given set of Parserrules
     * @param parserRules
     * @param checkedRules
     * @return
     */
    private boolean nullable(ArrayList<Rule> parserRules, String checkedRules) {
        
        if(terminal || checkedRules.contains(getName())) return false;
        if(this.equals(EPSILON) || isOptional()) return true;
        
        if(!singular) { //composed rule (term factor)
            // System.out.println(getName() + " is not singular consists of " + subRules.get(0));
            LinkedList<Rule> subProds = subRules.get(0);
            for (Rule rule : subProds) {
                if(!rule.nullable(parserRules,checkedRules)) return false;
            }
            return true;
        } else if(!mainRule) { //non terminal on the RHS of a rule
            Rule mainVersion = parserRules.get(parserRules.indexOf(this));    
            return mainVersion.nullable(parserRules,checkedRules);
        } else {    //Main rule
            // System.out.println(this + " is a main rule, checking nullable");
            checkedRules = checkedRules + ","  + getName();
            if(containsEpsilon()) return true;
            
            for(LinkedList<Rule> prod : subRules) {
                boolean nullable = true;
                for(Rule rule : prod) {
                    nullable &= rule.nullable(parserRules,checkedRules);
                }
                if(nullable) return true;
            }
            return false;
        }
    }
    
    public boolean nullable(ArrayList<Rule> parserRules) {
        return nullable(parserRules, "");
    }

    public boolean containsEpsilon() {
        // System.out.println("Searching for EPSILON in " + subRules);
        for (LinkedList<Rule> prod : subRules) {
            // System.out.println("Checking " +  prod);
            // if(prod.getFirst().equals(EPSILON)) System.out.println("Found EPSILON returning true");;
            if(prod.getFirst().equals(EPSILON)) return true;
        }
        return false;
    }
    

    public void setSubRules(ArrayList<LinkedList<Rule>> toSet) {
        // System.out.println("Changing rules of " + this + " to " + toSet);
        subRules.clear();
        toSet.forEach(prod -> {
            LinkedList<Rule> newProd = new LinkedList<Rule>();
            prod.forEach(rule -> newProd.add(rule));
            subRules.add(newProd);
        });
    }
    /**
     * Removes rules that consist entirely of left recursive productions    term: term | factor; -> term: factor;
     */
    public void removeSimpleLeftRecursives() {
        subRules.removeIf(subRule -> (subRule.size() == 1 && subRule.getFirst().getName().equals(getName())));
    }
    
    /**
     * Replaces repeated left recursive productions with a single one to be clean up by other functions
     * term: term term factor | term term; -> term: term factor | term;
     */
    public void simplifyRepeatingLR() {
        // System.out.println("simplifyRepeatingLR " + subRules);
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> currRule = subRules.get(i);
            if(currRule.getFirst().getName().equals(getName())) {
                while(currRule.size() > 1 && currRule.get(1).getName().equals(getName())) currRule.removeFirst();
            }
        }
    }

    
    public boolean containLeftRecursiveProd() {
        for(LinkedList<Rule> prod : subRules) {
            try {
                if(prod.getFirst().getName().equals(getName())) {
                    // System.out.println(this.getName() + " contains a left recursive production " + prod);
                    return true;
                }
            } catch (NoSuchElementException e) {
                System.err.println(prod + " in " + this);
            }
        }
        // System.out.println(this.getName() + " does not contains a left recursive production ");
        return false;
    }

	public void removeEpsilon() {
        // System.out.println("Removing Epsilon from " + this);
        subRules.removeIf(prod -> prod.getFirst().equals(EPSILON));
	}
    /**
     * Attempts to group rules on the RHS of this rule, returns true if successful else returns false
     * this can fail if the rule does not contain a sub production with more than 1 symbol
     */
	public void groupProductions() {
        if(!canGroup()) return;

        LinkedList<Rule> prodToGroup = subRules.get(randInt(subRules.size()));
        while(prodToGroup.size() < 2) prodToGroup = subRules.get(randInt(subRules.size()));
        int startIndex = prodToGroup.size() == 2 ? 0 : randInt(prodToGroup.size()-2);
        int endIndex = startIndex + randInt(prodToGroup.size() - startIndex - 1) + 1;
        List<Rule> toRemove = prodToGroup.subList(startIndex, endIndex+1);
        // System.out.println("Making a new rule from " + toRemove);
        Rule newRule = new Rule(toRemove,depth+1);
        // System.out.println(getName() + " goes from \n" + subRules);
        // System.out.println("prodToGroup " + prodToGroup + " start " + startIndex + " end " + endIndex);
        for (int i = startIndex; i < endIndex; i++) {
            prodToGroup.remove(startIndex);
        }
        // prodToGroup.removeAll(toRemove);
        prodToGroup.set(startIndex, newRule);
        // System.out.println("To " + subRules);
    }

    public boolean canGroup() {
        for (LinkedList<Rule> prods : subRules) {
            if(prods.size() >= 2) return true;
        }
        return false;
    }

    /**
     * Attempts to expand out a bracketd rule into its constituent rules
     * returns true if a brackted rule was expanded else returns false
     */
    public void unGroupProductions() {
        if(this.equals(EPSILON)) return;
        // System.out.println("Running ungroup on " + this);
        LinkedList<Rule> expandables = new LinkedList<Rule>();
        LinkedList<int[]> indices = new LinkedList<int[]>();
        int prodIndex = 0;
        for (LinkedList<Rule> prods : subRules) {
            int ruleIndex = 0;
            for(Rule rule : prods) {
                if(rule.name.contains(" ") && !rule.equals(EPSILON)) {
                    expandables.add(rule);
                    int[] index = {prodIndex,ruleIndex};
                    indices.add(index);
                }
                ruleIndex++;
            }
            prodIndex++;
        }
        int toExpandIndex = randInt(expandables.size());
        Rule toExpand = expandables.get(toExpandIndex);
        int[] index = indices.get(toExpandIndex);
        
        //50% of the time we try to expand a subrule, if there are no expandable subrules ungroup will return false and the not will become true
        if(!(Math.random() < 1.0 && toExpand.canUngroup())) {
            // System.out.println("Expanding " + toExpand);
            List<Rule> toInsert = toExpand.subRules.get(0);
            LinkedList<Rule> prod = subRules.get(index[0]);
            prod.remove(index[1]);
            Collections.reverse(toInsert);
            toInsert.forEach(rule -> prod.add(index[1],rule));
            subRules.set(index[0], prod);
            if(!mainRule) resetName();
        }
        // System.out.println("New rule " + this);
    }

    public boolean canUngroup() {
        if(singular) return false;
        for (LinkedList<Rule> prods : subRules) {
            for(Rule rule : prods) {
                if(!rule.singular) return true;
            }
        }
        return false;
    }

    
    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    /**
     * Changes all occurances of toClean in this rule to be mandatory and non-iterative, used after a parserRule has epsilon added as alternative
     * @param toClean
     */
	public void cleanReferences(Rule toClean) {
        // System.out.println("Cleaning refrences to " + toClean.getName() + " in " + this);
        if(singular && this.equals(toClean)) {
            // System.out.println("Making " +  this + " mandatory");
            setOptional(false);
            return;
        }
        subRules.forEach(prod -> {
            prod.forEach(rule -> {
                // System.out.println("Checking " + rule.getName());
                if(rule.getName().contains(toClean.getName())) {
                    // if(rule.isOptional()) System.out.println("Making " + rule + " mandatory due to it containg " + toClean.getName());
                    if(rule.isOptional()) rule.setOptional(false);
                    rule.cleanReferences(toClean);
                }
            });
        });
    }
    
    /**
     * Inserts toAdd at a random position on the RHS of this rule
     * Changes toAdd to be a minor rule if it is not
     */
	public void extend(Rule toAdd) {
        toAdd.mainRule = false;
        int prodIndex = randInt(subRules.size());
        LinkedList<Rule> prod = subRules.get(prodIndex);
        int ruleIndex = randInt(prod.size());
        Rule toShift = prod.get(ruleIndex);
        if(!toShift.singular) {
            toShift.extend(toAdd);
        } else {
            prod.add(ruleIndex, toAdd);
        }
    }
    //Removes a random rule on the RHS, this includes removing rules inside brackets
    public void reduce() {
        int prodIndex = randInt(subRules.size());
        LinkedList<Rule> prod = subRules.get(prodIndex);
        int ruleIndex = randInt(prod.size());
        Rule toShift = prod.get(ruleIndex);
        //half the time we just remove the whole combined rule
        if(!toShift.singular && Math.random() < 1.0/toShift.getSubRules().get(0).size()) {
            toShift.reduce();
        } else {
            prod.remove(ruleIndex);
            if(prod.size() == 0) {
                subRules.remove(prodIndex);
            } 
        }
    }
    /**
     * Generates a new ruleName
     * @return
     */
    public static String genName() {    
        StringBuilder ruleNameBuilder = new StringBuilder();
                //Generates rule name by randomly concatting letters
        for (int nameIndex = 0; nameIndex < Constants.RULENAME_LEN; nameIndex++) {
            ruleNameBuilder.append((char)('a' + randInt(26)));
        }
        return ruleNameBuilder.toString();
    }
    /**
     * Replaces all references to oldRule with newRule, optional/iterative is maintained unless newRule is nullable
     * @param newRuleNullable is newRule nullable in the context of the calling grammar
     */
	public void replaceReferences(Rule oldRule, Rule newRule, boolean newRuleNullable) {
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> prod = subRules.get(i);
            for (int j = 0; j < prod.size(); j++) {
                Rule currRule = prod.get(j);
                if(currRule.equals(oldRule)) {
                    currRule.name = newRule.name;
                    if(newRuleNullable) {
                        currRule.setIterative(false);
                        currRule.setOptional(false);
                    }
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
            for (int j = 0; j < prod.size(); j++) {
                Rule currRule = prod.get(j);
                if(currRule.name.equals(toRemoveName)) {
                    prod.remove(j);
                } else if(!currRule.singular) {
                    currRule.removeReferences(toRemoveName);
                }
            }
        }
    }



    
    public int getSymbolCount() {
        int out = 0;
        for (LinkedList<Rule> prod : subRules) {
            out += prod.size();    
        }
        return out;
    }
	public boolean containsInfLoop(ArrayList<Rule> parserRules, String touchedRules) {
        System.out.println("Checking if " + getName() + " contains an infLoop touchedRules " + touchedRules);
        System.out.println("SubRule Size " + subRules.size() + " prod sizes\n" + subRules.stream().map(list -> {
            return list.toString() + " : " + list.size() + "\n";
        }).collect(Collectors.joining()));
        if(containsEpsilon()) {
            System.out.println(name + " contains epsilon returning false singular " + singular);
        }
        if(containsEpsilon()) return false;
        if(touchedRules.contains(getName()) && !containsEpsilon()) return true;
        if(mainRule) touchedRules = touchedRules + "," + getName();
        for(LinkedList<Rule> prod : subRules) {
            boolean out = true;
            for(Rule rule : prod) {
                System.out.println("Checking " + rule.getName());
                if(rule.singular) {
                    if(!rule.terminal) {
                        System.out.println("Fetching " + rule.name + " from parserRules");
                        Rule mainVer = parserRules.get(parserRules.indexOf(rule));
                        if(mainVer.containsInfLoop(parserRules, touchedRules)) return true;
                    }
                } else {
                    if(rule.containsInfLoop(parserRules, touchedRules)) return true;
                }
            }
        }


        return false;
	}

    
}
