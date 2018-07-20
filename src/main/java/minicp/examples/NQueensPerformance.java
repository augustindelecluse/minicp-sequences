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

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.SearchStatistics;

import java.util.Arrays;

import static minicp.cp.Factory.*;
import static minicp.search.Selector.branch;
import static minicp.search.Selector.selectMin;

public class NQueensPerformance {
    public static void main(String[] args) {
        int n = 88;
        Solver cp = makeSolver();
        IntVar[] q = makeIntVarArray(cp, n, n);

/*
        for(int i=0;i < n;i++)
            for(int j=i+1;j < n;j++) {
                cp.post(notEqual(q[i],q[j]));
                cp.post(notEqual(q[i],q[j],j-i));
                cp.post(notEqual(q[i],q[j],i-j));
            }*/

        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
                cp.post(notEqual(q[i], q[j]));
                cp.post(notEqual(plus(q[i], j - i), q[j]));
                cp.post(notEqual(minus(q[i], j - i), q[j]));
            }

        cp.onSolution(() ->
                System.out.println("solution:"+ Arrays.toString(q))
        );

        long t0 = System.currentTimeMillis();
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
        ).solve(statistics -> {
           if ((statistics.nNodes/2) % 10000 == 0) {
               //System.out.println("failures:"+statistics.nFailures);
               System.out.println("nodes:"+(statistics.nNodes/2));
           }
           return statistics.nFailures > 500000 || statistics.nSolutions > 0;
        });

        System.out.println("time:"+(System.currentTimeMillis()-t0));
        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}
