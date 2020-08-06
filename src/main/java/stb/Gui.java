package stb;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Gui extends Application {

    public static String RUN_GRAMMAR_OUT = "RGO";
    public static String RUN_LOG_OUT = "RLO";
    public static String GRAMMAR_OUT = "go";
    public static String LOG_OUT = "lo";
    public static String TOKEN_DELIM = "=";
    public static String LOG_FILE_NAME = "GUI_TOKENS.out";
    FileWriter logger;
    public HashMap<String, TextArea> outputAreas = new HashMap<String, TextArea>();
    public HashMap<String, Stack<String>> outputStacks = new HashMap<String, Stack<String>>();



    // format for output messages is target_type_message?

    // Wrappers for different output token type
    public static String appendToken(String toAppend) {
        return outputLambda.APPEND + TOKEN_DELIM + toAppend;
    }

    public static String pushToken() {
        return outputLambda.PUSH;
    }

    public static String popToken() {
        return outputLambda.POP;
    }

    public static String clearToken() {
        return outputLambda.CLEAR;
    }

    public static String setToken(String newText) {
        return outputLambda.SET + TOKEN_DELIM + newText;
    }

    public static String updateToken(String newText) {
        return outputLambda.UPDATE + TOKEN_DELIM + newText;
    }

    // Wrapper for different output areas
    public static String RGOToken(String output) {
        return RUN_GRAMMAR_OUT + TOKEN_DELIM + output;
    }

    public static String RLOToken(String output) {
        return RUN_LOG_OUT + TOKEN_DELIM + output;
    }

    public static String GOToken(String output) {
        return GRAMMAR_OUT + TOKEN_DELIM + output;
    }

    public static String LOToken(String output) {
        return LOG_OUT + TOKEN_DELIM + output;
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("gInferrer");
        stage.setWidth(1200);
        stage.setHeight(575);
        stage.initStyle(StageStyle.DECORATED);
        outputAreas = new HashMap<String, TextArea>();
        outputStacks = new HashMap<String, Stack<String>>();

        TabPane cont = new TabPane();
        cont.setStyle("-fx-font-size:  16;");
        Tab testTab = new Tab("Example", exampleThing());
        Tab settingsTab = new Tab("Settings", settingsThing(stage));
        Tab runTab = new Tab("Run", runScreen());
        Arrays.asList(testTab, runTab, settingsTab).forEach(t -> cont.getTabs().add(t));

        Scene mainScene = new Scene(cont);
        stage.setScene(mainScene);
        stage.show();
        App.loadStartGrammar();

    }

    public VBox settingsThing(Stage primStage) {
        VBox out = new VBox();
        out.setStyle("-fx-alignment: center;");
        Button changeOutputDir = new Button("change");
        changeOutputDir.setOnAction(event -> {
            DirectoryChooser newDir = new DirectoryChooser();
            Constants.setAntlrDir(newDir.showDialog(primStage).getAbsolutePath());
            System.out.println("Changing outputDir to " + Constants.ANTLR_DIR);
        });
        HBox outputDirBox = new HBox(new Label("outputDir: " + Constants.ANTLR_DIR), changeOutputDir);

        outputDirBox.setSpacing(10.0);

        out.getChildren().add(outputDirBox);
        out.getChildren().add(mutationToggle());

        return out;
    }

    public HBox exampleThing() {

        HBox greaterContainer = new HBox();
        VBox mutationInterface = new VBox();
        mutationInterface.setMinWidth(200);
        mutationInterface.getChildren().add(mutationBtns());
        TextArea logOutput = new TextArea();
        outputAreas.put(LOG_OUT, logOutput);
        outputStacks.put(LOG_OUT, new Stack<String>());
        outputLambda logOut = (String toLog) -> handleToken(toLog);
        App.setDemoOut(logOut);
        VBox consoleArea = new VBox(new Label("Output"), logOutput);
        mutationInterface.getChildren().add(consoleArea);
        TextArea grammarOutput = new TextArea();
        outputAreas.put(GRAMMAR_OUT, grammarOutput);
        outputStacks.put(GRAMMAR_OUT, new Stack<String>());
        outputLambda grammarOut = (String toLog) -> {
            handleToken(toLog);
            // String[] data = toLog.split(TOKEN_DELIM);
            // switch (data[0]) {
            //     case outputLambda.CLEAR:
            //         grammarOutput.setText("");
            //         break;

            //     case outputLambda.APPEND:
            //         grammarOutput.appendText(data[1]);
            //         break;

            //     case outputLambda.SET:
            //         grammarOutput.setText(data[1]);
            //         break;
            // }
        };
        grammarOutput.setMinWidth(600);
        greaterContainer.getChildren().add(mutationInterface);
        greaterContainer.getChildren().add(grammarOutput);
        return greaterContainer;

    }

    public HBox runScreen() {
        Task<Void> runOut = backGroundProcess();

        HBox greaterContainer = new HBox();
        VBox mutationInterface = new VBox();
        mutationInterface.setMinWidth(200);
        Button btnRun = new Button("Run");
        mutationInterface.getChildren().add(btnRun);
        TextArea logOutput = new TextArea();
        outputAreas.put(RUN_LOG_OUT, logOutput);
        outputStacks.put(RUN_LOG_OUT, new Stack<String>());

        VBox consoleArea = new VBox(new Label("Output"), logOutput);
        mutationInterface.getChildren().add(consoleArea);

        TextArea grammarOutput = new TextArea();
        outputAreas.put(RUN_GRAMMAR_OUT, grammarOutput);
        outputStacks.put(RUN_GRAMMAR_OUT, new Stack<String>());

        runOut.messageProperty().addListener((obs, prev, newVal) -> {
            handleToken(newVal);
        });

        btnRun.setOnAction(event -> new Thread(runOut).start());

        grammarOutput.setMinWidth(600);
        greaterContainer.getChildren().add(mutationInterface);
        greaterContainer.getChildren().add(grammarOutput);
        return greaterContainer;

    }

    public HBox testThing() {
        HBox greaterContainer = new HBox();
        VBox settingCont = new VBox();
        settingCont.setMinWidth(175);
        settingCont.getChildren().add(mutationBtns());
        settingCont.getChildren().add(mutationToggle());
        TextArea output = new TextArea();
        output.setMinWidth(625);
        greaterContainer.getChildren().add(settingCont);
        greaterContainer.getChildren().add(output);
        return greaterContainer;
    }

    public VBox mutationBtns() {
        Label heading = new Label("Apply mutation");
        heading.setStyle("-fx-font-weight: bold;");

        Button genNewNT = new Button("newNT");
        genNewNT.setOnAction(event -> App.newNTDemo());

        Button symbCount = new Button("symbolCount");
        symbCount.setOnAction(event -> App.symbolCountDemo());

        Button grouping = new Button("grouping");
        grouping.setOnAction(event -> App.groupDemo());

        Button heuristic = new Button("heuristic");
        heuristic.setOnAction(event -> App.demoHeuristic());

        Button crossover = new Button("crossover");
        crossover.setOnAction(event -> App.demoCrossover());

        VBox out = new VBox(heading, genNewNT, symbCount, grouping, heuristic, crossover);
        out.setStyle("-fx-padding: 16;-fx-border-color: black;");
        out.setSpacing(5);

        return out;
    }


    public Button makeButton(String buttonText, EventHandler<ActionEvent> onClick) {
        Button out = new Button(buttonText);
        out.setOnAction(onClick);
        return out;
    }

    public VBox mutationToggle() {
        Label heading = new Label("Toggle mutations");
        heading.setStyle("-fx-font-weight: bold;");

        HBox changeRuleCount = new HBox();
        changeRuleCount.setSpacing(2.0);
        CheckBox changeRuleCountBox = new CheckBox("Rule Count");
        changeRuleCountBox.setSelected(Constants.isCHANGE_RULE_COUNT());
        changeRuleCountBox.setOnAction(event -> Constants.setCHANGE_RULE_COUNT(!Constants.isCHANGE_RULE_COUNT()));
        Label lblPRuleChange = new Label("\tpRuleCountChange: ");
        TextField txfPRuleChange = new TextField(Constants.getP_CHANGE_RULE_COUNT() + "");
        changeRuleCount.getChildren().addAll(changeRuleCountBox, lblPRuleChange, txfPRuleChange);

        HBox changeSymbolCount = new HBox();
        changeSymbolCount.setSpacing(2.0);
        CheckBox changeSymbolCountBox = new CheckBox("Symbol Count");
        changeSymbolCountBox.setSelected(Constants.isCHANGE_SYMBOL_COUNT());
        changeSymbolCountBox.setOnAction(event -> Constants.setCHANGE_SYMBOL_COUNT(!Constants.isCHANGE_SYMBOL_COUNT()));
        Label lblPSymbolCount = new Label("\tpSymbolCountChange: ");
        TextField txfPSymbolChange = new TextField(Constants.getP_CHANGE_SYMBOL_COUNT() + "");
        Label lblAddSymb = new Label("\tpAddSymb: ");
        TextField txfPAddSymb = new TextField(Constants.getP_ADD_SYMBOL() + "");
        changeSymbolCount.getChildren().addAll(changeSymbolCountBox, lblPSymbolCount, txfPSymbolChange, lblAddSymb,
                txfPAddSymb);

        HBox changeGroup = new HBox();
        changeGroup.setSpacing(2.0);
        CheckBox changeGroupBox = new CheckBox("Group");
        changeGroupBox.setSelected(Constants.isGROUP());
        changeGroupBox.setOnAction(event -> Constants.setGROUP(!Constants.isGROUP()));
        Label lblPGroup = new Label("\tpGroup: ");
        TextField txfPGroup = new TextField(Constants.getP_GROUP() + "");
        changeGroup.getChildren().addAll(changeGroupBox, lblPGroup, txfPGroup);

        HBox changeSymbMutation = new HBox();
        changeSymbMutation.setSpacing(2.0);
        CheckBox changeSymbMutationBox = new CheckBox("Symbol Mutation");
        changeSymbMutationBox.setSelected(Constants.isMUTATE());
        changeSymbMutationBox.setOnAction(event -> Constants.setMUTATE(!Constants.isMUTATE()));
        Label lblPSymb = new Label("\tpSymbMutation: ");
        TextField txfPSymb = new TextField(Constants.getP_M() + "");
        changeSymbMutation.getChildren().addAll(changeSymbMutationBox, lblPSymb, txfPSymb);

        HBox changeHeuristic = new HBox();
        changeHeuristic.setSpacing(2.0);
        CheckBox changeHeuristicBox = new CheckBox("Heuristic");
        changeHeuristicBox.setSelected(Constants.isHEURISTIC());
        changeHeuristicBox.setOnAction(event -> Constants.setHEURISTIC(!Constants.isHEURISTIC()));
        Label lblPHeuristic = new Label("\tpHeuristic: ");
        TextField txfPHeur = new TextField(Constants.getP_H() + "");
        changeHeuristic.getChildren().addAll(changeHeuristicBox, lblPHeuristic, txfPHeur);

        HBox changeCrossover = new HBox();
        CheckBox changeCrossoverBox = new CheckBox("Crossover");
        changeCrossoverBox.setSelected(Constants.isCROSSOVER());
        changeCrossoverBox.setOnAction(event -> Constants.setCROSSOVER(!Constants.isCROSSOVER()));
        Label lblPCrossover = new Label("\tpCrossover: ");
        TextField txfPCross = new TextField(Constants.getP_C() + "");
        Label lblCrossMin = new Label("\t score range: ");
        TextField txfCrossMin = new TextField(Constants.getP_C_MIN() + "");
        Label lblPCrossTo = new Label(" to ");
        TextField txfCrossMax = new TextField(Constants.getP_C_MAX() + "");
        changeCrossover.getChildren().addAll(changeCrossoverBox, lblPCrossover, txfPCross, lblCrossMin, txfCrossMin,
                lblPCrossTo, txfCrossMax);

        Button btnSave = new Button("Save");

        VBox out = new VBox(heading, changeRuleCount, changeSymbolCount, changeSymbMutation, changeHeuristic,
                changeCrossover, btnSave);
        out.setStyle("-fx-padding: 16;-fx-border-color: black;");
        out.setSpacing(5);

        return out;
    }

    public VBox settingsContainer(List<Button> comps) {
        VBox out = new VBox();
        comps.forEach(comp -> out.getChildren().add(comp));
        out.setSpacing(5);
        return out;
    }

    public static void main(String[] args) {
        launch();

    }

    public Task<Void> backGroundProcess() {
        Task<Void> out = new Task<Void>() {
            @Override
            public Void call() {
                outputLambda runOut = (String toLog) -> {
                    updateMessage(toLog);
                };

                App.setRunOut(runOut);
                // App.setRunLogOutput(logOut);
                App.demoMainProgram();
                return null;
            }

        };
        return out;

        // return taskThread;
    }

    public void handleToken(String token) {
        logToken(token);
        
        String[] data = token.split(TOKEN_DELIM);
        System.err.println(Arrays.toString(data));
        TextArea target = outputAreas.get(data[0]);
        Stack<String> targetStack = outputStacks.get(data[0]);
        switch (data[1]) {
            case outputLambda.CLEAR:
                target.setText("");
                break;
            case outputLambda.APPEND:
                target.appendText(data[2]);
                break;
            case outputLambda.SET:
                target.setText(data[2]);
                break;
            case outputLambda.UPDATE:
                String curr = target.getText();
                int lastIndex = curr.lastIndexOf("\n");
                String out = curr.substring(0, lastIndex + 1) + data[2];
                target.setText(out);
                break;
            case outputLambda.PUSH:
                targetStack.push(target.getText());
                System.out.println(targetStack + " " + data[0]);
                break;
            case outputLambda.POP:
                target.setText(targetStack.pop());
                System.out.println(targetStack + " " + data[0]);
                break;
        }

    }

    public void logToken(String token) {
        try {
            if(logger == null) {
                logger = new FileWriter(LOG_FILE_NAME, false);
            } else {
                logger = new FileWriter(LOG_FILE_NAME, true);
            }
            token = "\n" + token.replace("\n", "") + "\n";
            logger.write(token);
            logger.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}