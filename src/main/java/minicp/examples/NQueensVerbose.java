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

import minicp.cp.Factory;
import minicp.engine.constraints.AllDifferentAC;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.Procedure;

import java.util.Arrays;

import static minicp.cp.Factory.*;

public class NQueensVerbose {
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
        cp.post(new AllDifferentAC( Factory.makeIntVarArray(n, i -> minus(q[i],i))));
        cp.post(new AllDifferentAC( Factory.makeIntVarArray(n, i -> plus(q[i],i))));


        DFSearch dfs = makeDfs(cp, () -> {
                    int i = -1; // index of smallest domain size variable
                    int best = Integer.MAX_VALUE;
                    for (int k = 0; k < q.length; k++) {
                        if (q[k].getSize() > 1 && q[k].getSize() < best) {
                            i = k;
                            best = q[k].getSize();
                        }
                    }
                    if (i == -1) {
                        return new Procedure[0];
                    } else {
                        IntVar qi = q[i];
                        int v = qi.getMin();
                        Procedure left = () -> equal(qi, v);
                        Procedure right = () -> notEqual(qi, v);
                        return new Procedure[]{left,right};
                    }
                }
        );

        dfs.onSolution(() ->
                System.out.println("solution:"+ Arrays.toString(q))
        );

        SearchStatistics stats = dfs.solve(statistics -> statistics.nSolutions == 1000);

        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}