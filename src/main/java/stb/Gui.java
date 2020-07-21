package stb;

import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Gui extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("gInferrer");
        stage.setWidth(1200);
        stage.setHeight(575);
        stage.initStyle(StageStyle.DECORATED);

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
        outputLambda logOut = (String toLog) -> logOutput.appendText(toLog + "\n");
        App.setLogOut(logOut);
        VBox consoleArea = new VBox(new Label("Output"), logOutput);
        mutationInterface.getChildren().add(consoleArea);
        TextArea grammarOutput = new TextArea();
        outputLambda grammarOut = (String toLog) -> {
            if (toLog.equals("clear")) {
                grammarOutput.setText("");
            } else {
                grammarOutput.appendText(toLog + "\n");
            }
        };
        App.setGrammarOut(grammarOut);
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
        
        VBox consoleArea = new VBox(new Label("Output"), logOutput);
        mutationInterface.getChildren().add(consoleArea);
        
        
        TextArea grammarOutput = new TextArea();
        
        runOut.messageProperty().addListener((obs, prev, newVal) -> {
            if(newVal.charAt(0)  == 'g') {
                grammarOutput.setText(newVal.substring(1));
            } else {
                logOutput.setText(newVal.substring(1));
            }
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
        Button changeRuleCount = new Button("ruleCount");
        changeRuleCount.setOnAction(event -> App.ruleCountDemo());
        Button changeSymbolCount = new Button("symbolCount");
        changeSymbolCount.setOnAction(event -> App.symbolCountDemo());
        Button group = new Button("group");
        group.setOnAction(event -> App.groupDemo());
        Button symbolMutation = new Button("symbolMutation");
        symbolMutation.setOnAction(event -> App.symbMutateDemo());
        Button heuristic = new Button("heuristic");
        heuristic.setOnAction(event -> App.demoHeuristic());
        Button crossover = new Button("crossover");
        crossover.setOnAction(event -> App.demoCrossover());
        VBox out = new VBox(heading, changeRuleCount, changeSymbolCount, group, symbolMutation, heuristic, crossover);
        out.setStyle("-fx-padding: 16;-fx-border-color: black;");
        out.setSpacing(5);

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
            @Override public Void call() {
                outputLambda grammarOut = (String toLog) -> {
                    if (toLog.equals("clear")) {
                        updateMessage("g");
                    } else {
                        updateMessage("g" + toLog);
                    }
                };

                outputLambda logOut = (String toLog) -> {
                    if (toLog.equals("clear")) {
                        updateMessage("l");
                    } else {
                        updateMessage("l" + toLog);
                    }
                };
                App.setRunGrammarOutput(grammarOut);
                App.setRunLogOutput(logOut);
                App.demoMainProgram();
                return null;
            }

        };
        return out;
        
        // return taskThread;
    }

}