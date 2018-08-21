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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class Element1DVarTest extends SolverTest {

    @Test
    public void element1dVarTest1() {

        try {

            Solver cp = solverFactory.get();
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, 2, 40);

            IntVar[] T = new IntVar[]{makeIntVar(cp, 9, 9), makeIntVar(cp, 8, 8), makeIntVar(cp, 7, 7), makeIntVar(cp, 5, 5), makeIntVar(cp, 6, 6)};

            cp.post(new Element1DVar(T, y, z));

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
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void element1dVarTest2() {

        try {

            Solver cp = solverFactory.get();
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -4, 40);

            IntVar[] T = new IntVar[]{makeIntVar(cp, 1, 2),
                    makeIntVar(cp, 3, 4),
                    makeIntVar(cp, 5, 6),
                    makeIntVar(cp, 7, 8),
                    makeIntVar(cp, 9, 10)};

            cp.post(new Element1DVar(T, y, z));

            assertEquals(0, y.getMin());
            assertEquals(4, y.getMax());

            assertEquals(1, z.getMin());
            assertEquals(10, z.getMax());

            y.removeAbove(2);
            cp.fixPoint();

            assertEquals(6, z.getMax());

            y.assign(2);
            cp.fixPoint();

            assertEquals(5, z.getMin());
            assertEquals(6, z.getMax());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void element1dVarTest3() {

        try {

            Solver cp = solverFactory.get();
            IntVar y = makeIntVar(cp, -3, 10);
            IntVar z = makeIntVar(cp, -20, 40);

            IntVar[] T = new IntVar[]{makeIntVar(cp, 9, 9), makeIntVar(cp, 8, 8), makeIntVar(cp, 7, 7), makeIntVar(cp, 5, 5), makeIntVar(cp, 6, 6)};

            cp.post(new Element1DVar(T, y, z));

            DFSearch dfs = makeDfs(cp, firstFail(y, z));
            dfs.onSolution(() ->
                    assertEquals(T[y.getMin()].getMin(), z.getMin())
            );
            SearchStatistics stats = dfs.solve();

            assertEquals(5, stats.nSolutions);


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

}
