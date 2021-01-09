package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GrammarGenerator {

    static HashSet<String> nullGrams = new HashSet<>();
    static int nullGramHits = 0;
    static int grammarCount = 0;
    static Gram terminalGrammar = new Gram(new File(Constants.CURR_TERMINALS_PATH));
    static Gram skeletonGrammar;
    static int numSkeletonRules = 0;
    static List<String> potentialRules = new LinkedList<>();
    static List<String> terminalRules = new LinkedList<>();
    static String[] skeletonBases;
    

    static boolean hasBeenChecked(Gram in) {
        if(nullGrams.contains(in.hashString())) {
            nullGramHits++;
            return true;
        } else {
            return false;
        }
    }


    public static LinkedList<Gram> generatePopulation(int popSize) {
        LinkedList<Gram> output = new LinkedList<Gram>();
        for (int i = 0; i < popSize; i++) {
            String grammarName = "Grammar_" + Gram.genGramName();
            // System.err.println("Generating " + grammarName);
            
            Gram currGrammar;
            if(skeletonBases  == null) {
                currGrammar = new Gram(grammarName,terminalGrammar.getAllRules());
                int grammarRuleCount = 2 + randInt(Constants.MAX_RULE_COUNT-1);
                for (int j = 0; j < grammarRuleCount; j++) {
                    int currRuleLen = 1 + randInt(Constants.MAX_RHS_SIZE-1);
                    String ruleName =  currGrammar.genRuleName();
                    currGrammar.generateNewRule(ruleName, currRuleLen);
                }
            } else {
                currGrammar = getSkeletonBase();
                currGrammar.scrambleRuleNames();
                // System.err.println("PreFill " + currGrammar);
                fillBlanks(currGrammar);
                // System.err.println("PostFill " + currGrammar);
            }

            currGrammar.removeDuplicateProductions();
            
            // currGrammar.removeUnreachableBoogaloo();
            boolean lrDeriv = currGrammar.containsImmediateLRDeriv();
            if(App.gramAlreadyChecked(currGrammar) || lrDeriv) {
                i--;
                continue;
            } else {
                output.add(currGrammar);
            }
        }
        return output; 
    }

    public static LinkedList<Gram> generateDemoCrossoverPop() {
        LinkedList<Gram> output = new LinkedList<Gram>();
        for (int i = 0; i < 1; i++) {
            String grammarName = "Grammar_" + grammarCount++;
            // System.err.println("Generating " + grammarName);
            Gram currGrammar = new Gram(grammarName,terminalGrammar.getAllRules());
            int grammarRuleCount = 1 + randInt(2);
            
            for (int j = 0; j < grammarRuleCount; j++) {
                int currRuleLen = 1 + randInt(2);
                String ruleName =  currGrammar.genRuleName();
                currGrammar.generateNewRule(ruleName, currRuleLen);
            }
            currGrammar.removeDuplicateProductions();
            currGrammar.removeUnreachableBoogaloo();
            boolean lrDeriv = currGrammar.containsImmediateLRDeriv();
            if(App.gramAlreadyChecked(currGrammar) || lrDeriv) {
                i--;
                continue;
            } else {
                output.add(currGrammar);
            }
        }
        return output; 
    }

    public static void fillBlanks(Gram toFill) {
        ArrayList<Rule> mainRules = toFill.getParserRules();
        HashMap<String, String> newRuleMappings = new HashMap<>();
        for (int mainIndex = 0; mainIndex < mainRules.size(); mainIndex++) {
            if(mainRules.get(mainIndex).toString().contains("_new")) {
                ArrayList<LinkedList<Rule>> currRule = mainRules.get(mainIndex).getSubRules();
                for (int prodIndex = 0; prodIndex < currRule.size(); prodIndex++) {
                    LinkedList<Rule> currProd = currRule.get(prodIndex);
                    for (int ruleIndex = 0; ruleIndex < currProd.size(); ruleIndex++) {
                        String currName = currProd.get(ruleIndex).getName();
                        if(currName.startsWith("_new")){
                            boolean generateNewRule = toFill.getParserRules().size() < Constants.MAX_RULE_COUNT && Math.random() < 1.0/(mainRules.size()+1);
                            Rule newRule;
                            if(generateNewRule) {
                                int currRuleLen = 1 + ThreadLocalRandom.current().nextInt(Constants.MAX_RHS_SIZE-1);
                                String ruleName = toFill.genRuleName();
                                newRuleMappings.put(currName, ruleName);
                                newRule = toFill.generateReturnNewRule(ruleName, currRuleLen);
                                toFill.getParserRules().add(newRule);
                            } else {
                                if(newRuleMappings.containsKey(currName)) {
                                    newRule = toFill.getRuleByName(newRuleMappings.get(currName));
                                } else {
                                    newRule = Gram.randGet(toFill.getParserRules(), true);
                                }
                            }
                            newRule = newRule.makeMinorCopy();
                            currProd.set(ruleIndex,newRule);
                        }
                    }
                }
            }
        }
        
    }
    static int upperLimit  = 128;

    public static Gram getSkeletonBase() {
        int key = ThreadLocalRandom.current().nextInt(upperLimit);
        Gram out = new Gram(skeletonBases[key]);
        out.setName(Gram.genGramName());
        return out;
        
    }


    public static void readFromMLCS(String toReadPath) {
        HashMap<String, List<String>> initRules = new HashMap<>();
        HashMap<String, String> terminalMappings = new HashMap<>();
        try(BufferedReader in = new BufferedReader(new FileReader(new File(toReadPath)))) {
            in.lines().forEach(l -> {
                l = l.split(":", 2)[1];
                String[] data = l.split("->", 2);
                data[1] = data[1].substring(1, data[1].length()-1).replaceAll("[,]", " ").trim();
                data[1] = data[1].isEmpty() ? "EPS" : data[1];
                if(initRules.containsKey(data[0])) {
                    initRules.get(data[0]).add(data[1]);
                } else {
                    initRules.put(data[0], new LinkedList<String>(List.of(data[1])));
                }
            });

        } catch(Exception e) {
            e.printStackTrace();
        }

        try(BufferedReader in = new BufferedReader(new FileReader(new File(Constants.CURR_TERMINALS_PATH)))) {
            in.readLine();
            in.lines().forEach(l -> {
                terminalRules.add(l);
                String[] data = l.replaceAll("[ ;']", "").split(":",2);
                terminalMappings.put(data[1].trim(), data[0]);
            });

        } catch(Exception e) {
            e.printStackTrace();
        }

        // terminalMappings.entrySet().forEach(System.err::println);
        List<String> possibleRules = new LinkedList<>();

        initRules.entrySet().forEach(r -> {
            String rhs = r.getValue().stream().map(s -> 
                    s.equals("EPS") ? " " : 
                    Arrays.stream(s.split(" ")).map(t -> terminalMappings.getOrDefault(t, t)).collect(Collectors.joining(" ")))
                .collect(Collectors.joining(" | ")) + ";";
            

            System.err.println(r.getKey() + ": " + r.getValue());
            if(r.getKey().equals("start") || !rhs.contains("null")) possibleRules.add(r.getKey() + ": " + rhs.replaceAll("null", "_new_"));
        });

        //Reorder rules so start is first
        for (int i = 0; i < possibleRules.size(); i++) {
            if(possibleRules.get(i).startsWith("start")) {
                possibleRules.add(0, possibleRules.remove(i));
                break;
            }
        }
        numSkeletonRules = possibleRules.size()-1;
        System.err.println("**POTENTIAL RULES**");
        
        potentialRules = possibleRules;
        skeletonBases = new String[1 << possibleRules.size()-1];

        System.err.println("There are " +  skeletonBases.length + " bases");

        for (int j = 0; j < skeletonBases.length; j++) {
            skeletonBases[j] = generateBase(j);
        }
        // System.err.println("**SKELETON GRAMMAR**");
        // StringBuilder skeletonStr = new StringBuilder();
        // possibleRules.forEach(r -> skeletonStr.append(r + "\n"));
        // terminalRules.forEach(r -> skeletonStr.append(r + "\n"));
        // skeletonGrammar = new Gram(skeletonStr.toString());
        // skeletonGrammar.setName("skeleton");
    }

    public static String generateBase(int key) {
        List<String> toInclude = new LinkedList<>();
        List<Integer> freeRules = new LinkedList<>(IntStream.range(0, numSkeletonRules).boxed().collect(Collectors.toList()));
        for (int i = 0; i < numSkeletonRules; i++) {
            if((key >> i) % 2 == 1) {
                toInclude.add("x" + Gram.randGet(freeRules, false));
            }
        }
        String parserRulesStr = potentialRules.stream().filter(r -> toInclude.contains(r.substring(0, r.indexOf(":")))).collect(Collectors.joining("\n"));
        parserRulesStr = (potentialRules.get(0)  + "\n" + parserRulesStr).trim();
        for (int i = 0; i < potentialRules.size(); i++) {
            if(!toInclude.contains("x"+i)) parserRulesStr = parserRulesStr.replaceAll("x"+i, "_new" + i);
        }
        return parserRulesStr + "\n" + terminalRules.stream().collect(Collectors.joining("\n"));
    }

    public static LinkedList<Gram> generateLocalisablePop(int popSize) {
        LinkedList<Gram> out = new LinkedList<Gram>();
        System.err.println("Generating " + popSize + " candidates");
        

        while(out.size() < popSize) {
            System.err.print("Generating new pop " + out.size() + "/" + popSize + "\r");
            LinkedList<Gram> newPop = generatePopulation(Constants.NUM_THREADS * 20);
            // int sizeIn = newPop.size();
            // newPop.removeIf(GrammarGenerator::hasBeenChecked);
            // int sizeOut = newPop.size();
            // nullGramHits += sizeIn-sizeOut;
            // System.err.print("Filtered out " + (sizeIn-sizeOut) + " null grams, totalHits: " + nullGramHits + ", totalNulls: " + nullGrams.size() + "\r");
            App.runTests(newPop);
            // newPop.forEach(App::runTests);
            // newPop.forEach(gram -> {if(gram.getPosScore() == 0.0) nullGrams.add(gram.hashString());});
            newPop.removeIf(Predicate.not(Gram.passesPosTest));
            out.addAll(newPop);
        }

        
        out = new LinkedList<Gram>(out.subList(0, popSize));
        
        return out;
    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    
}