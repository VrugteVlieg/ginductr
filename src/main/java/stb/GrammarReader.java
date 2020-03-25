package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;




public class GrammarReader {

    private File grammarFile;
    private String grammarName;
    private ArrayList<Rule> allRules = new ArrayList<Rule>();
    private ArrayList<Rule> parserRules;
    private ArrayList<Rule> lexerRules;
    

    public GrammarReader(String filePath) {
        grammarFile = new File(filePath);
        grammarName = grammarFile.getName().split("\\.")[0];
        readRules();

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
    // ArrayList<Rule> getLexerRules() {
    //     return lexerRules;
    // }

    
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
}