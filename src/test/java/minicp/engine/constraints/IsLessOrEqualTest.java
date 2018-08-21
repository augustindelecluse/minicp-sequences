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
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.junit.Test;

import static minicp.cp.Factory.*;
import static minicp.cp.BranchingScheme.firstFail;
import static org.junit.Assert.*;


public class IsLessOrEqualTest extends SolverTest {

    @Test
    public void test1() {
        try {

            Solver cp  = solverFactory.get();
            IntVar x = makeIntVar(cp, -4, 7);

            BoolVar b = makeBoolVar(cp);

            cp.post(new IsLessOrEqual(b, x, 3));

            DFSearch search = makeDfs(cp, firstFail(x));

            search.onSolution(() ->
                    assertTrue(x.getMin() <= 3 && b.isTrue() || x.getMin() > 3 && b.isFalse())
            );

            SearchStatistics stats = search.solve();


            assertEquals(12, stats.nSolutions);

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void test2() {

        try {

            Solver cp  = solverFactory.get();
            IntVar x = makeIntVar(cp, -4, 7);

            BoolVar b = makeBoolVar(cp);

            cp.post(new IsLessOrEqual(b, x, -2));

            cp.getStateManager().save();
            equal(b, 1);
            assertEquals(-2, x.getMax());
            cp.getStateManager().restore();

            cp.getStateManager().save();
            equal(b, 0);
            assertEquals(-1, x.getMin());
            cp.getStateManager().restore();

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void test3() {
        try {

            Solver cp  = solverFactory.get();
            IntVar x = makeIntVar(cp, -4, 7);
            equal(x, -2);
            {
                BoolVar b = makeBoolVar(cp);
                cp.post(new IsLessOrEqual(b, x, -2));
                assertTrue(b.isTrue());
            }
            {
                BoolVar b = makeBoolVar(cp);
                cp.post(new IsLessOrEqual(b, x, -3));
                assertTrue(b.isFalse());
            }

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void test4() {
        try {

            Solver cp  = solverFactory.get();
            IntVar x = makeIntVar(cp, -4, 7);
            BoolVar b = makeBoolVar(cp);

            cp.getStateManager().save();
            equal(b, 1);
            cp.post(new IsLessOrEqual(b, x, -2));
            assertEquals(-2, x.getMax());
            cp.getStateManager().restore();

            cp.getStateManager().save();
            equal(b, 0);
            cp.post(new IsLessOrEqual(b, x, -2));
            assertEquals(-1, x.getMin());
            cp.getStateManager().restore();


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


}
