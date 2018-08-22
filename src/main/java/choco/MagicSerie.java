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
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;


public class MagicSerie {
    public static void main(String[] args) {

        int n = 300;
        Model model = new Model(n + "-magic series problem");
        IntVar[] s = new IntVar[n];
        for (int i = 0; i < n; i++) {
            s[i] = model.intVar("s_" + i, 0, n - 1, false);
        }

        model.sum(s, "=", n).post();


        for (int i = 0; i < n; i++) {
            final int fi = i;
            BoolVar[] b = new BoolVar[n];
            for (int k = 0; k < n; k++) {
                b[k] = model.arithm(s[k], "=", i).reify();
            }
            model.sum(b, "=", s[i]).post();
        }

        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(
                new VariableSelectorWithTies<>(
                        new FirstFail(model),
                        new Smallest()),
                new IntDomainMin(),
                ArrayUtils.append(s))
        );

        solver.showShortStatistics();
        while (solver.solve()) {
            System.out.println(Arrays.toString(s));
            System.out.println("solution");
            ;
        }
    }
}
