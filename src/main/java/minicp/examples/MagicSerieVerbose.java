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
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.Procedure;

import java.util.Arrays;

import static minicp.cp.Factory.*;


public class MagicSerieVerbose {
    public static void main(String[] args) {

        int n = 8;
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[i] = n;

        Solver cp = makeSolver();

        IntVar[] s = makeIntVarArray(cp, n, n);

        for (int i : pos) {
            cp.post(sum(makeIntVarArray(n, j -> isEqual(s[j], i)), s[i]));
        }

        cp.post(sum(s, n));
        cp.post(sum(makeIntVarArray(n, i -> mul(s[i], i)), n));


        long t0 = System.currentTimeMillis();
        DFSearch dfs = makeDfs(cp, () -> {
            int idx = -1; // index of the first variable that is not bound
            for (int k : pos)
                if (s[k].getSize() > 1) {
                    idx = k;
                    break;
                }
            if (idx == -1)
                return new Procedure[0];
            else {
                IntVar si = s[idx];
                int v = si.getMin();
                Procedure left = () -> equal(si, v);
                Procedure right = () -> notEqual(si, v);
                return new Procedure[]{left, right};
            }
        });


        dfs.onSolution(() ->
                System.out.println("solution:" + Arrays.toString(s))
        );

        SearchStatistics stats = dfs.solve();


        long t1 = System.currentTimeMillis();

        System.out.println(t1 - t0);

        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}
