package stb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class App {
    
    static HashMap<String,Double> scores = new HashMap<>();
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            // GrammarReader myReader = new GrammarReader("./grammars/arithmetic.g4");
            // runTests(myReader);   
            LinkedList<GrammarReader> myGrammars = GrammarGenerator.generatePopulation(2);
            myGrammars.forEach(grammar -> {
                runTests(grammar);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //TODO implement scoring system followed by mutations

    public static void runTests(GrammarReader myReader) {
        Chelsea.generateSources(myReader);
        try {
            HashMap<String,LinkedList<Stack<String>>> errors = Chelsea.runTestcases();
            if(errors.size() > 0) {
                errors.forEach((key,value) -> {
                    System.out.println("Errors for " + key + " = " + value.size());
                    for (int i = 0; i < value.size(); i++) {
                        System.out.println((i+1) + ": " + value.get(i));
                    }
                });
            } else {
                System.out.println("All tests pass");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
