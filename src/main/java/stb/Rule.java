package stb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;





public class Rule {
    private String name;
    private ArrayList<LinkedList<String>> productions = new ArrayList<LinkedList<String>>();
    private ArrayList<LinkedList<Rule>> subRules = new ArrayList<LinkedList<Rule>>();
    private boolean terminal;
    private boolean optional;
    private boolean iterative;
    private boolean mainRule;
    private boolean singular;
    private int depth = 0;

    public Rule(String ruleText, int depth) {
        ruleText = ruleText.trim();
        this.name = ruleText;
        printOut("New minor rule " + ruleText);
        this.mainRule = false;
        this.depth = depth;
        singular = !ruleText.contains(" ");
        optional = ruleText.charAt(ruleText.length()-1) == '?' || ruleText.charAt(ruleText.length()-1) == '*';
        iterative = ruleText.charAt(ruleText.length()-1) == '+' || ruleText.charAt(ruleText.length()-1) == '*';
        if(optional)printOut("Optional");
        if(iterative)printOut("Iterative");
        if(optional || iterative) {
            if(ruleText.charAt(0) == '(') {
                ruleText = ruleText.substring(1, ruleText.length()-2);
            } else {
                ruleText = ruleText.substring(0, ruleText.length());
            }
            printOut("New rule text " + ruleText);
        }
        if(!singular) addRuleLookahead(ruleText);
    }


    public Rule(String name, String ruleText) {
        ruleText = ruleText.trim();
        name = name.trim();
        this.name = name;
        printOut("New major rule " + name + " : " + ruleText);
        mainRule = true;
        terminal = Character.isUpperCase(name.charAt(0));
        optional = name.charAt(name.length()-1) == '?' || name.charAt(name.length()-1) == '*';
        iterative = name.charAt(name.length()-1) == '+' || name.charAt(name.length()-1) == '*';
        addRuleLookahead(ruleText);
    }

    public void addRule(String rule) {  //[ factor   ( Mulop factor )* | factor ]
        String[] ruleArray = rule.split("\\|"); // { factor   ( Mulop factor )* , factor }
        printOut("Adding greater { " + rule + " }");
        Arrays.asList(ruleArray).forEach(subRule -> {
            String workedRule = subRule.trim(); //{factor (Mulop factor)*}
            printOut("Adding rule { " + workedRule + " } to " + name);
            subRules.add(splitProduction(subRule));
            // productions.add(splitSubRules(workedRule));
        });
    }
     public void addRuleLookahead(String rule) {
         int index = 0;
         printOut("Adding " +  rule + " by lookahead");
         LinkedList<Rule> currProduction = new LinkedList<Rule>();
         StringBuilder currString = new StringBuilder();
         boolean StringLiteral = false;
         int brackets = 0;
         while(index < rule.length()) {
            printOut(rule.charAt(index) + " -> " + currString.toString().replaceAll(" ", "_SPACE_"));
            switch(rule.charAt(index)) {
               case ' ':
                   if(currString.length() != 0 && !StringLiteral && brackets==0) {
                       printOut("new production { " + currString.toString() + "}");
                       currProduction.add(new Rule(currString.toString(),depth+1));
                       printOut("currProduction: " + currProduction.toString() );
                       currString = new StringBuilder(); 
                   } else if(StringLiteral || brackets!=0){
                       currString.append(" ");
                   } else {
                       printOut("Ignoring space");
                   }
                   break;
               
               case '|':
                   if(StringLiteral) {
                       currString.append("|");
                   } else {
                       subRules.add(currProduction);
                       printOut("Pre reassign " + subRules.toString());
                       currProduction = new LinkedList<Rule>();
                       printOut("Post reassign " + subRules.toString());
                   }
                   break;

                case ';':
                    if(currString.length() != 0) {
                        printOut("new production { " + currString.toString() + "}");
                        currProduction.add(new Rule(currString.toString(),depth+1));
                        printOut("currProduction: " + currProduction.toString());
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

    public String getRawRule() {
        return this.toString().replace(name, "");
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

    private LinkedList<Rule> splitProduction(String input) {
        input = input.trim();
        LinkedList<Rule> out = new LinkedList<Rule>();
        if(!input.contains("(")) {
            String[] subRules = input.split(" ");
            Arrays.asList(subRules).forEach(rule -> out.add(new Rule(rule,depth+1)));
            return out;
        } else {
            StringBuilder inputBuilder =  new StringBuilder(input);
            int index = inputBuilder.indexOf("(");
            while(index != -1) {
                int endIndex = inputBuilder.indexOf(")");
                inputBuilder.replace(index, endIndex+1, inputBuilder.substring(index,endIndex+1).replaceAll(" ", "_"));
                index = input.substring(endIndex).indexOf("(");
            }
            String[] subRules = inputBuilder.toString().split(" ");
            printOut("Before passing to lamda " + Arrays.toString(subRules));
            Arrays.asList(subRules).forEach(rule -> out.add(new Rule(rule.replaceAll("_", " "), depth+1)));
            return out;
        }
    }

    
    public String toString() {
        if(!mainRule) {
            return name;
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

    String getName() {return name;}
    ArrayList<LinkedList<String>> getProductions() {return productions;}

    public LinkedList<String> cartesianProduct(LinkedList<String> a, LinkedList<String> b) {

        if(a.size() == 0) {
            return b;
        } else if(b.size() == 0) {
            return a;
        } else {

            LinkedList<String> output = new LinkedList<String>();
            a.forEach(elementA -> {
                b.forEach(elementB -> {
                    output.add(elementA + "" + elementB);
                });
            });
            System.out.println("Taking cart prod of " + a.size() + "\n and \n" + b.size() + "\n is \n" + output.size());
            return output;
        }
    }

    public LinkedList<String> filterLinkedList(LinkedList<String> input,  double filter) {
        LinkedList<String> out = new LinkedList<>();
        input.forEach(string -> {
            if(Math.random() < filter)out.add(string);
        });
        return out;
    }

    public void cartesianProduct(LinkedList<String> a, LinkedList<String> b, LinkedList<String> out) {
        out.addAll(cartesianProduct(a, b));
    }

    private void printOut(String toPrint) {
        if(Constants.DEBUG)System.out.println(" ".repeat(depth) + "Context " + name + ": " + toPrint);
    }
}
