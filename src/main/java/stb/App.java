package stb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import javafx.application.Application;

public class App {  
    static GrammarReader demoGrammar;
    static GrammarReader demoGrammar2;
    static int numGrammarsChecked = 0;
    static int hashtableHits = 0;
    static int floatingEOF = 0;

    public static outputLambda logOutput = (String toPrint) -> System.out.println(toPrint);
    public static outputLambda grammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runGrammarOutput = (String toPrint) -> System.out.println(toPrint);
    static outputLambda runLogOutput = (String toPrint) -> System.out.println(toPrint);

    static GrammarReader bestGrammar;

    static LinkedList<GrammarReader> myGrammars;
    //Can reduce memory footprint my storing grammarStrings in positiveGrammars and reconstructing when needed
    static LinkedList<GrammarReader> positiveGrammars = new LinkedList<GrammarReader>();
    static LinkedList<GrammarReader> perfectGrammars = new LinkedList<GrammarReader>();
    static HashMap<Double, LinkedList<GrammarReader>> evaluatedGrammars = new HashMap<Double, LinkedList<GrammarReader>>();
    static HashMap<Integer, Boolean> generatedGrammars = new HashMap<Integer, Boolean>();
    public static void main(String[] args) {
        if(Constants.USE_GUI) {
            Application.launch(Gui.class, new String[]{});
            
            System.exit(0);
        }

        
        // GrammarReader goldenGrammar = new GrammarReader(new File(Constants.CURR_GRAMMAR_PATH));
        // goldenGrammar.injectEOF();
        // runLocaliser(goldenGrammar);
        // GrammarReader seededGrammar = new GrammarReader(new File(Constants.SEEDED_GRAMMAR_PATH));
        
        // try(FileWriter outFile = new FileWriter(new File(Constants.localizerGPath))) {
        //     outFile.append(goldenGrammar.toString().replaceFirst(goldenGrammar.getName(), "UUT"));
        //     outFile.append("WS  : [ \\n\\r\\t]+ -> skip;");
        //     outFile.close();
            
        //     Process testProc = Runtime.getRuntime().exec("sh -c " + Constants.localizerSPath);
        //     // Process testProc = Runtime.getRuntime().exec("sh -c " + );
        //     BufferedReader inputReader = new BufferedReader(new InputStreamReader(testProc.getInputStream()));
        //     inputReader.lines().forEach(System.out::println);

            
        //     System.out.println("Done waiting");
        //     getSusList().forEach(System.out::println);
        //     return;
        // } catch(Exception e) {
        //     e.printStackTrace();
        // }
    }


    
    public static void runTests(GrammarReader myReader, String testDir, scoringLambda scoreCalc) {
        if(myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            System.out.println("Flagging " + myReader.getName() + " for removal");
            myReader.flagForRemoval();
            return;
        }
        myReader.injectEOF();
      
        lamdaArg removeCurr = () ->  myReader.flagForRemoval();
        Chelsea.generateSources(myReader, removeCurr);
        if(myReader.toRemove()) {
            System.out.println("Code gen failed for \n" + myReader);
            myReader.stripEOF();
            return;
        }
        numGrammarsChecked++;
        try {
            
            int[] testResult = Chelsea.runTestcases(removeCurr, testDir);
            myReader.stripEOF();
            if(myReader.toRemove()) {
                return;
            }

            scoreCalc.eval(testResult, myReader);

        } catch(Exception e) {
            System.err.println("Exception in runTests " + e.getCause());
        }
    }

