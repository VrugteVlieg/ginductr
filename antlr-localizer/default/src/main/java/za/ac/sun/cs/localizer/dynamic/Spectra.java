package za.ac.sun.cs.localizer.dynamic;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.*;
import java.util.*;
import java.io.*;

public class Spectra {

     static String[] rules;
     public static class UUTLoader extends UUTBaseListener {
          int ruleIndex;
          ArrayList<Integer> l = new ArrayList<Integer>();
          ArrayList<Integer> alt = new ArrayList<Integer>();

          /* where the magic happens */
          public void enterEveryRule(ParserRuleContext ctx) {
               l.add(ctx.getRuleIndex());
               alt.add(ctx.getAltNumber());
          }
     }

     public static class VerboseListener extends BaseErrorListener {

          @Override
          public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {

                 List<String> stack = ((Parser)recognizer).getRuleInvocationStack();
                 Collections.reverse(stack);
                 //System.err.println("rule stack: "+stack);
                 //System.err.println("line "+line+":"+charPositionInLine+" at "+ offendingSymbol+": "+msg);
          }
     }

     /*  utility functions of outer class */
     public static ArrayList<String> produceSpectra(UUTParser parser, ParseTree tree, ParseTreeWalker walker) {

          ArrayList<String> rule = new ArrayList<String>();
          UUTLoader loader = new UUTLoader();
          walker.walk(loader, tree);

          rules = parser.getRuleNames();

          for(int i=0; i<loader.l.size(); i++) {
                if(loader.alt.get(i) != 0)
                       if(!rule.contains(rules[loader.l.get(i)]+":"+loader.alt.get(i)))
                                rule.add(rules[loader.l.get(i)]+":"+loader.alt.get(i));
          }
          return rule;
     }

     public static VerboseListener getErrorListener() {
            return new VerboseListener();
     }

     public static String[] getRules() {
            return rules;
     }
}
