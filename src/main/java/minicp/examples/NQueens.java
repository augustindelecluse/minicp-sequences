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

package minicp.examples;

import minicp.engine.constraints.AllDifferentAC;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.InconsistencyException;
import minicp.search.SearchStatistics;
import java.util.Arrays;

import static minicp.search.Selector.*;
import static minicp.cp.Factory.*;

public class NQueens {
    public static void main(String[] args) throws InconsistencyException {
        int n = 8;
        Solver cp = makeSolver();
        IntVar[] q = makeIntVarArray(cp, n, n);


        for(int i=0;i < n;i++)
            for(int j=i+1;j < n;j++) {
                cp.post(notEqual(q[i],q[j]));
                cp.post(notEqual(q[i],q[j],j-i));
                cp.post(notEqual(q[i],q[j],i-j));
            }

        cp.post(new AllDifferentAC(q));
        cp.post(new AllDifferentAC( makeIntVarArray(cp,n, i -> minus(q[i],i))));
        cp.post(new AllDifferentAC( makeIntVarArray(cp,n, i -> plus(q[i],i))));


        SearchStatistics stats = makeDfs(cp,
                selectMin(q,
                        qi -> qi.getSize() > 1,
                        qi -> qi.getSize(),
                        qi -> {
                            int v = qi.getMin();
                            return branch(() -> equal(qi,v),
                                          () -> notEqual(qi,v));
                        }
                )
        ).onSolution(() ->
                System.out.println("solution:"+ Arrays.toString(q))
        ).start(statistics -> statistics.nSolutions == 1000);

        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}