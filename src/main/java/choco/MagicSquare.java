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

package choco;


import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.Smallest;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelectorWithTies;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;


public class MagicSquare {
    public static void main(String[] args) {

        int n = 5;
        int M = n * (n * n + 1) / 2;
        Model model = new Model(n + "-magic square problem");
        IntVar[][] x = new IntVar[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                x[i][j] = model.intVar("x_" + i + "," + j, 1, n * n, false);
            }
        }


        IntVar[] xFlat = new IntVar[x.length * x.length];
        for (int i = 0; i < x.length; i++) {
            System.arraycopy(x[i], 0, xFlat, i * x.length, x.length);
        }


        // AllDifferent
        for (int i = 0; i < xFlat.length; i++) {
            for (int j = i + 1; j < xFlat.length; j++) {
                model.arithm(xFlat[i], "!=", xFlat[j]).post();
            }
        }

        // Sum on lines
        for (int i = 0; i < n; i++) {
            model.sum(x[i], "=", M).post();
        }

        // Sum on columns
        for (int j = 0; j < x.length; j++) {
            IntVar[] column = new IntVar[n];
            for (int i = 0; i < x.length; i++)
                column[i] = x[i][j];
            model.sum(column, "=", M).post();
        }

        // Sum on diagonals
        IntVar[] diagonalLeft = new IntVar[n];
        IntVar[] diagonalRight = new IntVar[n];
        for (int i = 0; i < x.length; i++) {
            diagonalLeft[i] = x[i][i];
            diagonalRight[i] = x[n - i - 1][i];
        }
        model.sum(diagonalLeft, "=", M).post();
        model.sum(diagonalRight, "=", M).post();

        // Symetries breaking
        //model.arithm(x[0][0],"=",1).post;
        model.arithm(x[0][n - 1], "<", x[n - 1][0]).post();
        model.arithm(x[0][0], "<", x[n - 1][n - 1]).post();
        model.arithm(x[0][0], "<", x[n - 1][0]).post();


        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(
                new VariableSelectorWithTies<>(
                        new FirstFail(model),
                        new Smallest()),
                new IntDomainMin(),
                ArrayUtils.append(xFlat))
        );

        solver.showShortStatistics();
        System.out.println("let's go");
        int nSol = 0;
        while (nSol < 10000 && solver.solve()) {
            nSol++;
        }

        System.out.println("solution");
        ;

    }
}
