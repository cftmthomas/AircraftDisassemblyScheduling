package org.adsp.visu.runnable;

import ilog.cp.IloCP;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.Chart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.adsp.cpoptimizer.OptionalIntervalModel;
import org.adsp.cpoptimizer.OptionalIntervalModelRelaxed;
import org.adsp.datamodel.*;
import org.adsp.tools.TimeWindow;
import org.adsp.tools.json.JsonReader;
import org.adsp.visu.charts.CumulFunctionChart;
import org.adsp.visu.charts.GanttChart;
import org.adsp.visu.charts.GanttChart.*;

import java.util.*;

public class Visu extends Application {
    //Parameters:
    private static Instance instance;
    private static String model;
    private static Solution solution;
    private static int failLimit = Integer.MAX_VALUE;
    private static int timeLimit = Integer.MAX_VALUE;
    private static int timeLimit2 = Integer.MAX_VALUE;
    private static boolean startSol = false;
    private static boolean silent = false;
    private static String search = "Auto";
    private static int nWorkers = 1;
    private static String out = "default/";

    //Visualisation parameters:
    public static final int WINDOW_WIDTH = 1920;
    public static final int WINDOW_HEIGHT = 1080;
    public static final int ACTIVITY_HEIGHT = 20;
    public static final int CUMUL_CHART_HEIGHT = 200;

    //Data helpers:
    private static final Map<String, Integer> opCardToId = new HashMap<>();
    private static XYChart.Data<Number, String>[] operationsData;
    private static List<Integer>[] resourcesAssigned;
    private static final List<Activity> afActivities = new ArrayList<>();
    private static final List<Activity> lrActivities = new ArrayList<>();

    //Charts:
    private static GanttChart<Number, String> operationsGant;
    private static GanttChart<Number, String> resourcesGant;
    private static CumulFunctionChart balanceAFChart;
    private static CumulFunctionChart balanceLRChart;
    private static CumulFunctionChart[] occupancyCharts;

