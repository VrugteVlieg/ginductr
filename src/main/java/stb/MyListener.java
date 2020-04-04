package stb;

import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;

/*
Adapted from code I found on Stackoverflow
https://stackoverflow.com/questions/45406853/whats-the-difference-between-antlr4s-errorlistener-and-errorhandler
*/

public class MyListener implements ANTLRErrorListener {
    private LinkedList<Stack<String>> errors;
    String grammarName;
    public MyListener(String grammarName){
        errors = new LinkedList<Stack<String>>();
        this.grammarName = grammarName;
    }
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) 
    {
        List<String> stack = ((Parser)recognizer).getRuleInvocationStack(); Collections.reverse(stack);
        System.err.println(grammarName + " error " + msg);
        System.err.println("Stack " + stack);
        Stack<String> toAdd = new Stack<String>();
        stack.forEach(rule -> toAdd.add(rule));
        errors.add(toAdd);

    }



    



    public LinkedList<Stack<String>> getErrors() {
        return errors;
    }

    @Override
    public void reportAmbiguity(Parser recognizer, org.antlr.v4.runtime.dfa.DFA dfa, int startIndex, int stopIndex,
            boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
            System.err.println("Report ambiguity m8");
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void reportAttemptingFullContext(Parser recognizer, org.antlr.v4.runtime.dfa.DFA dfa, int startIndex,
        int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
            System.err.println("Report full context m8");
            // TODO Auto-generated method stub
            
        }
        
        @Override
        public void reportContextSensitivity(Parser recognizer, org.antlr.v4.runtime.dfa.DFA dfa, int startIndex,
        int stopIndex, int prediction, ATNConfigSet configs) {
            System.err.println("Report context sensitivity m8");
        // TODO Auto-generated method stub

    }
       
}