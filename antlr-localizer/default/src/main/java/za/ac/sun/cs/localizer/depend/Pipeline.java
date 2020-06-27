package za.ac.sun.cs.localizer.depend;

import java.io.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Pipeline {
    /*
     **/
    public static void pipeline(String grammar) {
        /* standard pipeline -- see antlr book */
        try {
            FileInputStream fis = new FileInputStream(grammar);
            CharStream in = CharStreams.fromStream(fis);
            ANTLRv4Lexer lexer = new ANTLRv4Lexer(in);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
            /* build tree from start rule */
            ParseTree tree = parser.grammarSpec();
            /* custom tree walker */
            Dependency graph = new Dependency();
            /* built-in tree walker */
            ParseTreeWalker walker = new ParseTreeWalker();
            /* walk the parse tree*/
            walker.walk(graph, tree);
        } catch(IOException e) {
            e.printStackTrace();
        } catch(RecognitionException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
