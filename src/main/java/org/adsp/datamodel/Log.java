package org.adsp.datamodel;

public record Log(String instance, int makespanBound, int costBound, LogEntry[] log) {

    public int bestMakespan() {
        int bestObj = Integer.MAX_VALUE;
        for(LogEntry entry: log){
            if(entry.makespan() < bestObj) bestObj = entry.makespan();
        }
        return bestObj;
    }

    public int bestCost() {
        int bestObj = Integer.MAX_VALUE;
        for(LogEntry entry: log){
            if(entry.cost() < bestObj) bestObj = entry.cost();
        }
        return bestObj;
    }

    public double timeToBestMakespan() {
        int bestObj = Integer.MAX_VALUE;
        double bestT = Integer.MAX_VALUE;
        for(LogEntry entry: log){
            if(entry.makespan() < bestObj || (entry.makespan() == bestObj && entry.time() < bestT)){
                bestObj = entry.makespan();
                bestT = entry.time();
            }
        }
        return bestT;
    }

    public double timeToBestCost() {
        int bestObj = Integer.MAX_VALUE;
        double bestT = Integer.MAX_VALUE;
        for(LogEntry entry: log){
            if(entry.cost() < bestObj || (entry.cost() == bestObj && entry.time() < bestT)){
                bestObj = entry.cost();
                bestT = entry.time();
            }
        }
        return bestT;
    }

    public boolean isMakespanOpti(){
        return bestMakespan() == makespanBound;
    }

    public boolean isCostOpti(){
        return bestCost() == costBound;
    }

    public int firstMakespan(){
        return log.length > 0 ? log[0].makespan() : Integer.MAX_VALUE;
    }

    public int firstCost(){
        return log.length > 0 ? log[0].cost() : Integer.MAX_VALUE;
    }

    public int lastMakespan(){
        return log.length > 0 ? log[log.length-1].makespan() : Integer.MAX_VALUE;
    }

    public int lastCost(){
        return log.length > 0 ? log[log.length-1].cost() : Integer.MAX_VALUE;
    }

    public double timeToFirstSol() {
        return log.length > 0 ? log[0].time() : Integer.MAX_VALUE;
    }

    public double timeToLastSol() {
        return log.length > 0 ? log[log.length-1].time() : Integer.MAX_VALUE;
    }

    public static double gap(double obj, double bound){
        return (obj - bound) / bound;
    }

    public boolean isLex(){
        int lastMakespan = Integer.MAX_VALUE;
        int lastCost = Integer.MAX_VALUE;
        for(LogEntry entry: log()){
            if(lastMakespan == entry.makespan() && lastCost == entry.cost()) {
                return true;
            } else{
                lastMakespan = entry.makespan();
                lastCost = entry.cost();
            }
        }
        return false;
    }

    public double secondSearchStart(){
        int lastMakespan = Integer.MAX_VALUE;
        int lastCost = Integer.MAX_VALUE;
        for(LogEntry entry: log()){
            if(lastMakespan == entry.makespan() && lastCost == entry.cost()) {
                return entry.time();
            } else{
                lastMakespan = entry.makespan();
                lastCost = entry.cost();
            }
        }
        return Double.MAX_VALUE;
    }

    public int bestMakespanFrom(double time) {
        int bestObj = Integer.MAX_VALUE;
        for(LogEntry entry: log) if(entry.time() >= time){
            if(entry.makespan() < bestObj) bestObj = entry.makespan();
        }
        return bestObj;
    }

    public int bestCostFrom(double time) {
        int bestObj = Integer.MAX_VALUE;
        for(LogEntry entry: log) if(entry.time() >= time){
            if(entry.cost() < bestObj) bestObj = entry.cost();
        }
        return bestObj;
    }

    public double timeToBestMakespanFrom(double time) {
        int bestObj = Integer.MAX_VALUE;
        double bestT = Integer.MAX_VALUE;
        for(LogEntry entry: log) if(entry.time() >= time){
            if(entry.makespan() < bestObj || (entry.makespan() == bestObj && entry.time() < bestT)){
                bestObj = entry.makespan();
                bestT = entry.time();
            }
        }
        return bestT;
    }

    public double timeToBestCostFrom(double time) {
        int bestObj = Integer.MAX_VALUE;
        double bestT = Integer.MAX_VALUE;
        for(LogEntry entry: log) if(entry.time() >= time){
            if(entry.cost() < bestObj || (entry.cost() == bestObj && entry.time() < bestT)){
                bestObj = entry.cost();
                bestT = entry.time();
            }
        }
        return bestT;
    }

    public int firstMakespanFrom(double time){
        for(LogEntry entry: log) if(entry.time() >= time) return entry.makespan();
        return Integer.MAX_VALUE;
    }

    public int firstCostFrom(double time){
        for(LogEntry entry: log) if(entry.time() >= time) return entry.cost();
        return Integer.MAX_VALUE;
    }
}
