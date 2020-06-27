package za.ac.sun.cs.localizer.dynamic;

import org.antlr.v4.runtime.*;
import java.util.*;

public aspect ParserAspect {
    
    static Set <String> spectrum; // = new HashSet<String>();

    public static void initSpectra() {
        /* clear some traces */
        spectrum = new HashSet<String>();
    }

    pointcut enterRuleAlt(ParserRuleContext ctx, int altNum, Parser parser) :
    call(void Parser.enterOuterAlt(ParserRuleContext, int)) && args(ctx, altNum) && target(parser);

    /*pointcut determineError(Parser recognizer, RecognitionException e,
    /BailErrorStrategy bail) :
    execution (Token ANTLRErrorStrategy.recoverInline (Parser, RecognitionException)) &&
    args(recognizer, e)  && target(bail); */


    before(ParserRuleContext ctx, int altNum, Parser parser) : enterRuleAlt(ctx,altNum, parser) {
        String[] rules = parser.getRuleNames();
        // int errors = parser.getNumberOfSyntaxErrors();
        //System.out.println("ruleIndex: "+rules[ctx.getRuleIndex()]+" alt: "+altNum);
        spectrum.add(rules[ctx.getRuleIndex()]+":"+altNum);
    }

   /* after (Parser parser) returning(Object r) : determineError(parser) {
        System.out.println("Errors: "+r.toString());
    }*/

    public static void  start() {
        System.out.println("dsdsds");
    }

    public static Set<String> getSpectra() {
        return spectrum;
    }

   // after (Parser recognizer, RecognitionException e, BailErrorStrategy bail)
   //  (Object r)  : determineError(recognizer, e,bail) {
   //     System.out.println("Caught!!") ;   
   // }


}
