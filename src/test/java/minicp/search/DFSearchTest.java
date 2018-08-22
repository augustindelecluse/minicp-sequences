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

import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.state.StateManagerTest;
import minicp.util.Counter;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Test;

import java.util.Arrays;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DFSearchTest extends StateManagerTest {

    public static SearchObserver makeSearchObserver() {
        return new AbstractSearcher() {
        };
    }

    @Test
    public void testExample1() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        int[] values = new int[3];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch(
                    () -> { // left branch
                        values[i.getValue()] = 0;
                        i.increment();
                    },
                    () -> { // right branch
                        values[i.getValue()] = 1;
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
        SearchObserver r = makeSearchObserver();
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        int[] values = new int[3];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch(
                    () -> { // left branch
                        values[i.getValue()] = 1;
                        i.increment();
                    },
                    () -> { // right branch
                        values[i.getValue()] = 0;
                        i.increment();
                    }
            );
        });


        r.onSolution(() -> {
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

        Counter nSols = new Counter();


        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch(
                    () -> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.getValue()] = true;
                        i.increment();
                    }
            );
        });

        dfs.onSolution(() -> {
            nSols.incr();
        });


        SearchStatistics stats = dfs.solve();


        assertEquals(16, nSols.getValue());
        assertEquals(16, stats.numberOfSolutions());
        assertEquals(0, stats.numberOfFailures());
        assertEquals((16 + 8 + 4 + 2), stats.numberOfNodes());

    }

    @Test
    public void testDFSSearchLimit() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = stateFactory.get();

        StateInt i = sm.makeStateInt(0);
        boolean[] values = new boolean[4];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.getValue() >= values.length) {
                return branch(() -> {
                    throw new InconsistencyException();
                });
            } else return branch(
                    () -> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.getValue()] = true;
                        i.increment();
                    }
            );
        });

        Counter nFails = new Counter();
        r.onFailure(() -> {
            nFails.incr();
        });


        // stop search after 2 solutions
        SearchStatistics stats = dfs.solve(stat -> stat.numberOfFailures() >= 3);

        assertEquals (0,stats.numberOfSolutions());
        assertEquals (3,stats.numberOfFailures());

    }


    @Test
    public void testDeepDFS() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = stateFactory.get();
        StateInt i = sm.makeStateInt(0);
        boolean[] values = new boolean[10000];

        DFSearch dfs = new DFSearch(sm, () -> {
            if (i.getValue() >= values.length) {
                return EMPTY;
            } else return branch(
                    () -> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    () -> {
                        // right branch
                        values[i.getValue()] = true;
                        i.increment();
                    }
            );
        });
        try {
            // stop search after 1 solutions (only left most branch)
            SearchStatistics stats = dfs.solve(stat -> stat.numberOfSolutions() >= 1);
            assertEquals (1,stats.numberOfSolutions());
        } catch (NotImplementedException e) {
            e.print();
        }

    }


}