    public static void runTestsWithLocalisation(GrammarReader myReader, String testDir, scoringLambda scoreCalc) {
        if(myReader.getParserRules().size() == 0 || myReader.containsInfLoop()) {
            System.out.println("Flagging " + myReader.getName() + " for removal");
            myReader.flagForRemoval();
            return;
        }
        myReader.injectEOF();
        List<String> result = runLocaliser(myReader);
        String[] scores = result.get(0).split(",");
        int numPass = Integer.parseInt(scores[0]);
        int numFail = Integer.parseInt(scores[1]);
        myReader.stripEOF();
        if(result.isEmpty()) {
            myReader.flagForRemoval();
        } else {
            int[] passFail = {numPass, numFail};
            scoreCalc.eval(passFail, myReader);
            System.out.println(myReader.getName() + " has a score of " + myReader.getScore() + " with " + Arrays.toString(passFail));
            if(myReader.getScore() != 0.0) {
                System.out.println(result);
                myReader.setMutationConsideration(result.subList(1, result.size()));
            } else {
                myReader.setMutationConsideration(new LinkedList<String>());
            }
        }

    }    
    public static void demoMainProgram() {
        try {
            myGrammars = GrammarGenerator.generateLocalisablePop(Constants.INIT_POP_SIZE, generatedGrammars, Constants.positiveScoring);
            runLogOutput.output("Generated " + myGrammars.size() + " base grammars\n");
            // runGrammarOutput.output("inital grammars\n" + myGrammars.stream().map(2).collect(Collectors.joining("\n")));
            
            //Postive testing 
            for (int genNum = 0; genNum < Constants.NUM_ITERATIONS; genNum++) {
                runLogOutput.output("Starting generation " + genNum);
                runLogOutput.output("Hashtable hits : " + hashtableHits + "\n");
                
                LinkedList<GrammarReader> totalPop  = new LinkedList<GrammarReader>();
                totalPop.addAll(myGrammars);
                if(genNum != 0) {
                    // runGrammarOutput.output("Computing suggested mutants of " + myGrammars.stream().map(GrammarReader::getName).collect(Collectors.joining("\n")));
                    //Dont generate mutants on the first gen, we use a lot of initial grammars to cover search space
                    myGrammars.forEach(grammar -> totalPop.addAll(grammar.computeMutants(Constants.MUTANTS_PER_BASE,generatedGrammars)));
                }
                totalPop.forEach(grammar -> {
                    runGrammarOutput.output("Testing " +  totalPop.indexOf(grammar) + "/" + totalPop.size() + "\n" + grammar);
                    runTestsWithLocalisation(grammar, Constants.POS_TEST_DIR, Constants.localiserScoring);
                    // System.out.println(grammar.getMutationConsideration().stream().map(Arrays::toString).collect(Collectors.joining("\n")));
                    double outScore = grammar.getScore();
                    if(grammar.getScore() == 1.0) perfectGrammars.add(grammar);
                    if(grammar.getScore() == 1.0) grammarOutput.output("New positive grammar\n"  +  grammar);
                    if(outScore > getBestScore()) {
                        runLogOutput.output("\nNew best grammar\nBest score " + getBestScore() + " -> " +  outScore);
                        runLogOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
                        setBestGrammar(grammar);
                    }
                });
                totalPop.removeIf(GrammarReader::toRemove);

                
                totalPop.forEach(grammar -> {
                    double currScore = grammar.getScore();
                    if(evaluatedGrammars.containsKey(currScore)){
                        evaluatedGrammars.get(currScore).add(grammar);
                    } else {
                        LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                        thisScoreList.add(grammar);
                        evaluatedGrammars.put(currScore,thisScoreList);
                    }
                });
                
    
                List<Double> scoreList = evaluatedGrammars.keySet().stream()
                                    .sorted(Comparator.reverseOrder())
                                    .collect(Collectors.toList());

                // runGrammarOutput.output("Score list " + scoreList);

                
                //Add crossover grammars
                double bestScore = scoreList.get(0);
                //Only start performing crossover if some decent grammars already exist
                if(bestScore > 0.2 && false) {

                    ArrayList<GrammarReader> crossoverPop = performCrossOver(scoreList);
                    crossoverPop.forEach(grammar -> {
                        double outScore = grammar.getScore();
                        if(grammar.getPosScore() == 1.0) positiveGrammars.add(grammar);
                        if(grammar.getPosScore() == 1.0) grammarOutput.output("New positive grammar\n"  +  grammar);
                        if(outScore > getBestScore()) {
                            runGrammarOutput.output("\nNew best grammar\nBest score " + getBestScore() + " -> " +  outScore);
                            runGrammarOutput.output(grammar.hashString() + '\n' + grammar.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
                            setBestGrammar(grammar);
                        }
                    });
                    crossoverPop.forEach(grammar -> {
                        double currScore = grammar.getScore();
                        if(evaluatedGrammars.containsKey(currScore)){
                            evaluatedGrammars.get(currScore).add(grammar);
                        } else {
                            LinkedList<GrammarReader> thisScoreList = new LinkedList<GrammarReader>();
                            thisScoreList.add(grammar);
                            evaluatedGrammars.put(currScore,thisScoreList);
                        }
                    });
                    scoreList = evaluatedGrammars.keySet().stream()
                                    .sorted(Comparator.reverseOrder())
                                    .collect(Collectors.toList());
                }
    
                int grammarsToCarry = myGrammars.size();
                myGrammars.clear();
                // int grammarsToCarry = Constants.POP_SIZE - Constants.FRESH_POP_PER_GEN;
    
                scoreList.stream()
                        .map(evaluatedGrammars::get)
                        .flatMap(LinkedList::stream)
                        .limit(grammarsToCarry)
                        .forEach(myGrammars::add);

                // myGrammars.addAll(GrammarGenerator.generatePopulation(Constants.FRESH_POP_PER_GEN));
            }
            System.err.println("We done boys");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.err.println("Best Grammar " + getBestScore() + "\n" + bestGrammar);
            System.err.println("Floating EOF " + floatingEOF);
        }

    }

    public static ArrayList<GrammarReader> performCrossOver(List<Double> scoreArr) {
        //Calculate crossoverPop
        ArrayList<GrammarReader> crossoverPop = new ArrayList<GrammarReader>();
        double numScores = scoreArr.size();
        if(numScores == 0) return crossoverPop;
        double maxIndex = numScores-1;
        ArrayList<GrammarReader> toConsider = new ArrayList<GrammarReader>();
        if(maxIndex % 2 == 0) {
            toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int)(maxIndex/2))));
        } else {
            toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int)Math.floor(maxIndex/2))));
            toConsider.addAll(evaluatedGrammars.get(scoreArr.get((int)Math.ceil(maxIndex/2))));
        }
        for (int i = 0; i < Constants.NUM_CROSSOVER_PER_GEN; i++) {
            GrammarReader g1 = new GrammarReader(toConsider.remove(randInt(toConsider.size())));
            GrammarReader g2 = new GrammarReader(toConsider.remove(randInt(toConsider.size())));
            System.out.println("Performing crossover on " + g1 + " \nand\n" + g2);
            g1.applyCrossover(g2, runGrammarOutput);
            crossoverPop.add(g1);
            crossoverPop.add(g2);

        }

        //Run tests on crossoverPop
        crossoverPop.forEach(grammar -> {
            runTests(grammar, Constants.POS_TEST_DIR, Constants.positiveScoring);
        });
        crossoverPop.removeIf(GrammarReader::toRemove);
        return crossoverPop;

    }

    public static int randInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

    public static outputLambda getGrammarOut() {
        return grammarOutput;
    }
    
    public static void setGrammarOut(outputLambda toSet) {
        grammarOutput = toSet;
    }

    public static outputLambda getLogOut() {
        return logOutput;
    }
    
    public static void setLogOut(outputLambda toSet) {
        logOutput = toSet;
    }

    public static void loadStartGrammar() {
        GrammarReader out = new GrammarReader(new File(Constants.SEEDED_GRAMMAR_PATH));
        GrammarGenerator.fillBlanks(out);
        GrammarReader hashedOut = new GrammarReader(out.hashString() + '\n' + out.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        demoGrammar = hashedOut;
        hashedOut.setName("demoGrammar");
        ArrayList<String> reachables = new ArrayList<String>();
        demoGrammar.getParserRules().get(0).getReachables(demoGrammar.getParserRules(),reachables);
        // out.getParserRules().forEach(rule -> System.out.println(rule.getName() + rule.isSingular()));
        grammarOutput.output(hashedOut.toString());
    }



    public static void ruleCountDemo() {
        demoGrammar.demoChangeRuleCount(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void symbolCountDemo() {
        demoGrammar.demoChangeSymbolCount(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void groupDemo() {
        demoGrammar.demoGroupMutate(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void symbMutateDemo() {
        demoGrammar.demoMutate(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }

    public static void demoHeuristic() {
        demoGrammar.demoHeuristic(logOutput, grammarOutput);
        grammarOutput.output("\n"+demoGrammar.toString());
    }



    static void setRunGrammarOutput(outputLambda newRun) {
        runGrammarOutput = newRun;
    }

    static void setRunLogOutput(outputLambda newRun) {
        runLogOutput = newRun;
    }

    static void setBestGrammar(GrammarReader newBest) {
        bestGrammar = new GrammarReader(newBest.hashString() + '\n' + newBest.getTerminalRules().stream().map(Rule::toString).collect(Collectors.joining("\n")));
        bestGrammar.setName("bestCandidate");
        bestGrammar.setNegScore(newBest.getNegScore());
        bestGrammar.setPosScore(newBest.getPosScore());
        System.out.println("New Best grammar with score  " +  getBestScore());
    }

    static double getBestScore() {
        if(bestGrammar == null) return -1;
        return bestGrammar.getScore();
    }
    
    static String getBestGrammarString() {
        if(bestGrammar == null) return "";
        return bestGrammar.toString();
    }

    public static int numGrammarsEvalled() {
        return numGrammarsChecked;
    }

	public static void demoCrossover() {
		GrammarReader g1 = GrammarGenerator.generatePopulation(1).getFirst();
        GrammarReader g2 = GrammarGenerator.generatePopulation(1).getFirst();
        grammarOutput.output("clear");
        grammarOutput.output(g1.toString());
        grammarOutput.output(g2.toString());
        g1.applyCrossover(g2, grammarOutput);
        grammarOutput.output(g1.toString());
        grammarOutput.output(g2.toString());
	}

    public static List<String> getSusList() {

        try(Scanner myScanner = new Scanner(new File(Constants.CSV_PATH))) {
            HashMap<Double, List<String>> allScores = new HashMap<Double, List<String>>();
            myScanner.nextLine();
            while(myScanner.hasNext()) {

                String[] currLine = myScanner.nextLine().split(",");
                Double currScore = Double.parseDouble(currLine[5]);
                if(allScores.containsKey(currScore)) {
                    allScores.get(currScore).add(currLine[0]);
                } else {
                    List<String> toAdd = new LinkedList<String>();
                    toAdd.add(currLine[0]);
                    allScores.put(currScore,toAdd);
                }
            }
            return allScores.keySet().stream().sorted(Comparator.reverseOrder()).map(allScores::get).flatMap(list -> list.stream()).collect(Collectors.toList());
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new LinkedList<String>();
    }

    public static List<String> runLocaliser(GrammarReader currGrammar) {
        List<String> out = new LinkedList<String>();
        try(FileWriter outFile = new FileWriter(new File(Constants.localizerGPath))) {
            outFile.append(currGrammar.toString().replaceFirst(currGrammar.getName(), "UUT"));
            outFile.close();

            Process testProc = Runtime.getRuntime().exec("sh -c " + Constants.localizerSPath);
            
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(testProc.getInputStream()));
            HashMap<Double, LinkedList<String>> susScore = new HashMap<Double, LinkedList<String>>();
            int[] passFailCount = {-1, -1};
            
            String result= inputReader.readLine();
            if(result.equals("codeGenFailed")) {
                return out;
            }
            
            if(inputReader.toString().contains("nan")) return out;
            inputReader.lines().forEach(line -> {
                System.err.println(line);
                String[] data = line.split(",");
                if(data[0].equals("program:1") || data[5].equals("nan")) return;
                if(passFailCount[0] == -1) {
                    passFailCount[0] = (int)Double.parseDouble(data[2]) + (int)Double.parseDouble(data[4]);
                    passFailCount[1] = (int)Double.parseDouble(data[1]) + (int)Double.parseDouble(data[3]);
                }
                Double tarantula = Double.valueOf(data[5]);
                if(susScore.containsKey(tarantula)) {
                    susScore.get(tarantula).add(data[0]);
                } else {
                    LinkedList<String> toAdd = new LinkedList<String>();
                    toAdd.add(data[0]);
                    susScore.put(tarantula, toAdd);
                }
            });
            out.add(passFailCount[0] + "," + passFailCount[1]);
            out.addAll(susScore.keySet().stream()
                            .sorted(Comparator.reverseOrder())
                            .map(susScore::get)
                            .flatMap(LinkedList::stream)
                            .collect(Collectors.toList()));

            System.err.println(out);
            return out;
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        return out;

    }
   
}
