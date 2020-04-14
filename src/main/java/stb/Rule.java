package stb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


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

    public static Rule EPSILON =  new Rule(" ",1);
    

    /**
     * Creates a deep-copy of a rule
     * @param toCopy
     */
    public Rule(Rule toCopy) {
        this.name = toCopy.name;
        this.subRules = new ArrayList<LinkedList<Rule>>(toCopy.subRules);
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
        } else {
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
        singular = true;
        printOut("New major rule " + name + " : " + ruleText);
        mainRule = true;
        terminal = Character.isUpperCase(name.charAt(0));
        optional = name.charAt(name.length()-1) == '?' || name.charAt(name.length()-1) == '*';
        iterative = name.charAt(name.length()-1) == '+' || name.charAt(name.length()-1) == '*';
        addRuleLookahead(ruleText);
    }

     public void addRuleLookahead(String rule) {
         int index = 0;
         if(!mainRule) rule = rule+";"; //adds ; to the end of the rule text to make parsing easier
        //  printOut("Adding \'" +  rule + "\' by lookahead");
         LinkedList<Rule> currProduction = new LinkedList<Rule>();
         StringBuilder currString = new StringBuilder();
         boolean StringLiteral = false;
         int brackets = 0;
         while(index < rule.length()) {
            // printOut(rule.charAt(index) + " -> " + currString.toString().replaceAll(" ", "_SPACE_"));
            switch(rule.charAt(index)) {
               case ' ':
                    if(currString.length() != 0 && !StringLiteral && brackets==0) {
                        currProduction.add(new Rule(currString.toString(),depth+1));
                        currString = new StringBuilder(); 
                    } else if(StringLiteral || brackets!=0 || rule.charAt(index+1) == ';' || rule.charAt(index+1) == ';'){
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
                        currProduction.add(new Rule(currString.toString(),depth+1));
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
    //This might not be updated properly when a rule is mutated

    public String getRuleText() {
        return ruleText;
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
        output.append(name + " : ");
        
        subRules.forEach(ruleLL -> {
            ruleLL.forEach(rule -> {
                if(!rule.getName().equals(" ")) output.append(rule + " ");
            });
            output.append("| ");
        });
        output.replace(output.length()-2, output.length(), ";");
       
        return output.toString();
    }

    String getName() {
        if(name.equals(" ")) return name; //this is nullToken 
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
        this.iterative = setTo;
        out.append(" -> " + getName());
        printOut(out.toString());
    }

    public boolean isIterative() {
        return this.iterative;
    }

    public boolean isOptional() {
        return this.optional;
    }



    public void setOptional(boolean setTo) {
        StringBuilder out = new StringBuilder(getName());
        this.optional = setTo;
        out.append(" -> " + getName());
        printOut(out.toString());
    }
    public boolean isTerminal() {
        return this.terminal;
    }
    public void setProduction(int index, Rule toSet) {
        Rule test = new Rule(toSet);
        test.mainRule = false;
        int counter = 0;
        // System.out.println("Setting rule " + index + " in " + this + " to " + test);
        int subSetIndex = 0;
        for(LinkedList<Rule> subSet : subRules) {
            int subRuleIndex = 0;
            for(Rule subRule : subSet) {
                int counterPre = counter;
                if(counterPre == index-1) {
                    // printOut("Setting " + subRules.get(subSetIndex).get(subRuleIndex) + " to " + test.name);
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
    
    public void addAlternative(ArrayList<LinkedList<Rule>> toAdd) {
        this.subRules.addAll(toAdd);
    }
    public void addAlternative(LinkedList<Rule> toAdd) {
        this.subRules.add(toAdd);
    }
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

    private boolean nullable(ArrayList<Rule> parserRules, String checkedRules) {
        // System.out.println("Checking if " + this + " in " + parserRules + " is nullable");
        // System.out.println("checkedRules " + checkedRules);
        // if(checkedRules.contains(getName())) System.out.println(this + " has been visited, returning false");
        if(terminal || checkedRules.contains(getName())) return false;
        if(this.equals(EPSILON)) return true;
        
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
            for(LinkedList<Rule> prod : subRules) {
                if(prod.get(0).equals(EPSILON)) {
                    // System.out.println(getName() + " contains EPSILON, nullable");
                    return true;
                }
            }
            // System.out.println(getName() + " does not contain EPSILON, checking nullability of productions");
            for(LinkedList<Rule> prod : subRules) {
                // System.out.println("    Checking if " + prod + " is nullable");
                boolean nullable = true;
                for(Rule rule : prod) {
                    // System.out.println("        Checking if " + rule + " is nullable");
                    nullable &= rule.nullable(parserRules,checkedRules);
                }
                // if(nullable) System.out.println(prod + " in " + getName() + " is nullable");
                // if(nullable) System.out.println(this + " is nullable");
                if(nullable) return true;
            }
            // System.out.println(this + " is not nullable");
            return false;
        }
    }
    
    public boolean nullable(ArrayList<Rule> parserRules) {
        return nullable(parserRules, "");
    }

    public boolean containsEpsilon() {
        for (LinkedList<Rule> linkedList : subRules) {
            if(linkedList.getFirst().equals(EPSILON)) return true;
        }
        return false;
    }
    
    public void heuristic(double pH) {

        if(!mainRule && Math.random() < pH) {
            double choice = Math.random();
            if(choice < 0.33) {
                setIterative(!iterative);
            } else if(choice < 0.66) {
                setOptional(!optional);
            } else {
                setOptional(!optional);
                setIterative(!iterative);
            }
        } else {
            subRules.forEach(subRule -> {
                subRule.forEach(production -> {
                    production.heuristic(pH);
                });
            });
        }            
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
        for (int i = 0; i < subRules.size(); i++) {
            LinkedList<Rule> currRule = subRules.get(i);
            if(currRule.getFirst().getName().equals(getName())) {
                while(currRule.size() > 1 && currRule.get(1).getName().equals(getName())) currRule.removeFirst();
            }
        }
    }

    public boolean containLeftRecursiveProd() {
        for(LinkedList<Rule> prod : subRules) {
            if(prod.getFirst().getName().equals(getName())) {
                // System.out.println(this.getName() + " contains a left recursive production " + prod);
                return true;
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
	public boolean groupProductions() {
        boolean canGroup =  false;
        for (LinkedList<Rule> prods : subRules) {
            canGroup |= (prods.size() >= 2);
        }
        //This rule does not contain any productions with more than 1 symbol
        if(!canGroup) return false;

        LinkedList<Rule> prodToGroup = subRules.get(randInt(subRules.size()));
        while(prodToGroup.size() < 2) prodToGroup = subRules.get(randInt(subRules.size()));
        int startIndex = randInt(prodToGroup.size()-2);
        int endIndex = startIndex + randInt(prodToGroup.size() - startIndex - 1) + 1;
        List<Rule> toRemove = prodToGroup.subList(startIndex, endIndex+1);
        System.out.println("Making a new rule from " + toRemove);
        Rule newRule = new Rule(toRemove,depth+1);
        System.out.println(getName() + " goes from \n" + subRules);
        System.out.println("prodToGroup " + prodToGroup + " start " + startIndex + " end " + endIndex);
        for (int i = startIndex; i < endIndex; i++) {
            prodToGroup.remove(startIndex);
        }
        // prodToGroup.removeAll(toRemove);
        prodToGroup.set(startIndex, newRule);
        System.out.println("To " + subRules);
        
		return true;
    }

    /**
     * Attempts to expand out a bracketd rule into its constituent rules
     * returns true if a brackted rule was expanded else returns false
     */
    public boolean unGroupProductions() {
        System.out.println("Running ungroup on " + this);
        boolean canExpand = false;
        LinkedList<Rule> expandables = new LinkedList<Rule>();
        LinkedList<int[]> indices = new LinkedList<int[]>();
        int prodIndex = 0;
        for (LinkedList<Rule> prods : subRules) {
            int ruleIndex = 0;
            for(Rule rule : prods) {
                if(rule.name.contains(" ")) {
                    canExpand |= rule.name.contains(" ");
                    expandables.add(rule);
                    int[] index = {prodIndex,ruleIndex};
                    indices.add(index);
                }
                ruleIndex++;
            }
            prodIndex++;
        }
        if(!canExpand) System.out.println("Cannot expand anything in " + getName());
        if(!canExpand) return false;
        int toExpandIndex = randInt(expandables.size());
        Rule toExpand = expandables.get(toExpandIndex);
        int[] index = indices.get(toExpandIndex);
        
        //50% of the time we try to expand a subrule, if there are no expandable subrules ungroup will return false and the not will become true
        if(!(Math.random() < 1.0 && toExpand.unGroupProductions())) {
            List<Rule> toInsert = toExpand.subRules.get(0);
            LinkedList<Rule> prod = subRules.get(index[0]);
            prod.remove(index[1]);
            Collections.reverse(toInsert);
            toInsert.forEach(rule -> prod.add(index[1],rule));
            subRules.set(index[0], prod);
            if(!mainRule) resetName();
        }
        System.out.println("New rule " + this);
        return true;
    }


    //TODO add a mutation to expand out brackted rules
    
    public int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
