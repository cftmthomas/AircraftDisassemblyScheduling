package org.adsp.cpoptimizer;

import ilog.concert.IloIntervalVar;

import java.util.ArrayList;

public class IntervalVarList extends ArrayList<IloIntervalVar> {
    public IloIntervalVar[] toArray() {
        return this.toArray(new IloIntervalVar[0]);
    }
}