    @Override public void start(Stage stage) {
        //Initializing visu:
        initOps(stage);
        initAssign();
        initCumul();

        //Launching model:
        switch(model){
            case "CPOOptInterModel":
                OptionalIntervalModel solver = new OptionalIntervalModel(instance);
                if(startSol){
                    solver.setSolution(solution);
                    solver.setStartSol(true);
                }
                if(timeLimit < Integer.MAX_VALUE) solver.setTimeLimit(timeLimit);
                if(timeLimit2 < Integer.MAX_VALUE) solver.setSecondTimeLimit(timeLimit2);
                if(failLimit < Integer.MAX_VALUE) solver.setFailLimit(failLimit);
                if(nWorkers != 4) solver.setWorkers(nWorkers);
                solver.setOutputPath(out + search + "/");
                solver.setOnSolution(Visu::onSolution);
                switch(search){
                    case "LEX-DF":
                        solver.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solver.lexSearch();
                        break;
                    case "LEX-FD":
                        solver.setFailureDirected(true);
                        solver.lexSearch();
                        break;
                    case "ILEX-AUTO":
                        solver.invertedLexSearch();
                        break;
                    case "ILEX-DF":
                        solver.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solver.invertedLexSearch();
                        break;
                    case "ILEX-FD":
                        solver.setFailureDirected(true);
                        solver.invertedLexSearch();
                        break;
                    case "MK-AUTO":
                        solver.makespanSearch();
                        break;
                    case "MK-DF":
                        solver.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solver.makespanSearch();
                        break;
                    case "MK-FD":
                        solver.setFailureDirected(true);
                        solver.makespanSearch();
                        break;
                    case "CST-AUTO":
                        solver.costSearch();
                        break;
                    case "CST-DF":
                        solver.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solver.costSearch();
                        break;
                    case "CST-FD":
                        solver.setFailureDirected(true);
                        solver.costSearch();
                        break;
                    default:
                        solver.lexSearch();
                }
                solver.close();
                break;
            case "CPOOptInterModelRelax":
                OptionalIntervalModelRelaxed solverRelaxed = new OptionalIntervalModelRelaxed(instance);
                if(startSol){
                    solverRelaxed.setSolution(solution);
                    solverRelaxed.setStartSol(true);
                }
                if(timeLimit < Integer.MAX_VALUE) solverRelaxed.setTimeLimit(timeLimit);
                if(timeLimit2 < Integer.MAX_VALUE) solverRelaxed.setSecondTimeLimit(timeLimit2);
                if(failLimit < Integer.MAX_VALUE) solverRelaxed.setFailLimit(failLimit);
                if(nWorkers != 4) solverRelaxed.setWorkers(nWorkers);
                solverRelaxed.setOutputPath(out + search + "/");
                solverRelaxed.setOnSolution(Visu::onSolution);
                switch(search){
                    case "LEX-DF":
                        solverRelaxed.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solverRelaxed.lexSearch();
                        break;
                    case "LEX-FD":
                        solverRelaxed.setFailureDirected(true);
                        solverRelaxed.lexSearch();
                        break;
                    case "ILEX-AUTO":
                        solverRelaxed.invertedLexSearch();
                        break;
                    case "ILEX-DF":
                        solverRelaxed.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solverRelaxed.invertedLexSearch();
                        break;
                    case "ILEX-FD":
                        solverRelaxed.setFailureDirected(true);
                        solverRelaxed.invertedLexSearch();
                        break;
                    case "MK-AUTO":
                        solverRelaxed.makespanSearch();
                        break;
                    case "MK-DF":
                        solverRelaxed.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solverRelaxed.makespanSearch();
                        break;
                    case "MK-FD":
                        solverRelaxed.setFailureDirected(true);
                        solverRelaxed.makespanSearch();
                        break;
                    case "CST-AUTO":
                        solverRelaxed.costSearch();
                        break;
                    case "CST-DF":
                        solverRelaxed.setSearchType(IloCP.ParameterValues.DepthFirst);
                        solverRelaxed.costSearch();
                        break;
                    case "CST-FD":
                        solverRelaxed.setFailureDirected(true);
                        solverRelaxed.costSearch();
                        break;
                    default:
                        solverRelaxed.lexSearch();
                }
                solverRelaxed.close();
                break;
            case "InstanceStats":
                System.out.println("Characteristics of instance " + instance.name());
                System.out.println("Number of operations " + instance.nOps());
                System.out.println("Makespan lower bound " + instance.makespanLB());
                System.out.println("Cost lower bound " + instance.costLB());
                break;
            case "DisplaySol": {
                displaySol();
                break;
            }
        }
    }

    public static void main(String[] args) {
        //Arguments: instance/sol model [-st -sil -t <time limit> -t2 <2nd time limit> -f <fail limit> -s <search> -n <n workers> -out <output path>]
        //Reading parameters:
        if(args.length < 2){
            System.out.println("No instance file path or model provided!");
            return;
        }

        model = args[1];

        int a = 2;
        while(a < args.length){
            String arg = args[a];
            switch (arg){
                case "-st":
                    startSol = true;
                    a++;
                    break;
                case "-sil":
                    silent = true;
                    a++;
                    break;
                case "-t":
                    timeLimit = Integer.parseInt(args[a+1]);
                    a+=2;
                    break;
                case "-t2":
                    timeLimit2 = Integer.parseInt(args[a+1]);
                    a+=2;
                    break;
                case "-f":
                    failLimit = Integer.parseInt(args[a+1]);
                    a+=2;
                    break;
                case "-s":
                    search = args[a+1];
                    a+=2;
                    break;
                case "-n":
                    nWorkers = Integer.parseInt(args[a+1]);
                    a+=2;
                    break;
                case "-out":
                    out = args[a+1];
                    a+=2;
                    break;
                default:
                    System.out.println("Argument " + arg + " is not recognized and will be ignored.");
                    a++;
            }
        }
        boolean parseSol = startSol || model.equals("DisplaySol");
        if(parseSol) solution = JsonReader.readSolutionFile(args[0]);
        instance = parseSol ? solution.instance() : JsonReader.readInstanceFile(args[0]);
        if(instance == null || instance.id().equals("error")){
            return;
        }
        if(!instance.version().equals("0.4")){
            System.out.println("Incompatible instance format! Instance version is " + instance.version() + ", must be 0.4");
            return;
        }
        //Arguments: instance/solution model [time limit] [fail limit]
        //Reading instance file:
        if(args.length < 2){
            System.out.println("No file path or model provided!");
            return;
        }

        for(int i = 0; i < instance.operations().length; i++) opCardToId.put(instance.operations()[i].card(), i);
        operationsData = new XYChart.Data[instance.operations().length];
        resourcesAssigned = new List[instance.operations().length];
        occupancyCharts = new CumulFunctionChart[instance.locations().length];

        launch(args);
    }

