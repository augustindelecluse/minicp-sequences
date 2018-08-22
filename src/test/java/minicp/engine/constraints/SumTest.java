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

import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.makeDfs;
import static minicp.cp.Factory.makeIntVar;
import static org.junit.Assert.*;


public class SumTest extends SolverTest {

    @Test
    public void sum1() {
        try {
            Solver cp = solverFactory.get();
            IntVar y = makeIntVar(cp, -100, 100);
            IntVar[] x = new IntVar[]{makeIntVar(cp, 0, 5), makeIntVar(cp, 1, 5), makeIntVar(cp, 0, 5)};
            cp.post(new Sum(x, y));

            assertEquals(1, y.getMin());
            assertEquals(15, y.getMax());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }

    }

    @Test
    public void sum2() {
        try {
            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, -5, 5), makeIntVar(cp, 1, 2), makeIntVar(cp, 0, 1)};
            IntVar y = makeIntVar(cp, 0, 100);
            cp.post(new Sum(x, y));

            assertEquals(-3, x[0].getMin());
            assertEquals(0, y.getMin());
            assertEquals(8, y.getMax());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void sum3() {

        try {

            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, -5, 5), makeIntVar(cp, 1, 2), makeIntVar(cp, 0, 1)};
            IntVar y = makeIntVar(cp, 5, 5);
            cp.post(new Sum(x, y));

            x[0].removeBelow(1);
            // 1-5 + 1-2 + 0-1 = 5
            x[1].assign(1);
            // 1-5 + 1 + 0-1 = 5
            cp.fixPoint();

            assertEquals(4, x[0].getMax());
            assertEquals(3, x[0].getMin());
            assertEquals(1, x[2].getMax());
            assertEquals(0, x[2].getMin());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void sum4() {

        try {

            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, 0, 5), makeIntVar(cp, 0, 2), makeIntVar(cp, 0, 1)};
            cp.post(new Sum(x, 0));

            assertEquals(0, x[0].getMax());
            assertEquals(0, x[1].getMax());
            assertEquals(0, x[2].getMax());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void sum5() {
        try {

            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, -5, 0), makeIntVar(cp, -5, 0), makeIntVar(cp, -3, 0)};
            cp.post(new Sum(x, 0));

            assertEquals(0, x[0].getMin());
            assertEquals(0, x[1].getMin());
            assertEquals(0, x[2].getMin());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void sum6() {
        try {

            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, -5, 0), makeIntVar(cp, -5, 0), makeIntVar(cp, -3, 3)};
            cp.post(new Sum(x, 0));
            assertEquals(-3, x[0].getMin());
            assertEquals(-3, x[1].getMin());

            x[2].removeAbove(0);
            cp.fixPoint();

            assertEquals(0, x[0].getMin());
            assertEquals(0, x[1].getMin());
            assertEquals(0, x[2].getMin());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void sum7() {
        try {

            Solver cp = solverFactory.get();
            IntVar[] x = new IntVar[]{makeIntVar(cp, -5, 0), makeIntVar(cp, -5, 0), makeIntVar(cp, -3, 3)};
            cp.post(new Sum(x, 0));
            assertEquals(-3, x[0].getMin());
            assertEquals(-3, x[1].getMin());

            x[2].remove(1);
            x[2].remove(2);
            x[2].remove(3);
            x[2].remove(4);
            x[2].remove(5);
            cp.fixPoint();

            assertEquals(0, x[0].getMin());
            assertEquals(0, x[1].getMin());
            assertEquals(0, x[2].getMin());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void sum8() {
        try {

            Solver cp = solverFactory.get();

            // {0,0,0},  1
            // {-2,1,1}  3
            // {2,-1,-1} 3
            // {-1,1,0}  6
            // {0,-3,3}  6
            // {2,-2,0}  6
            // {-1,1,0}  6
            // {1,2,-3}  6


            IntVar[] x = new IntVar[]{makeIntVar(cp, -3, 3), makeIntVar(cp, -3, 3), makeIntVar(cp, -3, 3)};
            cp.post(new Sum(x, 0));

            DFSearch search = makeDfs(cp, firstFail(x));

            SearchStatistics stats = search.solve();

            assertEquals(37, stats.nSolutions);


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void sum9() {
        Solver cp = solverFactory.get();

        IntVar[] x = new IntVar[]{makeIntVar(cp, -9, -9)};
        boolean failed = false;
        try {
            cp.post(new Sum(x));
        } catch (InconsistencyException e) {
            failed = true;
        }
        assertTrue(failed);
    }


    @Test
    public void sum10() {
        Solver cp = solverFactory.get();

        IntVar[] x = new IntVar[]{makeIntVar(cp, -9, -4)};
        boolean failed = false;
        try {
            cp.post(new Sum(x));
        } catch (InconsistencyException e) {
            failed = true;
        }
        assertTrue(failed);
    }


}
