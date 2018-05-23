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

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import org.junit.Test;

import static minicp.cp.Factory.makeIntVar;
import static minicp.cp.Factory.makeSolver;
import static minicp.cp.Heuristics.firstFail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class Element1DTest {

    @Test
    public void element1dTest1() {

        try {

            Solver cp = makeSolver();
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, 2, 40);

            int[] T = new int[]{9, 8, 7, 5, 6};

            cp.post(new Element1D(T, y, z));

            assertEquals(0, y.getMin());
            assertEquals(4, y.getMax());


            assertEquals(5, z.getMin());
            assertEquals(9, z.getMax());

            z.removeAbove(7);
            cp.fixPoint();

            assertEquals(2, y.getMin());


            y.remove(3);
            cp.fixPoint();

            assertEquals(7, z.getMax());
            assertEquals(6, z.getMin());


        } catch (InconsistencyException e) {
            fail("should not fail");
        }
    }

    @Test
    public void element1dTest2() {

        try {

            Solver cp = new Solver();
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -20, 40);

            int[] T = new int[]{9, 8, 7, 5, 6};

            cp.post(new Element1D(T, y, z));

            DFSearch dfs = new DFSearch(cp, firstFail(y, z));
            dfs.onSolution(() ->
                    assertEquals(T[y.getMin()], z.getMin())
            );
            SearchStatistics stats = dfs.solve();

            assertEquals(5, stats.nSolutions);


        } catch (InconsistencyException e) {
            fail("should not fail");
        }
    }

}
