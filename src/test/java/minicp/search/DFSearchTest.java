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
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.search;

import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.state.StateManagerTest;
import minicp.util.NotImplementedExceptionAssume;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DFSearchTest extends StateManagerTest {



    @Test
    public void testExample1() {
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        int[] values = new int[3];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.value() >= values.length)
                return EMPTY;
            else return branch(
                    () -> { // left branch
                        values[i.value()] = 0;
                        i.increment();
                    },
                    () -> { // right branch
                        values[i.value()] = 1;
                        i.increment();
                    }
            );
        });


        SearchStatistics stats = dfs.solve();

        assertEquals(8,stats.numberOfSolutions());
        assertEquals (0,stats.numberOfFailures());
        assertEquals (8 + 4 + 2,stats.numberOfNodes());
    }


    @Test
    public void testExample3() {
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        int[] values = new int[3];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.value() >= values.length)
                return EMPTY;
            else return branch(
                    () -> { // left branch
                        values[i.value()] = 1;
                        i.increment();
                    },
                    () -> { // right branch
                        values[i.value()] = 0;
                        i.increment();
                    }
            );
        });


        dfs.onSolution(() -> {
            assert (Arrays.stream(values).allMatch(v -> v == 1));
        });


        SearchStatistics stats = dfs.solve(stat -> stat.numberOfSolutions() >= 1);

        assertEquals (1,stats.numberOfSolutions());
    }


    @Test
    public void testDFS() {
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        boolean[] values = new boolean[4];

        AtomicInteger nSols = new AtomicInteger(0);


        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.value() >= values.length)
                return EMPTY;
            else return branch(
                    () -> {
                        // left branch
                        values[i.value()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.value()] = true;
                        i.increment();
                    }
            );
        });

        dfs.onSolution(() -> {
            nSols.incrementAndGet();
        });


        SearchStatistics stats = dfs.solve();


        assertEquals(16, nSols.get());
        assertEquals(16, stats.numberOfSolutions());
        assertEquals(0, stats.numberOfFailures());
        assertEquals((16 + 8 + 4 + 2), stats.numberOfNodes());

    }

    @Test
    public void testDFSSearchLimit() {
        StateManager sm = stateFactory.get();

        StateInt i = sm.makeStateInt(0);
        boolean[] values = new boolean[4];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.value() >= values.length) {
                return branch(() -> {
                    throw new InconsistencyException();
                });
            } else return branch(
                    () -> {
                        // left branch
                        values[i.value()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.value()] = true;
                        i.increment();
                    }
            );
        });




        // stop search after 2 solutions
        SearchStatistics stats = dfs.solve(stat -> stat.numberOfFailures() >= 3);

        assertEquals (0,stats.numberOfSolutions());
        assertEquals (3,stats.numberOfFailures());

    }


    @Test
    public void testDeepDFS() {
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        boolean[] values = new boolean[10000];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.value() >= values.length) {
                return EMPTY;
            } else return branch(
                    () -> {
                        // left branch
                        values[i.value()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.value()] = true;
                        i.increment();
                    }
            );
        });
        try {
            // stop search after 1 solutions (only left most branch)
            SearchStatistics stats = dfs.solve(stat -> stat.numberOfSolutions() >= 1);
            assertEquals (1,stats.numberOfSolutions());
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }

    }


}
