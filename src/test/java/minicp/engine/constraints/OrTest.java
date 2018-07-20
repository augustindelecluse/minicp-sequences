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

package minicp.engine.constraints;

import minicp.engine.core.BoolVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Test;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;
import static org.junit.Assert.*;


public class OrTest {

    @Test
    public void or1() {
        try {
            try {

                Solver cp = makeSolver();
                BoolVar [] x = new BoolVar[] {makeBoolVar(cp),makeBoolVar(cp),makeBoolVar(cp),makeBoolVar(cp)};
                cp.post(new Or(x));

                for (BoolVar xi : x) {
                    assertTrue(!xi.isBound());
                }

                equal(x[1],0);
                equal(x[2],0);
                equal(x[3],0);
                assertTrue(x[0].isTrue());

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            e.print();
        }

    }

    @Test
    public void or2() {
        try {
            try {

                Solver cp = makeSolver();
                BoolVar [] x = new BoolVar[] {makeBoolVar(cp),makeBoolVar(cp),makeBoolVar(cp),makeBoolVar(cp)};
                cp.post(new Or(x));


                DFSearch dfs = makeDfs(cp,firstFail(x));

                dfs.onSolution(() -> {
                            int nTrue = 0;
                            for (BoolVar xi: x) {
                                if (xi.isTrue()) nTrue++;
                            }
                            assertTrue(nTrue > 0);

                        }
                );

                SearchStatistics stats = dfs.solve();

                assertEquals(15,stats.nSolutions);

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            e.print();
        }

    }






}
