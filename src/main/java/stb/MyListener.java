package stb;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;




/*
Adapted from code I found on Stackoverflow
https://stackoverflow.com/questions/45406853/whats-the-difference-between-antlr4s-errorlistener-and-errorhandler
*/

public class MyListener extends BaseErrorListener {
    private LinkedList<Stack<String>> errors;
    public MyListener(){
        errors = new LinkedList<Stack<String>>();
    }
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e)
    {
        List<String> stack = ((Parser)recognizer).getRuleInvocationStack(); Collections.reverse(stack);
        // System.out.println("From listener " + msg);
        Stack<String> toAdd = new Stack<String>();
        stack.forEach(rule -> toAdd.add(rule));
        errors.add(toAdd);

    }

    public LinkedList<Stack<String>> getErrors() {
        return errors;
    }
       
}