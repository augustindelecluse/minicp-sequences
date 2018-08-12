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
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import org.junit.Test;

import static minicp.cp.Factory.*;
import static minicp.cp.BranchingScheme.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class AllDifferentTest {

    @Test
    public void allDifferentTest1() {

        Solver cp  = makeSolver();

        IntVar [] x = makeIntVarArray(cp,5,5);

        try {
            cp.post(allDifferent(x));
            equal(x[0],0);
            for (int i = 1; i < x.length; i++) {
                assertEquals(4,x[i].getSize());
                assertEquals(1,x[i].getMin());
            }

        } catch (InconsistencyException e) {
            assert(false);
        }
    }


    @Test
    public void allDifferentTest2() {

        Solver cp  = makeSolver();

        IntVar [] x = makeIntVarArray(cp,5,5);

        try {
            cp.post(allDifferent(x));

            SearchStatistics stats = makeDfs(cp,firstFail(x)).solve();
            assertEquals(120,stats.nSolutions);

        } catch (InconsistencyException e) {
            assert(false);
        }
    }

}
