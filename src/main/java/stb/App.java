package stb;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;

public class App {
    
    public static void main(String[] args) {
        try {
            System.out.println("Hello World!");
            GrammarReader myReader = new GrammarReader("./grammars/arithmetic.g4");
            Chelsea.generateSources(myReader);
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
