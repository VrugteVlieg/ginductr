package stb.localiser.depend;

import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import stb.Gram;

public class Pipeline {
    /*
     **/
    /**
     * Parses the grammarfile with ANTLR4 lexer and parser
     * , attaches Dependency and parser.grammarSpec() to parseTreeWalker and walks
     * this records the dfa state names for all the rule and prods, writes to grammar.json
     * @param grammarPath
     */
    public static void pipeline(String UUT) {
        /* standard pipeline -- see antlr book */
        try {
            CharStream in = CharStreams.fromString(UUT);
            ANTLRv4Lexer lexer = new ANTLRv4Lexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
            /* build tree from start rule */
            ParseTree tree = parser.grammarSpec();
            /* custom tree walker */
            System.err.println("Pre dep");
            Dependency graph = new Dependency();
            System.err.println("Post dep");
            /* built-in tree walker */
            ParseTreeWalker walker = new ParseTreeWalker();
            /* walk the parse tree*/
            walker.walk(graph, tree);
        } catch(RecognitionException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
