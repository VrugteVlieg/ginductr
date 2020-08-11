package stb.localiser.depend;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.misc.*;

import java.lang.reflect.Array;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;
import java.io.*;

import stb.localiser.dynamic.Logger;

public class Dependency extends ANTLRv4ParserBaseListener {

    Map<String, Integer> indexedRules = new HashMap<String, Integer>();
    Map<String, Integer> individuals = new HashMap<String, Integer>();
    /* used to assign index to rule  */
    int ruleCount = 0;
    /* keep track of lhs names of rules */
    ArrayList<String> nonTerminals = new ArrayList<String>();

    ArrayList<Production> productions = new ArrayList<Production>();
    ArrayList<String> defs;

    public void enterGrammarSpec(ANTLRv4Parser.GrammarSpecContext ctx) {
        //	ctx.rules().forEach(rule -> enterRules(rule));
        enterRules(ctx.rules());
        /* spit out file */
        try {
            writeFile();
        } catch(Exception e) { 
            e.printStackTrace();
        }
        /* enforce this manual traversal of the tree */
        int noOfRules = indexedRules.size();
        Logger logger = new Logger(noOfRules, indexedRules);
        return;
    }

    public void enterRules(ANTLRv4Parser.RulesContext ctx) {
        ctx.ruleSpec().forEach(rspec -> enterRuleSpec(rspec));	
    }

