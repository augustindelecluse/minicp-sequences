/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2017. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;

public class Minimize extends AbstractConstraint {

    public int bound = Integer.MAX_VALUE;
    private final IntVar x;
    private final DFSearch dfs;

    public Minimize(IntVar x, DFSearch dfs) {
        super(x.getSolver());
        this.x = x;
        this.dfs = dfs;
    }

    protected void tighten() {
        if (!x.isBound()) throw new RuntimeException("objective not bound");
        this.bound = x.getMax() - 1;
    }


    @Override
    public void post()  {
        x.propagateOnBoundChange(this);
        // Ensure that the constraint is scheduled on backtrack
        dfs.onSolution(() -> {
            tighten();
            cp.schedule(this);
        });
        dfs.onFail(() -> cp.schedule(this));
    }

    @Override
    public void propagate()  {
        x.removeAbove(bound);
    }
}
