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
import minicp.util.InconsistencyException;
import minicp.util.InputReader;

import java.util.Random;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;

public class QAPLNS {

    public static void main(String[] args) throws InconsistencyException {
        
        // ---- read the instance -----

        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int [][] w = new int[n][n];
        for (int i = 0; i < n ; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int [][] d = new int[n][n];
        for (int i = 0; i < n ; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
            }
        }

        // ----- build the model ---

        Solver cp = makeSolver();
        IntVar[] x = makeIntVarArray(cp, n, n);

        cp.post(allDifferent(x));

        DFSearch dfs = makeDfs(cp,firstFail(x));


        // build the objective function
        IntVar[] weightedDist = new IntVar[n*n];
        int ind = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                weightedDist[ind] = mul(element(d,x[i],x[j]),w[i][j]);
                ind++;
            }
        }
        IntVar objective = sum(weightedDist);
        cp.post(minimize(objective,dfs));


        // --- Large Neighborhood Search ---

        // Current best solution
        int[] xBest = new int[n];
        for (int i = 0; i < n; i++) {
            xBest[i] = i;
        }

        dfs.onSolution(() -> {
            // Update the current best solution
            for (int i = 0; i < n; i++) {
                xBest[i] = x[i].getMin();
            }
            System.out.println("objective:"+objective.getMin());
        });


        int nRestarts = 1000;
        int failureLimit = 50;
        Random rand = new java.util.Random(0);

        for (int i = 0; i < nRestarts; i++) {
            System.out.println("restart number #"+i);

            // Record the state such that the fragment constraints can be cancelled
            cp.push();

            // Assign the fragment 5% of the variables randomly chosen
            for (int j = 0; j < n; j++) {
                if (rand.nextInt(100) < 5) {
                    equal(x[j],xBest[j]);
                }
            }
            dfs.start(statistics -> statistics.nFailures >= failureLimit);

            // cancel all the fragment constraints
            cp.pop();
        }

    }
}
