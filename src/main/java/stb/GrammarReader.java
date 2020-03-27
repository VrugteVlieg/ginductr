package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;




public class GrammarReader {

    private File grammarFile;
    private String grammarName;
    private ArrayList<Rule> allRules = new ArrayList<Rule>();
    private ArrayList<Rule> parserRules;
    private ArrayList<Rule> terminalRules;
    

    public GrammarReader(String filePath) {
        grammarFile = new File(filePath);
        grammarName = grammarFile.getName().split("\\.")[0];
        readRules();
    }

    public GrammarReader(String name, ArrayList<Rule> terminalRules) {
        grammarName = name;
        this.terminalRules = terminalRules;
        this.allRules = terminalRules;
    }

    private void readRules() {
        try (BufferedReader in = new BufferedReader(new FileReader(grammarFile));){
            String input;
            in.readLine();
            while((input = in.readLine()) != null) {
                System.out.println(input);
                input = input.replaceAll("[ ]+", " ").trim();
                String RuleName = input.substring(0,input.indexOf(":")).trim();
                String RuleString = input.substring(input.indexOf(":")+1); //cuts off the ; at the end
                Rule currRule = new Rule(RuleName, RuleString);
                // currRule.addRule(RuleString);
                allRules.add(currRule);

            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ArrayList<Rule> getAllRules() {
        return allRules;
    }
    public String getStartSymbol() {
        return allRules.get(0).getName();
    }
    

    
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append("grammar " + grammarName + ";\n");
        allRules.forEach(Rule -> out.append(Rule + "\n"));
        return out.toString();
    }

    public String getName() {return grammarName;}

    ArrayList<Rule> getParserRules() {
        return parserRules;
    }

    public void setTerminalRules(ArrayList<Rule> newRules) {
        allRules.removeAll(this.terminalRules);
        this.terminalRules =  newRules;
        allRules.addAll(newRules);
    }

    public void generateNewRule(int RHSLen) {
        if(RHSLen == 0) return;
        String ruleName = "rule_" + allRules.size();
        StringBuilder newRuleText = new StringBuilder();
        // System.out.println(allRules);
        for (int i = 0; i < RHSLen; i++) {
            int newRulesIndex = ThreadLocalRandom.current().nextInt(allRules.size());
            newRuleText.append(allRules.get(newRulesIndex).getName() + " ");
        }
        newRuleText.append(";");
        Rule toAdd =  new Rule(ruleName,newRuleText.toString());
        allRules.add(toAdd);
    }
}