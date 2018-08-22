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

import minicp.engine.core.IntVar;
import minicp.search.Objective;
import minicp.util.InconsistencyException;

public class Minimize implements Objective {
    public int bound = Integer.MAX_VALUE;
    private final IntVar x;

    public Minimize(IntVar x) {
        this.x = x;
        x.getSolver().onFixPoint(() -> x.removeAbove(bound));
    }

    public void tighten() {
        if (!x.isBound()) throw new RuntimeException("objective not bound");
        this.bound = x.getMax() - 1;
        throw InconsistencyException.INCONSISTENCY;
    }
}
