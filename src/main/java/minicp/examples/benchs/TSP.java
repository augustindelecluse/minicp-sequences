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

import java.util.Random;

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;

public class TSP {
    public static void main(String[] args) {


        //https://people.sc.fsu.edu/~jburkardt/datasets/tsp/p01_d.txt

        // instance gr17 https://people.sc.fsu.edu/~jburkardt/datasets/tsp/gr17_d.txt
        int[][] distanceMatrix = new int[][]{{0, 633, 257, 91, 412, 150, 80, 134, 259, 505, 353, 324, 70, 211, 268, 246, 121},
                {633, 0, 390, 661, 227, 488, 572, 530, 555, 289, 282, 638, 567, 466, 420, 745, 518},
                {257, 390, 0, 228, 169, 112, 196, 154, 372, 262, 110, 437, 191, 74, 53, 472, 142},
                {91, 661, 228, 0, 383, 120, 77, 105, 175, 476, 324, 240, 27, 182, 239, 237, 84},
                {412, 227, 169, 383, 0, 267, 351, 309, 338, 196, 61, 421, 346, 243, 199, 528, 297},
                {150, 488, 112, 120, 267, 0, 63, 34, 264, 360, 208, 329, 83, 105, 123, 364, 35},
                {80, 572, 196, 77, 351, 63, 0, 29, 232, 444, 292, 297, 47, 150, 207, 332, 29},
                {134, 530, 154, 105, 309, 34, 29, 0, 249, 402, 250, 314, 68, 108, 165, 349, 36},
                {259, 555, 372, 175, 338, 264, 232, 249, 0, 495, 352, 95, 189, 326, 383, 202, 236},
                {505, 289, 262, 476, 196, 360, 444, 402, 495, 0, 154, 578, 439, 336, 240, 685, 390},
                {353, 282, 110, 324, 61, 208, 292, 250, 352, 154, 0, 435, 287, 184, 140, 542, 238},
                {324, 638, 437, 240, 421, 329, 297, 314, 95, 578, 435, 0, 254, 391, 448, 157, 301},
                {70, 567, 191, 27, 346, 83, 47, 68, 189, 439, 287, 254, 0, 145, 202, 289, 55},
                {211, 466, 74, 182, 243, 105, 150, 108, 326, 336, 184, 391, 145, 0, 57, 426, 96},
                {268, 420, 53, 239, 199, 123, 207, 165, 383, 240, 140, 448, 202, 57, 0, 483, 153},
                {246, 745, 472, 237, 528, 364, 332, 349, 202, 685, 542, 157, 289, 426, 483, 0, 336},
                {121, 518, 142, 84, 297, 35, 29, 36, 236, 390, 238, 301, 55, 96, 153, 336, 0}};



        int n = distanceMatrix.length;

        Solver cp = makeSolver();
        IntVar[] succ = makeIntVarArray(cp, n, n);
        IntVar[] distSucc = makeIntVarArray(cp, n, 745);

        cp.post(new Circuit(succ));

        for (int i = 0; i < n; i++) {
            cp.post(new Element1D(distanceMatrix[i], succ[i], distSucc[i]));
        }

        IntVar totalDist = sum(distSucc);

        Objective obj = cp.minimize(totalDist);


        DFSearch dfs = makeDfs(cp,firstFail(succ));


        dfs.onSolution(() ->
                System.out.println(totalDist)
        );

        long t0 = System.currentTimeMillis();
        SearchStatistics stats = dfs.optimize(obj);

        System.out.println("time:"+(System.currentTimeMillis()-t0));

        System.out.println(stats);


    }
}
