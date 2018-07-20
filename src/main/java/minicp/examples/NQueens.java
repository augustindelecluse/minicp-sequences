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
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import java.util.Arrays;

import static minicp.search.Selector.*;

public class NQueens {
    public static void main(String[] args) {
        int n = 8;
        Solver cp = Factory.makeSolver();
        IntVar[] q = Factory.makeIntVarArray(cp, n, n);


        for(int i=0;i < n;i++)
            for(int j=i+1;j < n;j++) {
                cp.post(Factory.notEqual(q[i],q[j]));
                cp.post(Factory.notEqual(q[i],q[j],j-i));
                cp.post(Factory.notEqual(q[i],q[j],i-j));
            }


        cp.post(Factory.allDifferentAC(q));
        cp.post(Factory.allDifferentAC(Factory.makeIntVarArray(n, i -> Factory.minus(q[i],i))));
        cp.post(Factory.allDifferentAC(Factory.makeIntVarArray(n, i -> Factory.plus(q[i],i))));


        DFSearch search = Factory.makeDfs(cp,
                selectMin(q,
                        qi -> qi.getSize() > 1,
                        qi -> qi.getSize(),
                        qi -> {
                            int v = qi.getMin();
                            return branch(() -> Factory.equal(qi, v),
                                          () -> Factory.notEqual(qi, v));
                        }
                ));

        search.onSolution(() ->
                System.out.println("solution:" + Arrays.toString(q))
        );
        SearchStatistics stats = search.solve(statistics -> statistics.nSolutions == 1000);

        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}
