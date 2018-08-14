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

package minicp.search;

import minicp.cp.BranchingScheme;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.state.Trailer;
import minicp.util.Counter;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import minicp.util.Procedure;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Supplier;

import static minicp.cp.Factory.*;
import static org.junit.Assert.assertEquals;


public class LimitedDiscrepancyBranchingTest {

    public static SearchObserver makeSearchObserver() {
        return new AbstractSearcher() {};
    }

    @Test
    public void testExample1() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = new Trailer();
        StateInt i = sm.makeStateInt(0);
        int [] values = new int[4];

        Supplier<Procedure[]> bs = () -> {
            if (i.getValue() >= values.length)
                return BranchingScheme.EMPTY;
            else return BranchingScheme.branch(
                    ()-> { // left branch
                        values[i.getValue()] = 0;
                        i.increment();
                    },
                    ()-> { // right branch
                        values[i.getValue()] = 1;
                        i.increment();
                    });
        };

        LimitedDiscrepancyBranching bsDiscrepancy =
                new LimitedDiscrepancyBranching(bs,2);

        DFSearch dfs = new DFSearch(sm,bsDiscrepancy);

        dfs.onSolution(() -> {
            int n1 = 0;
            for (int k = 0; k < values.length; k++) {
                n1 += values[k];
            }
            Assert.assertTrue(n1 <= 2);
        });

        SearchStatistics stats = dfs.solve();

        assertEquals(11, stats.nSolutions);
        assertEquals(0,stats.nFailures);
        assertEquals(24,stats.nNodes); // root node does not count
    }





}
