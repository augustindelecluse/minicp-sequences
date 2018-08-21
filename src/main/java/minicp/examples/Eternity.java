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

import minicp.cp.Factory;
import minicp.engine.constraints.TableDecomp;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InputReader;

import java.util.Arrays;

import static minicp.cp.BranchingScheme.and;
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;


public class Eternity {

    public static IntVar[] flatten(IntVar[][] x) {
        return Arrays.stream(x).flatMap(Arrays::stream).toArray(IntVar[]::new);
    }

    public static void main(String[] args) {

        // Reading the data

        InputReader reader = new InputReader("data/eternity/eternity7x7.txt");

        int n = reader.getInt();
        int m = reader.getInt();

        int[][] pieces = new int[n * m][4];
        int max_ = 0;

        for (int i = 0; i < n * m; i++) {
            for (int j = 0; j < 4; j++) {
                pieces[i][j] = reader.getInt();
                if (pieces[i][j] > max_)
                    max_ = pieces[i][j];
            }
            System.out.println(Arrays.toString(pieces[i]));
        }
        final int max = max_;

        // ------------------------

        // Table with makeIntVarArray pieces and for each their 4 possible rotations

        int[][] table = new int[4 * n * m][5];

        for (int i = 0; i < pieces.length; i++) {
            for (int r = 0; r < 4; r++) {
                table[i * 4 + r][0] = i;
                table[i * 4 + r][1] = pieces[i][(r + 0) % 4];
                table[i * 4 + r][2] = pieces[i][(r + 1) % 4];
                table[i * 4 + r][3] = pieces[i][(r + 2) % 4];
                table[i * 4 + r][4] = pieces[i][(r + 3) % 4];
            }
        }


        Solver cp = makeSolver();


        IntVar[][] id = new IntVar[n][m]; // id
        IntVar[][] u = new IntVar[n][m];  // up
        IntVar[][] r = new IntVar[n][m];  // right
        IntVar[][] d = new IntVar[n][m];  // down
        IntVar[][] l = new IntVar[n][m];  // left

        for (int i = 0; i < n; i++) {
            u[i] = Factory.makeIntVarArray(m, j -> makeIntVar(cp, 0, max));
            id[i] = makeIntVarArray(cp, m, n * m);
        }
        for (int k = 0; k < n; k++) {
            final int i = k;
            if (i < n - 1) d[i] = u[i + 1];
            else d[i] = Factory.makeIntVarArray(m, j -> makeIntVar(cp, 0, max));
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                l[i][j] = makeIntVar(cp, 0, max);
            }
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                if (j < m - 1) r[i][j] = l[i][j + 1];
                else r[i][j] = makeIntVar(cp, 0, max);
            }
        }

        // The constraints of the problem

        // makeIntVarArray the pieces placed are different
        cp.post(allDifferent(flatten(id)));

        // makeIntVarArray the pieces placed are valid one (one of the given mxn piece possibly rotated)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                //cp.post(new TableCT(new IntVar[]{id[i][j],u[i][j],r[i][j],d[i][j],l[i][j]},table));
                cp.post(new TableDecomp(new IntVar[]{id[i][j], u[i][j], r[i][j], d[i][j], l[i][j]}, table));
            }
        }

        // 0 on the border
        for (int i = 0; i < n; i++) {
            equal(l[i][0], 0);
            equal(r[i][m - 1], 0);
        }
        for (int j = 0; j < m; j++) {
            equal(u[0][j], 0);
            equal(d[n - 1][j], 0);
        }


        // The search using the and combinator

        DFSearch dfs = makeDfs(cp,
                and(firstFail(flatten(id)),
                        firstFail(flatten(u)),
                        firstFail(flatten(r)),
                        firstFail(flatten(d)),
                        firstFail(flatten(l)))
        );


        dfs.onSolution(() -> {
            // Pretty Print
            for (int i = 0; i < n; i++) {
                String line = "   ";
                for (int j = 0; j < m; j++) {
                    line += u[i][j].getMin() + "   ";
                }
                System.out.println(line);
                line = " ";
                for (int j = 0; j < m; j++) {
                    line += l[i][j].getMin() + "   ";
                }
                line += r[i][m - 1].getMin();
                System.out.println(line);
            }
            String line = "   ";
            for (int j = 0; j < m; j++) {
                line += d[n - 1][j].getMin() + "   ";
            }
            System.out.println(line);

        });


        SearchStatistics stats = dfs.solve(statistics -> statistics.nSolutions == 1);

        System.out.format("#Solutions: %s\n", stats.nSolutions);
        System.out.format("Statistics: %s\n", stats);

    }
}
