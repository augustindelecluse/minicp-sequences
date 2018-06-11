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
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;

import java.util.Arrays;

import static minicp.cp.Factory.*;
import static minicp.cp.Factory.notEqual;
import static minicp.search.Selector.branch;
import static minicp.search.Selector.selectMin;
import minicp.util.Box;

/**
 * Created by ldm on 2/12/17.
 */
public class NQueensPhase {
    public static void main(String[] args) throws InconsistencyException {

        int n = 8;
        Solver cp = makeSolver();

        IntVar[] q = makeIntVarArray(cp, n, n);
        IntVar[] ql = makeIntVarArray(cp,n,i -> plus(q[i],i));
        IntVar[] qr = makeIntVarArray(cp,n,i -> plus(q[i],-i));
        IntVar[]  q1 = makeIntVarArray(cp,n/2,i -> q[i]);
        IntVar[]  q2 = makeIntVarArray(cp,n/2,i -> q[i+4]);

        cp.post(allDifferent(q));
        cp.post(allDifferent(ql));
        cp.post(allDifferent(qr));
        Box<Integer> nbSol = new Box<>(0);
        SearchStatistics stats = makeDfs(cp,
                selectMin(q1,
                        qi -> qi.getSize() > 1,
                        qi -> qi.getSize(),
                        qi -> {
                            int v = qi.getMin();
                            return branch(() -> equal(qi,v),
                                          () -> notEqual(qi,v)
                                    );
                        }
                )
        ).onSolution(() ->
                makeDfs(cp,selectMin(q2,
                        qi -> qi.getSize() > 1,
                        qi -> (float)qi.getSize(),
                        qi -> {
                            int v = qi.getMin();
                            return branch(() -> equal(qi,v),
                                          () -> notEqual(qi,v)
                                    );
                        })
                ).onSolution(() -> {
                    nbSol.set(nbSol.get() + 1);
                    System.out.println("solution:"+ Arrays.toString(q));
                }).solve()
        ).solve();

        System.out.format("#Solutions: %s\n", nbSol);
    }
}

