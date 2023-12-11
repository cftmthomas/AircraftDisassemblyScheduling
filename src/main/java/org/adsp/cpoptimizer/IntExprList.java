package org.adsp.cpoptimizer;

import ilog.concert.IloIntExpr;

import java.util.ArrayList;

public class IntExprList extends ArrayList<IloIntExpr> {
    public IloIntExpr[] toArray() {
        return this.toArray(new IloIntExpr[0]);
    }
}
