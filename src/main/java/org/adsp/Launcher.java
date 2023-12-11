package org.adsp;

import ilog.cp.IloCP;
import org.adsp.cpoptimizer.OptionalIntervalModel;
import org.adsp.cpoptimizer.OptionalIntervalModelRelaxed;
import org.adsp.datamodel.Instance;
import org.adsp.datamodel.Solution;
import org.adsp.tools.json.JsonReader;

public class Launcher {
    public static void main(String[] args){
        //Arguments: instance/sol model -st -sil -t [time limit] -t2 [2nd time limit] -f [fail limit] -s [search] -n [n workers] -out [outputPath]
        //Reading parameters:
        if(args.length < 2){
            System.out.println("No instance file path or model provided!");
            return;
        }

        Solution sol = null;
        boolean startSol = false;
        boolean silent = false;
        int timeLimit = Integer.MAX_VALUE;
        int timeLimit2 = Integer.MAX_VALUE;
        int failLimit = Integer.MAX_VALUE;
        String search = "Auto";
        int nWorkers = 1;
        String out = "default/";

        String model = args[1];

        int i = 2;
        while(i < args.length){
            String arg = args[i];
            switch (arg){
                case "-st":
                    startSol = true;
                    i++;
                    break;
                case "-sil":
                    silent = true;
                    i++;
                    break;
                case "-t":
                    timeLimit = Integer.parseInt(args[i+1]);
                    i+=2;
                    break;
                case "-t2":
                    timeLimit2 = Integer.parseInt(args[i+1]);
                    i+=2;
                    break;
                case "-f":
                    failLimit = Integer.parseInt(args[i+1]);
                    i+=2;
                    break;
                case "-s":
                    search = args[i+1];
                    i+=2;
                    break;
                case "-n":
                    nWorkers = Integer.parseInt(args[i+1]);
                    i+=2;
                    break;
                case "-out":
                    out = args[i+1];
                    i+=2;
                    break;
                default:
                    System.out.println("Argument " + arg + " is not recognized and will be ignored.");
                    i++;
            }
        }
        if(startSol) sol = JsonReader.readSolutionFile(args[0]);
        Instance instance = startSol ? sol.instance() : JsonReader.readInstanceFile(args[0]);
        if(instance == null || instance.id().equals("error")){
            return;
        }
        if(!instance.version().equals("0.4")){
            System.out.println("Incompatible instance format! Instance version is " + instance.version() + ", must be 0.4");
            return;
        }

        switch(model){
            case "CPOOptInterModel":
                OptionalIntervalModel solver = new OptionalIntervalModel(instance);
                if(startSol){
                    solver.setSolution(sol);
                    solver.setStartSol(true);
                }
                if(timeLimit < Integer.MAX_VALUE) solver.setTimeLimit(timeLimit);
                if(timeLimit2 < Integer.MAX_VALUE) solver.setSecondTimeLimit(timeLimit2);
                if(failLimit < Integer.MAX_VALUE) solver.setFailLimit(failLimit);
                if(nWorkers != 4) solver.setWorkers(nWorkers);
                solver.setOutputPath(out + search + "/");
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
                    solverRelaxed.setSolution(sol);
                    solverRelaxed.setStartSol(true);
                }
                if(timeLimit < Integer.MAX_VALUE) solverRelaxed.setTimeLimit(timeLimit);
                if(timeLimit2 < Integer.MAX_VALUE) solverRelaxed.setSecondTimeLimit(timeLimit2);
                if(failLimit < Integer.MAX_VALUE) solverRelaxed.setFailLimit(failLimit);
                if(nWorkers != 4) solverRelaxed.setWorkers(nWorkers);
                solverRelaxed.setOutputPath(out + search + "/");
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
        }
    }
}
