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

import minicp.engine.SolverTest;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.junit.Test;

import java.util.Arrays;

import static minicp.cp.BranchingScheme.and;
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class MaximumTest extends SolverTest {

    @Test
    public void maximumTest1() {

        try {

            Solver cp = solverFactory.get();
            IntVar[] x = makeIntVarArray(cp, 3, 10);
            IntVar y = makeIntVar(cp, -5, 20);
            cp.post(new Maximum(x, y));

            assertEquals(9, y.max());
            assertEquals(0, y.min());

            y.removeAbove(8);
            cp.fixPoint();

            assertEquals(8, x[0].max());
            assertEquals(8, x[1].max());
            assertEquals(8, x[2].max());

            y.removeBelow(5);
            x[0].removeAbove(2);
            x[1].removeBelow(6);
            x[2].removeBelow(6);
            cp.fixPoint();

            assertEquals(8, y.max());
            assertEquals(6, y.min());

            y.removeBelow(7);
            x[1].removeAbove(6);
            cp.fixPoint();
            // x0 = 0..2
            // x1 = 6
            // x2 = 6..8
            // y = 7..8
            assertEquals(7, x[2].min());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void maximumTest2() {

        try {

            Solver cp = solverFactory.get();
            IntVar x1 = makeIntVar(cp, 0, 0);
            IntVar x2 = makeIntVar(cp, 1, 1);
            IntVar x3 = makeIntVar(cp, 2, 2);
            IntVar y = maximum(x1, x2, x3);


            assertEquals(2, y.max());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void maximumTest3() {

        try {

            Solver cp = solverFactory.get();
            IntVar x1 = makeIntVar(cp, 0, 10);
            IntVar x2 = makeIntVar(cp, 0, 10);
            IntVar x3 = makeIntVar(cp, -5, 50);
            IntVar y = maximum(x1, x2, x3);

            y.removeAbove(5);
            cp.fixPoint();

            assertEquals(5, x1.max());
            assertEquals(5, x2.max());
            assertEquals(5, x3.max());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void maximumTest4() {
        try {
            try {
                Solver cp = solverFactory.get();
                IntVar[] x = makeIntVarArray(cp, 4, 5);
                IntVar y = makeIntVar(cp, -5, 20);

                DFSearch dfs = makeDfs(cp, and(firstFail(x), firstFail(y)));

                cp.post(new Maximum(x, y));
                // 5*5*5*5 // 625

                dfs.onSolution(() -> {
                    int max = Arrays.stream(x).mapToInt(xi -> xi.max()).max().getAsInt();
                    assertEquals(y.min(), max);
                    assertEquals(y.max(), max);
                });

                SearchStatistics stats = dfs.solve();

                assertEquals(625, stats.numberOfSolutions());

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


}
