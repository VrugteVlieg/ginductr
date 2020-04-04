package stb;

import java.util.ArrayList;
import java.util.LinkedList;


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
     * Creates a minor rule, these are rules inside of major rules
     * @param ruleText
     * @param depth how many levels deep is this rule, used to format printOut
     */
    public Rule(String ruleText, int depth) {
        ruleText = ruleText.trim();
        this.name = ruleText;
        this.ruleText = ruleText;
        // printOut("New minor rule " + ruleText);
        this.mainRule = false;
        this.depth = depth;
        singular = !ruleText.contains(" ");

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
        this.name = ruleText.trim();
        
        if(singular) terminal = Character.isUpperCase(name.charAt(0)); 
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
                    } else if(StringLiteral || brackets!=0){
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
        //  printOut("Final subRules " + subRules.toString());
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
                output.append(rule + " ");
            });
            output.append("| ");
        });
        output.replace(output.length()-2, output.length(), ";");
       
        return output.toString();
    }

    String getName() {
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
        int counter = 0;
        int subSetIndex = 0;
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
            reachables.add("Undefined " + this.getName());
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
}
