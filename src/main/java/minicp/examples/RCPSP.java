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
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.examples;

import minicp.engine.constraints.Cumulative;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.InputReader;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;

/**
 * Resource Constrained Project Scheduling Problem (RCPSP)
 * http://www.om-db.wi.tum.de/psplib/library.html
 */
public class RCPSP {


    public static void main(String[] args) {

        // Reading the data

        InputReader reader = new InputReader("data/rcpsp/j90_1_1.rcp");

        int nActivities = reader.getInt();
        int nResources = reader.getInt();

        int[] capa = new int[nResources];
        for (int i = 0; i < nResources; i++) {
            capa[i] = reader.getInt();
        }

        int[] duration = new int[nActivities];
        int[][] consumption = new int[nResources][nActivities];
        int[][] successors = new int[nActivities][];


        int H = 0;
        for (int i = 0; i < nActivities; i++) {
            // durations, demand for each resource, successors
            duration[i] = reader.getInt();
            H += duration[i];
            for (int r = 0; r < nResources; r++) {
                consumption[r][i] = reader.getInt();
            }
            successors[i] = new int[reader.getInt()];
            for (int k = 0; k < successors[i].length; k++) {
                successors[i][k] = reader.getInt() - 1;
            }
        }


        // -------------------------------------------

        // The Model

        Solver cp = makeSolver();

        IntVar[] start = makeIntVarArray(cp, nActivities, H);
        IntVar[] end = new IntVar[nActivities];


        for (int i = 0; i < nActivities; i++) {
            end[i] = plus(start[i], duration[i]);
        }

        for (int r = 0; r < nResources; r++) {
            cp.post(new Cumulative(start, duration, consumption[r], capa[r]));
        }

        for (int i = 0; i < nActivities; i++) {
            for (int k : successors[i]) {
                // activity i must precede activity k
                cp.post(lessOrEqual(end[i], start[k]));
            }
        }

        IntVar makespan = maximum(end);


        DFSearch dfs = makeDfs(cp, firstFail(start));


        cp.post(minimize(makespan, dfs));

        dfs.onSolution(() ->

                System.out.println("makespan:" + makespan)
        );

        SearchStatistics stats = dfs.solve();

        System.out.format("Statistics: %s\n", stats);


    }
}