    //Operations gant:
    private void initOps(Stage stage){
        final NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time");
        xAxis.setTickLabelFill(Color.CHOCOLATE);

        final CategoryAxis yAxis = new CategoryAxis();
        yAxis.setLabel("Operation");
        yAxis.setTickLabelFill(Color.CHOCOLATE);
        yAxis.setTickLabelGap(10);

        ArrayList<String> operationNames = new ArrayList<>();
        //Iterating from the end to display operation names in the correct order:
        for(int i = instance.operations().length-1; i >= 0; i--) operationNames.add(instance.operations()[i].card());
        yAxis.setCategories(FXCollections.observableArrayList(operationNames));

        operationsGant = new GanttChart<>(xAxis, yAxis);
        operationsGant.setMinWidth(instance.maxTime());
        operationsGant.setMinHeight(instance.operations().length * ACTIVITY_HEIGHT);
        operationsGant.setBlockHeight(ACTIVITY_HEIGHT);
        operationsGant.setTitle("Operations planning");
        operationsGant.setLegendVisible(false);

        //chart.getStylesheets().add(getClass().getResource("ganttchart.css").toExternalForm());
        operationsGant.setStylesheet("ganttchart.css");

        ScrollPane operationsPane = new ScrollPane(operationsGant);
        operationsPane.setFitToWidth(true);
        operationsPane.setFitToHeight(true);
        operationsPane.setPannable(true);
        Scene operations  = new Scene(operationsPane, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(operations);
        stage.setTitle("Operations");
        stage.show();
    }

    //Resources assignation:
    private void initAssign(){
        final NumberAxis resXAxis = new NumberAxis();
        resXAxis.setLabel("Time");
        resXAxis.setTickLabelFill(Color.CHOCOLATE);

        final CategoryAxis resYAxis = new CategoryAxis();
        resYAxis.setLabel("Resource");
        resYAxis.setTickLabelFill(Color.CHOCOLATE);
        resYAxis.setTickLabelGap(10);

        ArrayList<String> resourceNames = new ArrayList<>();
        //Iterating from the end to display resources in the correct order:
        for(int i = instance.resources().length-1; i >= 0; i--) resourceNames.add(instance.resources()[i].name());
        resYAxis.setCategories(FXCollections.observableArrayList(resourceNames));

        resourcesGant = new GanttChart<Number,String>(resXAxis,resYAxis);
        resourcesGant.setMinWidth(instance.maxTime());
        resourcesGant.setMinHeight(instance.resources().length * ACTIVITY_HEIGHT);
        resourcesGant.setBlockHeight(ACTIVITY_HEIGHT);
        resourcesGant.setTitle("Resources assignation");
        resourcesGant.setLegendVisible(false);

        //chart.getStylesheets().add(getClass().getResource("ganttchart.css").toExternalForm());
        resourcesGant.setStylesheet("ganttchart.css");

        ScrollPane resourcesPane = new ScrollPane(resourcesGant);
        resourcesPane.setFitToWidth(true);
        resourcesPane.setFitToHeight(true);
        resourcesPane.setPannable(true);
        Stage assignationStage = new Stage();
        Scene assignations = new Scene(resourcesPane, WINDOW_WIDTH, WINDOW_HEIGHT);
        assignationStage.setScene(assignations);
        assignationStage.setTitle("Resources assignments");
        assignationStage.show();
    }

    //Cumulative graphs:
    private void initCumul(){
        VBox profileContainer = new VBox();

        //Balance Aft - Forward chart:
        final NumberAxis balAFXAxis = new NumberAxis();
        balAFXAxis.setLabel("Mass diff");
        balAFXAxis.setTickLabelFill(Color.CHOCOLATE);
        final CategoryAxis balAFYAxis = new CategoryAxis();
        balAFYAxis.setLabel("Time");
        balAFYAxis.setTickLabelFill(Color.CHOCOLATE);
        balanceAFChart = new CumulFunctionChart(instance.balanceAF(), -instance.balanceAF());
        balanceAFChart.setTitle("Balance Aft - Forward");
        balanceAFChart.setOnMouseClicked(this::handleChartClick); //Giving focus when mouse clicked
        ScrollPane balAFPane = new ScrollPane(balanceAFChart);
        balAFPane.setMinHeight(CUMUL_CHART_HEIGHT);
        balAFPane.setFitToWidth(true);
        balAFPane.setFitToHeight(true);
        balAFPane.setPannable(true);
        profileContainer.getChildren().add(balAFPane);

        //Balance Left - Right chart:
        final NumberAxis balLRXAxis = new NumberAxis();
        balLRXAxis.setLabel("Mass diff");
        balLRXAxis.setTickLabelFill(Color.CHOCOLATE);
        final CategoryAxis balLRYAxis = new CategoryAxis();
        balLRYAxis.setLabel("Time");
        balLRYAxis.setTickLabelFill(Color.CHOCOLATE);
        balanceLRChart = new CumulFunctionChart(instance.balanceLR(), -instance.balanceLR());
        balanceLRChart.setTitle("Balance Left - Right");
        balanceLRChart.setOnMouseClicked(this::handleChartClick); //Giving focus when mouse clicked
        ScrollPane balLRPane = new ScrollPane(balanceLRChart);
        balLRPane.setMinHeight(CUMUL_CHART_HEIGHT);
        balLRPane.setFitToWidth(true);
        balLRPane.setFitToHeight(true);
        balLRPane.setPannable(true);
        profileContainer.getChildren().add(balLRPane);

        //Location occupancy charts:
        for(int loc = 0; loc < instance.locations().length; loc++) if(!instance.locations()[loc].name().isBlank()){
            final NumberAxis xAxis = new NumberAxis();
            xAxis.setLabel("Occupancy");
            xAxis.setTickLabelFill(Color.CHOCOLATE);
            final CategoryAxis yAxis = new CategoryAxis();
            yAxis.setLabel("Time");
            yAxis.setTickLabelFill(Color.CHOCOLATE);
            CumulFunctionChart occupancyChart = new CumulFunctionChart(instance.locations()[loc].capacity()+1, 0);
            occupancyChart.setTitle("Occupancy of " + instance.locations()[loc].name());
            occupancyCharts[loc] = occupancyChart;
            occupancyChart.setOnMouseClicked(this::handleChartClick); //Giving focus when mouse clicked
            ScrollPane occPane = new ScrollPane(occupancyChart);
            occPane.setMinHeight(CUMUL_CHART_HEIGHT);
            occPane.setFitToWidth(true);
            occPane.setFitToHeight(true);
            occPane.setPannable(true);
            profileContainer.getChildren().add(occPane);
        }

        ScrollPane profilePane = new ScrollPane(profileContainer);
        profilePane.setFitToWidth(true);
        profilePane.setFitToHeight(true);
        Stage profileStage = new Stage();
        Scene profiles  = new Scene(profilePane, WINDOW_WIDTH, WINDOW_HEIGHT);
        profileStage.setScene(profiles);
        profileStage.setTitle("Balance and Occupancy profiles");
        profileStage.show();
    }

    public static void displaySol(){
        if(solution.activities().length > 0) {
            operationsGant.setTitle("Operations planning (makespan: " + solution.makespan() + ", cost: " + solution.cost() + ")");
            resourcesGant.setTitle("Resources assignation (makespan: " + solution.makespan() + ", cost: " + solution.cost() + ")");
            displayResources();
            displayOperations();
            displayBalance();
            displayOccupancy();
        } else System.out.println("Solution is empty. Nothing to display");
    }

    //Displaying operations:
    private static void displayOperations(){
        //Clearing previous data:
        afActivities.clear();
        lrActivities.clear();
        operationsGant.getData().clear();

        //Parsing activities:
        for(Activity act : solution.activities()){
            int op = act.operation();
            String card = instance.operations()[op].card();

            //Building label:
            StringBuilder label = new StringBuilder("Operation " + card + "\nResources assigned:");
            if(resourcesAssigned[op] != null && !resourcesAssigned[op].isEmpty()){
                for(int res: resourcesAssigned[op]) label.append("\n").append(instance.resources()[res].name());
            }

            //Adding data:
            operationsData[op] = new XYChart.Data<>(
                    act.start(),
                    card,
                    new TimeBlock( act.end() - act.start(), "status-green", label.toString())
            );

            //Adding activity to balance lists if needed:
            String zone = instance.locations()[instance.operations()[op].location()].zone();
            if(instance.operations()[op].mass() > 0) {
                if (zone.equals("AFT") || zone.equals("FWD")) afActivities.add(act);
                else if (zone.equals("LH") || zone.equals("RH")) lrActivities.add(act);
            }
        }

        //Adding new data to chart:
        operationsGant.addAll(Arrays.stream(operationsData).map(opData ->{
            XYChart.Series<Number, String> opSeries = new XYChart.Series<>();
            opSeries.getData().add(opData);
            return opSeries;
        }).toList());
        CategoryAxis yAxis = (CategoryAxis) operationsGant.getYAxis();

        //Sorting operations by start time on chart:
        yAxis.getCategories().sort((op1, op2) ->
                operationsData[opCardToId.get(op2)].getXValue().intValue() - operationsData[opCardToId.get(op1)].getXValue().intValue()
        );
    }

    //Displaying resources:
    private static void displayResources(){
        //Clearing previous data:
        for(List<Integer> list: resourcesAssigned) if(list != null) list.clear();
        resourcesGant.getData().clear();

        //Creating resources data series:
        XYChart.Series<Number, String>[] resSeries = new XYChart.Series[instance.resources().length];
        for(int r = 0; r < instance.resources().length; r++){
            resSeries[r] = new XYChart.Series<>();
        }

        //Parsing assignments:
        for(Assignment assignment: solution.assignments()){
            int res = assignment.resource();
            int op = assignment.operation();
            resSeries[res].getData().add(new XYChart.Data<>(
                    assignment.start(),
                    instance.resources()[res].name(),
                    new TimeBlock(
                            assignment.end() - assignment.start(),
                            "status-green",
                            "Assigned to operation " + instance.operations()[op].card()
                    )
            ));
            if(resourcesAssigned[op] == null) resourcesAssigned[op] = new ArrayList<>();
            resourcesAssigned[op].add(res);
        }

        //Adding unavailabilies:
        for(int res = 0; res < instance.resources().length; res++){
            for(TimeWindow unav: instance.resources()[res].unavailable()) {
                resSeries[res].getData().add(new XYChart.Data<>(
                        unav.start(),
                        instance.resources()[res].name(),
                        new TimeBlock(unav.duration(), "status-red", "Unavailable")
                ));
            }
        }

        resourcesGant.addAll(List.of(resSeries)); //Adding new data to chart
    }

    //Displaying balance cumulative profiles:
    private static void displayBalance(){
        //Aft - forward balance:
        balanceAFChart.clearData();
        afActivities.sort(Comparator.comparingInt(Activity::start));
        int[] afStarts = new int[afActivities.size()+2];
        String[] afOps = new String[afActivities.size()+2];
        double[] afMassValues = new double[afActivities.size()+2];
        afStarts[0] = 0;
        afOps[0] = "At dismantling start";
        afMassValues[0] = 0;
        for(int i = 1; i < afOps.length-1; i++){
            Activity act = afActivities.get(i-1);
            Operation op = instance.operations()[act.operation()];
            String zone = instance.locations()[op.location()].zone();
            afOps[i] = "Operation " + op.card();
            afStarts[i] = act.start();
            if(zone.equals("AFT")) afMassValues[i] = -op.mass();
            else afMassValues[i] = op.mass();
        }
        afStarts[afStarts.length-1] = solution.makespan();
        afOps[afOps.length-1] = "At dismantling end";
        afMassValues[afMassValues.length-1] = 0;
        balanceAFChart.addSteps("Balance Aft - Forward", afStarts, afMassValues, afOps);

        //Left - right balance:
        balanceLRChart.clearData();
        lrActivities.sort(Comparator.comparingInt(Activity::start));
        int[] lrStarts = new int[lrActivities.size()+2];
        String[] lrOps = new String[lrActivities.size()+2];
        double[] lrMassValues = new double[lrActivities.size()+2];
        lrStarts[0] = 0;
        lrOps[0] = "At dismantling start";
        lrMassValues[0] = 0;
        for(int i = 1; i < lrOps.length-1; i++){
            Activity act = lrActivities.get(i-1);
            Operation op = instance.operations()[act.operation()];
            String zone = instance.locations()[op.location()].zone();
            lrOps[i] = "Operation " + op.card();
            lrStarts[i] = act.start();
            if(zone.equals("LH")) lrMassValues[i] = -op.mass();
            else lrMassValues[i] = op.mass();
        }
        lrStarts[lrStarts.length-1] = solution.makespan();
        lrOps[lrOps.length-1] = "At dismantling end";
        lrMassValues[lrMassValues.length-1] = 0;
        balanceLRChart.addSteps("Balance Left - Right", lrStarts, lrMassValues, lrOps);
    }

    private static void displayOccupancy(){
        for(int loc = 0; loc < instance.locations().length; loc++){
            if(occupancyCharts[loc] != null){
                occupancyCharts[loc].clearData();
                //Creating time steps for activities:
                List<TimeStep> timeSteps = new ArrayList<>();
                timeSteps.add(new TimeStep(0, 0, "At dismantling start"));
                for(Activity act : solution.activities()){
                    Operation op = instance.operations()[act.operation()];
                    if(op.location() == loc){
                        timeSteps.add(new TimeStep(act.start(), op.occupancy(), "Start of operation " + op.card()));
                        timeSteps.add(new TimeStep(act.end(), -op.occupancy(), "End of operation " + op.card()));
                    }
                }
                timeSteps.add(new TimeStep(solution.makespan(), 0, "At dismantling end"));
                timeSteps.sort(Comparator.comparingInt(TimeStep::time)); //Sorting steps by time

                //Adding data to chart:
                occupancyCharts[loc].addSteps(
                        "Occupancy " + instance.locations()[loc].name(),
                        timeSteps.stream().mapToInt(TimeStep::time).toArray(),
                        timeSteps.stream().mapToDouble(TimeStep::value).toArray(),
                        timeSteps.stream().map(TimeStep::label).toArray(String[]::new)
                );
            }
        }
    }

    public static void onSolution(Solution sol){
        solution = sol;
        displaySol();
    }

    private void handleChartClick(MouseEvent event) {
        // Request focus when a chart is clicked
        Object source = event.getSource();
        if(source instanceof Chart) ((Chart) source).requestFocus();
    }

    private record TimeStep(int time, int value, String label){}
}