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

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.InconsistencyException;

public class AllDifferentAC extends Constraint {

    private IntVar [] x;

    public AllDifferentAC(IntVar ... x) {
        super(x[0].getSolver());
        this.x = x;
    }

    @Override
    public void post() throws InconsistencyException {
        for (int i = 0; i < x.length; i++) {
            x[i].propagateOnDomainChange(this);
        }
    }

    @Override
    public void propagate() throws InconsistencyException {
        super.propagate();
    }
}
