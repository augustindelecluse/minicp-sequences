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
 
 
import minicp.cp.Factory;
import minicp.engine.constraints.LessOrEqual;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
 
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
 
public class MS2 {
 
    // https://en.wikipedia.org/wiki/Magic_square
    public static void main(String[] args) {
 
 
        int n = 5;
        int M = n * (n * n + 1) / 2;
 
        Solver cp = Factory.makeSolver();
        IntVar[][] x = new IntVar[n][n];
 
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                x[i][j] = makeIntVar(cp, 1, n * n);
            }
        }
 
 
        IntVar[] xFlat = new IntVar[x.length * x.length];
        for (int i = 0; i < x.length; i++) {
            System.arraycopy(x[i], 0, xFlat, i * x.length, x.length);
        }
 
 
        // AllDifferent
        cp.post(allDifferent(xFlat));
 
        // Sum on lines
        for (int i = 0; i < n; i++) {
            cp.post(sum(x[i], M));
        }
 
        // Sum on columns
        for (int j = 0; j < x.length; j++) {
            IntVar[] column = new IntVar[n];
            for (int i = 0; i < x.length; i++)
                column[i] = x[i][j];
            cp.post(sum(column, M));
        }
 
        // Sum on diagonals
        IntVar[] diagonalLeft = new IntVar[n];
        IntVar[] diagonalRight = new IntVar[n];
        for (int i = 0; i < x.length; i++) {
            diagonalLeft[i] = x[i][i];
            diagonalRight[i] = x[n - i - 1][i];
        }
        cp.post(sum(diagonalLeft, M));
        cp.post(sum(diagonalRight, M));
 
        cp.post(new LessOrEqual(x[0][n - 1], minus(x[n - 1][0], 1)));
        cp.post(new LessOrEqual(x[0][0], minus(x[n - 1][n - 1], 1)));
        cp.post(new LessOrEqual(x[0][0], minus(x[n - 1][0], 1)));
 
 
        DFSearch dfs = makeDfs(cp, firstFail(xFlat));
 
        long t0 = System.currentTimeMillis();
        SearchStatistics stats = dfs.solve(stat -> stat.numberOfSolutions() >= 10000); // stop on first solution
        System.out.println(stats);
        System.out.println("time:" + (System.currentTimeMillis() - t0));
    }
 
}
