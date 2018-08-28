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

import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1D;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.io.InputReader;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;

public class TSP {
    public static void main(String[] args) {

        // instance gr17 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/gr17_d.txt
        InputReader reader = new InputReader("data/tsp.txt");

        int n = reader.getInt();

        int[][] distanceMatrix = reader.getMatrix(n,n);

        Solver cp = makeSolver();
        IntVar[] succ = makeIntVarArray(cp, n, n);
        IntVar[] distSucc = makeIntVarArray(cp, n, 745);

        cp.post(new Circuit(succ));

        for (int i = 0; i < n; i++) {
            cp.post(new Element1D(distanceMatrix[i], succ[i], distSucc[i]));
        }

        IntVar totalDist = sum(distSucc);

        Objective obj = cp.minimize(totalDist);


        DFSearch dfs = makeDfs(cp, firstFail(succ));


        dfs.onSolution(() ->
                System.out.println(totalDist)
        );

        long t0 = System.currentTimeMillis();
        SearchStatistics stats = dfs.optimize(obj);

        System.out.println("time:" + (System.currentTimeMillis() - t0));

        System.out.println(stats);


    }
}
