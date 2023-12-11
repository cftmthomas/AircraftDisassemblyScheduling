package org.adsp.cpoptimizer;

import ilog.cp.*;
import ilog.concert.*;
import org.adsp.datamodel.*;
import org.adsp.tools.json.JsonWriter;

import java.util.*;
import java.util.function.Consumer;

/**
 * CP Optimizer model for the Aircraft Disassembly Scheduling Problem.
 */
public class OptionalIntervalModel {
    //Parameters:
    private boolean silent = false;
    private String outputPath =  "output/default/";
    private boolean startSol;
    private Consumer<Solution> onSolution = (Solution) -> {};
    private long searchStart = System.nanoTime();
    private double timeLimit = 60;
    private double secondTimeLimit = 60; //Only used in lexicographical search
    private int failLimit = Integer.MAX_VALUE;
    private int workers = 4;
    private IloCP.ParameterValues searchType = IloCP.ParameterValues.Auto;
    private boolean FailureDirected = false;

    //Data:
    private final Instance instance;
    private final int nResources;
    private final int nLocations;
    private final int nOperations;

    //Model:
    private IloCP cp; //Cp solver

    private final IloCumulFunctionExpr[] locUsage; //Occupancy of locations.

    private final IloIntervalVar[] operations; //Main Activities.
    private final IntervalVarList[] resourceActivities; //Optional activities.

    private final IntExprList ends = new IntExprList(); //End time of activities. Used for makespan objective.
    private final IntExprList costs = new IntExprList(); //Costs of optional activities. Used for cost objective.

    //Cumulative differences of mass between balance zones:
    //Note that the range is shifted to avoid negative cumulative values:
    //The range is between 0 and 2 * the maximum mass difference.
    //The equilibrium value is equal to the maximum mass difference.
    private IloCumulFunctionExpr diffAF;
    private IloCumulFunctionExpr diffLR;

    //Objectives:
    private final IloIntExpr makespan; //Main objective: minimize makespan.
    private final IloIntExpr cost; //Secondary objective: minimize costs.

    //Solution management:
    private IloSolution currentSol;
    private Solution lastSol;
    private final ArrayList<LogEntry> log = new ArrayList<>();