    public void enterRuleSpec(ANTLRv4Parser.RuleSpecContext ctx) {
        /* all parser rules */
        if(ctx.parserRuleSpec() != null)
            enterParserRuleSpec(ctx.parserRuleSpec());
    }
    String temp = "";
    public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
        defs =  new ArrayList<String>();
        nonTerminals.add(ctx.RULE_REF().getText());
        temp = ctx.RULE_REF().getText();
        addRuleIndex(temp);
        ruleCount++;
        enterRuleBlock(ctx.ruleBlock());
        productions.add(new Production(temp, defs));
        defs = new ArrayList<String>();
    }
    public void enterRuleBlock(ANTLRv4Parser.RuleBlockContext ctx) {
        enterRuleAltList(ctx.ruleAltList());
    }
    public void enterRuleAltList(ANTLRv4Parser.RuleAltListContext ctx) {
        enterLabeledAlt(ctx.labeledAlt(0));
        for(int i=1; i<ctx.labeledAlt().size(); i++) {
            addRuleIndex(temp);
            ruleCount++;
            productions.add(new Production(temp, defs));
            defs = new ArrayList<String>();
            enterLabeledAlt(ctx.labeledAlt(i));
        }
    }
    private void addRuleIndex(String nonTerminal) {
        if (!individuals.containsKey(nonTerminal)) {
            individuals.put(nonTerminal, 1);
            indexedRules.put(nonTerminal+":1", ruleCount);
        } else {
            int alt = individuals.get(nonTerminal) + 1;
            individuals.put(nonTerminal, alt);
            indexedRules.put(nonTerminal+":"+alt, ruleCount);
        }
    }
    public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
        enterAlternative(ctx.alternative());
    }
    /* Rule alts, just after lexer rules */
    public void enterAltList(ANTLRv4Parser.AltListContext ctx) {
        enterAlternative(ctx.alternative(0));
        for(int i=1; i<ctx.alternative().size(); i++) {
            defs.add(ctx.OR(i-1).getText());
            enterAlternative(ctx.alternative(i));
        }
    }
    public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {
        if(ctx.elementOptions() != null)
            enterElementOptions(ctx.elementOptions());
        if (ctx.element() != null) 
            ctx.element().forEach(ele -> enterElement(ele));
        // check for explictit empty alts 
        if (ctx.empty() != null) 
            defs.add("\'epsilon\'");
    }
    public void enterElement(ANTLRv4Parser.ElementContext ctx) {
        if(ctx.labeledElement() != null) {
            enterLabeledElement(ctx.labeledElement());
            if(ctx.ebnfSuffix() != null)
                enterEbnfSuffix(ctx.ebnfSuffix()); ///////////////////////////////
        }

        if(ctx.atom() != null) {
            enterAtom(ctx.atom());
            if(ctx.ebnfSuffix() != null)
                enterEbnfSuffix(ctx.ebnfSuffix()); ///////////////////////////
        }

        if(ctx.ebnf() != null) {
            enterEbnf(ctx.ebnf());
        }

        if(ctx.actionBlock() != null) {
            enterActionBlock(ctx.actionBlock());
            if(ctx.QUESTION() != null) {
                defs.add(ctx.QUESTION().getText());
            }
        }
    }
    public void enterLabeledElement(ANTLRv4Parser.LabeledElementContext ctx) {
        if(ctx.atom() != null) 
            enterAtom(ctx.atom());
        if(ctx.block() != null) 
            enterBlock(ctx.block());
    }
    public void enterEbnf(ANTLRv4Parser.EbnfContext ctx) {
        enterBlock(ctx.block());
        if(ctx.blockSuffix() != null)
            enterBlockSuffix(ctx.blockSuffix());
    }
    public void enterBlockSuffix(ANTLRv4Parser.BlockSuffixContext ctx) {
        enterEbnfSuffix(ctx.ebnfSuffix());
    }
    public void enterEbnfSuffix(ANTLRv4Parser.EbnfSuffixContext ctx) {
        if(ctx.getChildCount() == 1) {
            defs.add(ctx.getChild(0).getText());
        }
        else {  defs.add(ctx.getChild(0).getText());
            defs.add(ctx.getChild(1).getText());
        }
    }
    public void enterAtom(ANTLRv4Parser.AtomContext ctx) {
        if(ctx.terminal() != null)
            enterTerminal(ctx.terminal());
        if(ctx.ruleref() != null)
            enterRuleref(ctx.ruleref());
        if(ctx.notSet() != null)
            enterNotSet(ctx.notSet());
        if(ctx.DOT() != null) {
            defs.add(ctx.DOT().getText());
            if(ctx.elementOptions() != null)
                enterElementOptions(ctx.elementOptions());
        }
    }

    public void enterNotSet(ANTLRv4Parser.NotSetContext ctx) {
        defs.add(ctx.NOT().getText());

        if(ctx.setElement() != null)
            enterSetElement(ctx.setElement());
        else enterBlockSet(ctx.blockSet());
    }
    public void enterBlockSet(ANTLRv4Parser.BlockSetContext ctx) {
        defs.add(ctx.LPAREN().getText());
        enterSetElement(ctx.setElement(0));
        for(int i=1; i<ctx.setElement().size(); i++) {
            defs.add(ctx.OR(i-1).getText());
            enterSetElement(ctx.setElement(i));
        }
        defs.add(ctx.RPAREN().getText());
    }
    public void enterSetElement(ANTLRv4Parser.SetElementContext ctx) {
        if(ctx.TOKEN_REF() != null) {
            defs.add(ctx.TOKEN_REF().getText());
        }
        if(ctx.STRING_LITERAL() != null) {
            defs.add(ctx.STRING_LITERAL().getText());
        }
        if(ctx.characterRange_() != null) 
            enterCharacterRange_(ctx.characterRange_());
        if(ctx.LEXER_CHAR_SET() != null) {
            defs.add(ctx.LEXER_CHAR_SET().getText());
        }
    }

    public void enterBlock(ANTLRv4Parser.BlockContext ctx) {
        defs.add(ctx.LPAREN().getText());
        if(ctx.optionsSpec() != null)
            enterOptionsSpec(ctx.optionsSpec());
        ctx.ruleAction().forEach(rule -> enterRuleAction(rule));
        if(ctx.COLON() != null) {
            defs.add(ctx.COLON().getText());
        }
        enterAltList(ctx.altList());
        defs.add(ctx.RPAREN().getText());
    }
    public void enterRuleref(ANTLRv4Parser.RulerefContext ctx) {
        nonTerminals.add(ctx.RULE_REF().getText());
        defs.add(ctx.RULE_REF().getText());
        if(ctx.argActionBlock() != null)
            enterArgActionBlock(ctx.argActionBlock());
        if(ctx.elementOptions() != null)
            enterElementOptions(ctx.elementOptions());
    }
    /* modification to translate not set */
    public void enterCharacterRange_(ANTLRv4Parser.CharacterRange_Context ctx) {
        defs.add(ctx.STRING_LITERAL(0).getText());
        defs.add(ctx.RANGE().getText());
        defs.add(ctx.STRING_LITERAL(1).getText());
    }
    public void enterCharacterRange(ANTLRv4Parser.CharacterRangeContext ctx) {
        defs.add(ctx.STRING_LITERAL(0).getText());
        defs.add(ctx.RANGE().getText());
        defs.add(ctx.STRING_LITERAL(1).getText());
    }
    public void enterTerminal(ANTLRv4Parser.TerminalContext ctx) {
        if(ctx.TOKEN_REF() != null && !ctx.TOKEN_REF().getText().equals("EOF")) {
            defs.add(ctx.TOKEN_REF().getText());
            if(ctx.elementOptions() != null)
                enterElementOptions(ctx.elementOptions()); ///////////////
        }
        if(ctx.STRING_LITERAL() != null) {
            defs.add(ctx.STRING_LITERAL().getText());
            if(ctx.elementOptions() != null)
                enterElementOptions(ctx.elementOptions());
        }
    }
    public void enterElementOptions(ANTLRv4Parser.ElementOptionsContext ctx) {
        defs.add(ctx.LT().getText());
        enterElementOption(ctx.elementOption(0));
        for(int i=1;i<ctx.elementOption().size();i++) {
            defs.add(ctx.COMMA(i-1).getText());
            enterElementOption(ctx.elementOption(i));
        }
        defs.add(ctx.GT().getText());
    }
    public void enterElementOption(ANTLRv4Parser.ElementOptionContext ctx) {
        if(ctx.getChildCount() == 1) {
            enterIdentifier(ctx.identifier(0));
        }
        else {
            enterIdentifier(ctx.identifier(0));
            defs.add(ctx.ASSIGN().getText());
            if(ctx.identifier().size() > 1) {
                enterIdentifier(ctx.identifier(1));
            }
            else {
                defs.add(ctx.STRING_LITERAL().getText());
            }
        }
    }
    public void enterIdentifier(ANTLRv4Parser.IdentifierContext ctx) {
        if(ctx.RULE_REF() != null) {
            nonTerminals.add(ctx.RULE_REF().getText());
            defs.add(ctx.RULE_REF().getText());
        }
        if(ctx.TOKEN_REF() != null && !ctx.TOKEN_REF().getText().equals("EOF")) {
            defs.add(ctx.TOKEN_REF().getText());
        }
    }
    
    /* write  all rules and indices to json file */
    private void writeFile() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter("grammar.json"));
            writer.append("{\n");
            ArrayList<String> keys = new ArrayList<String>(indexedRules.keySet());
            for (int i = 0; i < keys.size() ; i++) {
                String key = keys.get(i);
                writer.append("    \""+indexedRules.get(key)+"\"   : ");
                if (i != keys.size()-1) 
                    writer.append("\""+key+"\",\n");
                else 
                    writer.append("\""+key+"\"\n");
            }
            writer.append("}");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }
}
