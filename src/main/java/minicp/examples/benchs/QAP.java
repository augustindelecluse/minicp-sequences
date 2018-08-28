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

package minicp.examples.benchs;

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;

public class QAP {

    public static void main(String[] args) {

        // ---- read the instance -----


        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int[][] w = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
            }
        }

        // ----- build the model ---

        Solver cp = makeSolver();
        IntVar[] x = makeIntVarArray(cp, n, n);

        cp.post(allDifferent(x));


        // build the objective function
        IntVar[] weightedDist = new IntVar[n * n];
        for (int k = 0, i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                weightedDist[k] = mul(element(d, x[i], x[j]), w[i][j]);
                k++;
            }
        }
        IntVar totCost = sum(weightedDist);
        Objective obj = cp.minimize(totCost);

        DFSearch dfs = makeDfs(cp, firstFail(x));

        dfs.onSolution(() -> System.out.println("objective:" + totCost.min()));


        long t0 = System.currentTimeMillis();

        SearchStatistics stats = dfs.optimize(obj);

        long t1 = System.currentTimeMillis();

        System.out.println(stats);

        System.out.println("t=" + (t1 - t0));


    }
}