    public OptionalIntervalModel(Instance instance){

        //Preparing data:
        this.instance = instance;
        nResources = instance.resources().length;
        nLocations = instance.locations().length;
        nOperations = instance.operations().length;

        //Initializing solver and model:
        try {
            cp = new IloCP();

            diffAF = cp.cumulFunctionExpr();
            diffLR = cp.cumulFunctionExpr();
            diffAF = cp.sum(diffAF, cp.step(0, instance.balanceAF()));
            diffLR = cp.sum(diffLR, cp.step(0, instance.balanceLR()));

            locUsage = new IloCumulFunctionExpr[nLocations];
            Arrays.fill(locUsage, cp.cumulFunctionExpr());

            //Initializing main activities:
            operations = new IloIntervalVar[nOperations];
            for (int i = 0; i < nOperations; i++) {
                operations[i] = cp.intervalVar("O[" + i + "]");
            }
            for (int i = 0; i < nOperations; i++) {
                //Creating interval var
                Operation op = instance.operations()[i];
                IloIntervalVar act = operations[i];
                act.setSizeMin(op.duration());
                act.setSizeMax(op.duration());
                ends.add(cp.endOf(act));

                //Setting up mass impact:
                if(op.mass() > 0) {
                    if (instance.isForward(i)) diffAF = cp.sum(diffAF, cp.stepAtStart(act, op.mass()));
                    if (instance.isAft(i)) diffAF = cp.diff(diffAF, cp.stepAtStart(act, op.mass()));
                    if (instance.isRight(i)) diffLR = cp.sum(diffLR, cp.stepAtStart(act, op.mass()));
                    if (instance.isLeft(i)) diffLR = cp.diff(diffLR, cp.stepAtStart(act, op.mass()));
                }

                //setting up occupancy consumption:
                locUsage[op.location()] = cp.sum(locUsage[op.location()], cp.pulse(act, op.occupancy()));

                for (int j : op.precedences()) {
                    cp.add(cp.endBeforeStart(operations[j], act));
                }
            }

            //Initializing optional activities:
            resourceActivities = new IntervalVarList[nResources];
            for (int r = 0; r < nResources; r++) {
                resourceActivities[r] = new IntervalVarList();
            }

            //Allocating requirements to resources:
            for (int i = 0; i < nOperations; i++) {
                Operation op = instance.operations()[i];
                int nReqOp = op.resources().length;
                for (int r = 0; r < nReqOp; r++) {
                    Requirement req = op.resources()[r];
                    Set<String> reqCategories = new HashSet<>(Arrays.asList(req.category()));
                    IntervalVarList optionalActivities = new IntervalVarList();
                    for (int j = 0; j < nResources; j++) {
                        Resource res = instance.resources()[j];

                        // If the resource is compatible with this requirement:
                        if (reqCategories.contains(res.category())) {
                            IloIntervalVar optionalAct = cp.intervalVar(op.duration(), "R[" + i + "," + r + "," + j + "]");
                            optionalAct.setOptional();
                            optionalActivities.add(optionalAct);
                            resourceActivities[j].add(optionalAct);
                            costs.add(cp.prod(cp.presenceOf(optionalAct), op.duration() * res.cost()));
                        }
                    }
                    cp.add(cp.alternative(operations[i], optionalActivities.toArray(), req.quantity()));
                }
            }

            for (int r = 0; r < nResources; r++) {
                //Adding unavailability activities:
                Resource res = instance.resources()[r];
                for(int u = 0; u < res.unavailable().length; u++){
                    IloIntervalVar unav = cp.intervalVar("U[" + r + ":" + res.unavailable()[u].start() + ";" + res.unavailable()[u].start() + "]");
                    unav.setStartMin(res.unavailable()[u].start());
                    unav.setStartMax(res.unavailable()[u].start());
                    unav.setEndMin(res.unavailable()[u].end());
                    unav.setEndMax(res.unavailable()[u].end());
                    resourceActivities[r].add(unav);
                }
                //Creating seqVar and adding noOverlap constraint
                IloIntervalSequenceVar seq = cp.intervalSequenceVar(resourceActivities[r].toArray(), "S[" + r + "]");
                cp.add(cp.noOverlap(seq));
            }

            //Adding balance constraints:
            cp.add(cp.le(diffAF, instance.balanceAF()*2));
            cp.add(cp.ge(diffAF, 0));
            cp.add(cp.le(diffLR, instance.balanceLR()*2));
            cp.add(cp.ge(diffLR, 0));

            //Adding occupancy constraints:
            for(int l = 0; l < instance.locations().length; l++)
                cp.add(cp.le(locUsage[l], instance.locations()[l].capacity()));

            //Setting objectives:
            makespan = cp.max(ends.toArray()); //Primary objective: minimize makespan
            cp.add(cp.le(makespan, instance.maxTime()));

            cost = cp.sum(costs.toArray()); //Secondary objective: minimize costs
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    public void close(){
        if (cp != null) cp.end();
    }

    private void processSol() throws IloException {
        double currentSearchTime = (double) timeElapsed() / 1000000000;

        //Processing sol:
        Activity[] activities = new Activity[nOperations];
        for (int i = 0; i < nOperations; i++) {
            activities[i] = new Activity(i, cp.getStart(operations[i]), cp.getEnd(operations[i]));
        }
        ArrayList<Assignment> assignments = new ArrayList<>();
        for (int r = 0; r < nResources; r++) {
            for (IloIntervalVar resAct : resourceActivities[r]) {
                String actName = resAct.getName();
                if (actName.startsWith("R") && cp.isPresent(resAct)) {
                    int op = Integer.parseInt(actName.substring(2, actName.length() - 1).split(",")[0]);
                    assignments.add(new Assignment(r, op, cp.getStart(resAct), cp.getEnd(resAct)));
                }
            }
        }
        Solution sol = new Solution(instance, activities, assignments.toArray(new Assignment[0]), (int) cp.getValue(makespan), (int) cp.getValue(cost));

        //Logging sol:
        log.add(new LogEntry(currentSearchTime, sol.makespan(), sol.cost(), cp.getObjGap() == 0));

        //Printing sol:
        if(!silent) {
            System.out.println("new solution found at " + currentSearchTime);
            System.out.println("Makespan \t: " + sol.makespan());
            System.out.println("Cost \t: " + sol.cost());
//            JsonWriter.printSolution(sol);
        }

        //Saving sol:
        lastSol = sol;

        //Saving current sol:
        currentSol = cp.solution();
        for (IloIntervalVar act : operations) {
            if (cp.isPresent(act)) {
                currentSol.setPresent(act);
                currentSol.setStart(act, cp.getStart(act));
            } else {
                currentSol.setAbsent(act);
            }
        }
        for(IntervalVarList acts: resourceActivities) {
            for (IloIntervalVar act : acts) {
                if (cp.isPresent(act)) {
                    currentSol.setPresent(act);
                    currentSol.setStart(act, cp.getStart(act));
                    currentSol.setEnd(act, cp.getEnd(act));
                } else {
                    currentSol.setAbsent(act);
                }
            }
        }
        onSolution.accept(sol);
    }

    private void assignStartSol(Solution sol){
        try {
            currentSol = cp.solution();
            for (Activity act : sol.activities()) {
                currentSol.setPresent(operations[act.operation()]);
                currentSol.setStart(operations[act.operation()], act.start());
            }
            for(Assignment ass : sol.assignments()) {
                IloIntervalVar var = getResourceAct(ass.operation(), ass.requirement(), ass.resource());
                if(var != null){
                    currentSol.setPresent(var);
                    currentSol.setStart(var, ass.start());
                    currentSol.setEnd(var, ass.end());
                }
            }
            cp.add(cp.le(makespan, sol.makespan()));
            cp.add(cp.le(cost, sol.cost()));
            cp.setStartingPoint(currentSol);
        } catch (IloException e) {
            throw new RuntimeException(e);
        }
    }

    //Not optimal. If performances are needed, consider maintaining a map of activities by name.
    private IloIntervalVar getResourceAct(int op, int req, int res){
        if(res < 0 || res >= resourceActivities.length) return null;
        String name = "R[" + op + "," + req + "," + res + "]";
        for(IloIntervalVar act : resourceActivities[res]){
            if(act.getName().equals(name)) return act;
        }
        return null;
    }

    private void performSearch(){
        try {
            cp.startNewSearch();
            while(cp.next()){
                processSol();
            }
            cp.endSearch();
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }
    }

    //Search on makespan objective
    public void makespanSearch(){
        searchStart = System.nanoTime();
        int makespanBound = instance.makespanLB();
        int costBound = instance.costLB();
        try {
            IloObjective objective = cp.minimize(makespan);
            cp.add(objective);
            if(startSol) assignStartSol(lastSol);
            if(!silent) System.out.println("starting search on makespan objective");
            performSearch();
            int cpBound = (int) cp.getObjBound();
            if(cpBound > makespanBound) makespanBound = cpBound;
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }

        String logFile = outputPath + "logs/" + instance.name() + ".json";
        if(!silent) System.out.println("Writing search log to file: " + logFile);
        JsonWriter.writeLogToFile(new Log(instance.name(), makespanBound, costBound, log.toArray(new LogEntry[0])), logFile);

        if(lastSol != null){
            String solFile = outputPath + "solutions/" + instance.name() + ".json";
            System.out.println("Writing best solution to file: " + solFile);
            JsonWriter.writeSolutionToFile(lastSol, solFile);
        } else System.out.println("No solution found.");
    }

    //Search on cost objective
    public void costSearch(){
        searchStart = System.nanoTime();
        int makespanBound = instance.makespanLB();
        int costBound = instance.costLB();
        try {
            IloObjective objective = cp.minimize(cost);
            cp.add(objective);
            if(startSol) assignStartSol(lastSol);
            if(!silent) System.out.println("starting search on cost objective");
            performSearch();
            int cpBound = (int) cp.getObjBound();
            if(cpBound > costBound) costBound = cpBound;
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }

        String logFile = outputPath + "logs/" + instance.name() + ".json";
        if(!silent) System.out.println("Writing search log to file: " + logFile);
        JsonWriter.writeLogToFile(new Log(instance.name(), makespanBound, costBound, log.toArray(new LogEntry[0])), logFile);

        if(lastSol != null){
            String solFile = outputPath + "solutions/" + instance.name() + ".json";
            System.out.println("Writing best solution to file: " + solFile);
            JsonWriter.writeSolutionToFile(lastSol, solFile);
        } else System.out.println("No solution found.");
    }

    //Lexicographical search on makespan first:
    public void lexSearch(){
        searchStart = System.nanoTime();
        int makespanBound = instance.makespanLB();
        int costBound = instance.costLB();
        try {
            //Makespan:
            IloObjective objective = cp.minimize(makespan);
            cp.add(objective);
            if(startSol) assignStartSol(lastSol);
            if(!silent) System.out.println("starting search on makespan objective");
            performSearch();
            int cpBound = (int) cp.getObjBound();
            if(cpBound > makespanBound) makespanBound = cpBound;
            double remainingTime = timeLimit - ((double) timeElapsed() / 1000000000) + secondTimeLimit;

            //Cost:
            if(remainingTime > 0 && currentSol != null && lastSol != null) {
                cp.remove(objective);
                cp.add(cp.le(makespan, lastSol.makespan()));
                cp.add(cp.minimize(cost));
                cp.setStartingPoint(currentSol);
                //Allocating remaining time to search:
                setTimeLimit(remainingTime);
                if(!silent) System.out.println("starting search on cost objective");
                performSearch();
                cpBound = (int) cp.getObjBound();
                if(cpBound > costBound) costBound = cpBound;
            }
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }

        String logFile = outputPath + "logs/" + instance.name() + ".json";
        if(!silent) System.out.println("Writing search log to file: " + logFile);
        JsonWriter.writeLogToFile(new Log(instance.name(), makespanBound, costBound, log.toArray(new LogEntry[0])), logFile);

        if(lastSol != null){
            String solFile = outputPath + "solutions/" + instance.name() + ".json";
            System.out.println("Writing best solution to file: " + solFile);
            JsonWriter.writeSolutionToFile(lastSol, solFile);
        } else System.out.println("No solution found.");
    }

    //Lexicographical search on cost first:
    public void invertedLexSearch() {
        searchStart = System.nanoTime();
        int makespanBound = instance.makespanLB();
        int costBound = instance.costLB();
        try {
            //Cost:
            IloObjective objective = cp.minimize(cost);
            cp.add(objective);
            if(startSol) assignStartSol(lastSol);
            if(!silent) System.out.println("starting search on cost objective");
            performSearch();
            int cpBound = (int) cp.getObjBound();
            if(cpBound > costBound) costBound = cpBound;
            double remainingTime = timeLimit - ((double) timeElapsed() / 1000000000) + secondTimeLimit;

            //Makespan:
            if(remainingTime > 0 && currentSol != null && lastSol != null) {
                cp.remove(objective);
                cp.add(cp.le(cost, lastSol.cost()));
                System.out.println(" added cst: cost <= " + lastSol.cost());
                cp.add(cp.minimize(makespan));
                cp.setStartingPoint(currentSol);
                //Allocating remaining time to search:
                setTimeLimit(remainingTime);
                if(!silent) System.out.println("starting search on makespan objective");
                performSearch();
                cpBound = (int) cp.getObjBound();
                if(cpBound > makespanBound) makespanBound = cpBound;
            }
        } catch (IloException e) {
            close();
            throw new RuntimeException(e);
        }

        String logFile = outputPath + "logs/" + instance.name() + ".json";
        if(!silent) System.out.println("Writing search log to file: " + logFile);
        JsonWriter.writeLogToFile(new Log(instance.name(), makespanBound, costBound, log.toArray(new LogEntry[0])), logFile);

        if(lastSol != null){
            String solFile = outputPath + "solutions/" + instance.name() + ".json";
            System.out.println("Writing best solution to file: " + solFile);
            JsonWriter.writeSolutionToFile(lastSol, solFile);
        } else System.out.println("No solution found.");
    }

    private long timeElapsed(){
        return System.nanoTime() - searchStart;
    }

    public double getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(double timeLimit) {
        this.timeLimit = timeLimit;
        if(cp != null) {
            try {
                cp.setParameter(IloCP.DoubleParam.TimeLimit, timeLimit);
            } catch (IloException e) {
                close();
                throw new RuntimeException(e);
            }
        }
    }

    public double getSecondTimeLimit() {
        return secondTimeLimit;
    }

    public void setSecondTimeLimit(double timeLimit) {
        secondTimeLimit = timeLimit;
    }

    public int getFailLimit() {
        return failLimit;
    }

    public void setFailLimit(int failLimit) {
        this.failLimit = failLimit;
        if(cp != null) {
            try {
                cp.setParameter(IloCP.IntParam.FailLimit, failLimit);
            } catch (IloException e) {
                close();
                throw new RuntimeException(e);
            }
        }
    }

    public int getWorkers() {
        return workers;
    }

    public void setWorkers(int workers) {
        this.workers = workers;
        if(cp != null) {
            try {
                cp.setParameter(IloCP.IntParam.Workers, workers);
            } catch (IloException e) {
                close();
                throw new RuntimeException(e);
            }
        }
    }

    public IloCP.ParameterValues getSearchType() {
        return searchType;
    }

    public void setSearchType(IloCP.ParameterValues searchType) {
        this.searchType = searchType;
        if(cp != null) {
            try {
                cp.setParameter(IloCP.IntParam.SearchType, searchType);
            } catch (IloException e) {
                close();
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isSilent() {
        return silent;
    }

    public void setSilent(boolean silent) {
        this.silent = silent;
        if(cp != null) {
            if(silent) cp.setOut(null);
            else cp.setOut(System.out);
        }
    }

    public void setOnSolution(Consumer<Solution> onSolution) {
        this.onSolution = onSolution;
    }

    public boolean isStartSol() {
        return startSol;
    }

    public void setStartSol(boolean startSol) {
        this.startSol = startSol;
    }

    public void setSolution(Solution solution) {
        this.lastSol = solution;
    }

    public Solution getSolution() {
        return lastSol;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String path) {
        this.outputPath = path;
    }

    public boolean isFailureDirected() {
        return FailureDirected;
    }

    public void setFailureDirected(boolean failureDirected) {
        FailureDirected = failureDirected;
        if(failureDirected && cp != null){
            try {
                cp.setParameter(IloCP.DoubleParam.FailureDirectedSearchEmphasis, workers); //Setting all workers to FD search
                cp.setParameter(IloCP.IntParam.FailureDirectedSearchMaxMemory, 314572800); //Augmenting FD search memory
            } catch (IloException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
