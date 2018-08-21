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
import java.util.HashSet;

import static minicp.cp.Factory.*;
import static minicp.cp.BranchingScheme.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class AllDifferentACTest extends SolverTest {

    @Test
    public void allDifferentTest1() {

        Solver cp = solverFactory.get();

        IntVar[] x = makeIntVarArray(cp, 5, 5);

        try {
            cp.post(new AllDifferentAC(x));
            equal(x[0], 0);
            for (int i = 1; i < x.length; i++) {
                assertEquals(4, x[i].getSize());
                assertEquals(1, x[i].getMin());
            }

        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void allDifferentTest2() {

        Solver cp = solverFactory.get();

        IntVar[] x = makeIntVarArray(cp, 5, 5);

        try {
            cp.post(new AllDifferentAC(x));

            SearchStatistics stats = makeDfs(cp, firstFail(x)).solve();
            assertEquals(120, stats.nSolutions);

        } catch (InconsistencyException e) {
            assert (false);
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    private static IntVar makeIVar(Solver cp, Integer... values) {
        return makeIntVar(cp, new HashSet<>(Arrays.asList(values)));
    }


    @Test
    public void allDifferentTest3() {
        try {
            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{
                    makeIVar(cp, 1, 2),
                    makeIVar(cp, 1, 2),
                    makeIVar(cp, 1, 2, 3, 4)};
            int[] matching = new int[x.length];

            cp.post(new AllDifferentAC(x));

            assertEquals(x[2].getMin(), 3);
            assertEquals(x[2].getSize(), 2);

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }

    }

    @Test
    public void allDifferentTest5() {
        try {
            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{
                    makeIVar(cp, 1, 2, 3, 4, 5),
                    makeIVar(cp, 2),
                    makeIVar(cp, 1, 2, 3, 4, 5),
                    makeIVar(cp, 1),
                    makeIVar(cp, 1, 2, 3, 4, 5, 6),
                    makeIVar(cp, 6, 7, 8),
                    makeIVar(cp, 3),
                    makeIVar(cp, 6, 7, 8, 9),
                    makeIVar(cp, 6, 7, 8)};
            int[] matching = new int[x.length];

            cp.post(new AllDifferentAC(x));

            assertEquals(x[0].getSize(), 2);
            assertEquals(x[2].getSize(), 2);
            assertEquals(x[4].getMin(), 6);
            assertEquals(x[7].getMin(), 9);
            assertEquals(x[8].getMin(), 7);
            assertEquals(x[8].getMax(), 8);

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void allDifferentTest6() {
        try {
            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{
                    makeIVar(cp, 1, 2, 3, 4, 5),
                    makeIVar(cp, 2, 7),
                    makeIVar(cp, 1, 2, 3, 4, 5),
                    makeIVar(cp, 1, 3),
                    makeIVar(cp, 1, 2, 3, 4, 5, 6),
                    makeIVar(cp, 6, 7, 8),
                    makeIVar(cp, 3, 4, 5),
                    makeIVar(cp, 6, 7, 8, 9),
                    makeIVar(cp, 6, 7, 8)};
            int[] matching = new int[x.length];

            cp.post(new AllDifferentAC(x));

            DFSearch dfs = makeDfs(cp, () -> {
                IntVar xs = selectMin(x,
                        xi -> xi.getSize() > 1,
                        xi -> (float) -xi.getSize());
                if (xs == null)
                    return EMPTY;
                else {
                    int v = xs.getMin();
                    return branch(
                            () -> {
                                equal(xs, v);
                            },
                            () -> {
                                notEqual(xs, v);
                            });
                }
            });

            SearchStatistics stats = dfs.solve();
            // GAC filter with a single constraint should have no fail
            assertEquals(0, stats.nFailures);
            assertEquals(80, stats.nSolutions);


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void allDifferentTest7() {
        try {
            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{
                    makeIVar(cp, 3, 4),
                    makeIVar(cp, 1),
                    makeIVar(cp, 3, 4),
                    makeIVar(cp, 0),
                    makeIVar(cp, 3, 4, 5),
                    makeIVar(cp, 5, 6, 7),
                    makeIVar(cp, 2, 9, 10),
                    makeIVar(cp, 5, 6, 7, 8),
                    makeIVar(cp, 5, 6, 7)};
            int[] matching = new int[x.length];

            cp.post(new AllDifferentAC(x));

            assertTrue(!x[4].contains(3));
            assertTrue(!x[4].contains(4));
            assertTrue(!x[5].contains(5));
            assertTrue(!x[7].contains(5));
            assertTrue(!x[7].contains(6));
            assertTrue(!x[8].contains(5));
        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


}
