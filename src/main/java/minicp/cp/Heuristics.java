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

package minicp.cp;


import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.Choice;
import minicp.search.ChoiceCombinator;

import static minicp.search.Selector.branch;
import static minicp.search.Selector.selectMin;
import static minicp.cp.Factory.*;

public class Heuristics {

    public static Choice firstFail(IntVar... x) {
        Solver cp = x[0].getSolver();
        return selectMin(x,
                xi -> xi.getSize() > 1,
                xi -> xi.getSize(),
                xi -> {
                    int v = xi.getMin();
                    return branch(
                            () -> {
                                equal(xi,v);
                            },
                            () -> {
                                notEqual(xi,v);
                            }
                    );
                }
        );
    }


    public static Choice and(Choice ... choices) {
        return new ChoiceCombinator(choices);
    }

}
