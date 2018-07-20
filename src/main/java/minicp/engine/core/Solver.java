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

package minicp.engine.core;

import minicp.cp.Factory;
import minicp.engine.constraints.Minimize;
import minicp.search.SearchNode;
import minicp.util.Procedure;

public interface Solver extends SearchNode {
    void post(Constraint c);
    void schedule(Constraint c);
    void post(Constraint c, boolean enforceFixPoint);
    void fixPoint();

    public void onFixPoint(Procedure listener);

    default void minimize(IntVar x) { post(new Minimize(x)); }
    default void maximize(IntVar x) { minimize(Factory.minus(x)); }

    // ugly
    void post(BoolVar b);
    int registerVar(IntVar x);
}

