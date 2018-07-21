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

import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;

import java.util.Arrays;

import static minicp.cp.Factory.makeSolver;
import static minicp.cp.Heuristics.firstFail;
import static minicp.search.Selector.*;

import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.state.Trailer;
import minicp.util.Counter;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Test;
import static org.junit.Assert.*;


import static minicp.cp.Factory.*;


public class DFSearchTest {

    public static SearchObserver makeSearchObserver() {
        return new AbstractSearcher() {};
    }

    @Test
    public void testExample1() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = new Trailer();
        StateInt i = sm.makeStateInt(0);
        int [] values = new int[3];

        DFSearch dfs = new DFSearch(sm,() -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch(
                    ()-> { // left branch
                        values[i.getValue()] = 0;
                        i.increment();
                    },
                    ()-> { // right branch
                        values[i.getValue()] = 1;
                        i.increment();
                    }
            );
        });


        SearchStatistics stats = dfs.solve();

        assert(stats.nSolutions == 8);
        assert(stats.nFailures == 0);
        assert(stats.nNodes == (8+4+2));
    }

    @Test
    public void testExample2() {
        Solver cp = makeSolver();
        IntVar[] values = makeIntVarArray(cp,3,2);

        DFSearch dfs = makeDfs(cp,() -> {
            int sel = -1;
            for(int i = 0 ; i < values.length;i++)
                if (values[i].getSize() > 1 && sel == -1)
                    sel = i;
            final int i = sel;
            if (i == -1)
                return EMPTY;
            else return branch(()-> equal(values[i],0),
                               ()-> equal(values[i],1));
        });


        SearchStatistics stats = dfs.solve();

        assert(stats.nSolutions == 8);
        assert(stats.nFailures == 0);
        assert(stats.nNodes == (8+4+2));
    }

    @Test
    public void testExample3() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = new Trailer();
        StateInt i = sm.makeStateInt(0);
        int [] values = new int[3];

        DFSearch dfs = new DFSearch(sm,() -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch(
                    ()-> { // left branch
                        values[i.getValue()] = 1;
                        i.increment();
                    },
                    ()-> { // right branch
                        values[i.getValue()] = 0;
                        i.increment();
                    }
            );
        });


        r.onSolution(() -> {
            assert(Arrays.stream(values).allMatch(v -> v == 1));
        });


        SearchStatistics stats = dfs.solve(stat -> stat.nSolutions >= 1);

        assert(stats.nSolutions == 1);
    }




    @Test
    public void testDFS() {
        StateManager sm = new Trailer();
        StateInt i = sm.makeStateInt(0);
        boolean [] values = new boolean[4];

        Counter nSols = new Counter();


        DFSearch dfs = new DFSearch(sm,() -> {
            if (i.getValue() >= values.length)
                return EMPTY;
            else return branch (
                    ()-> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    ()-> {
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
        assertEquals(16, stats.nSolutions);
        assertEquals(0,stats.nFailures );
        assertEquals((16+8+4+2), stats.nNodes);

    }

    @Test
    public void testDFSSearchLimit() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = new Trailer();

        StateInt i = sm.makeStateInt(0);
        boolean [] values = new boolean[4];

        DFSearch dfs = new DFSearch(sm,() -> {
            if (i.getValue() >= values.length) {
                return branch(() -> {throw new InconsistencyException();});
            }
            else return branch (
                    ()-> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    ()-> {
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
        SearchStatistics stats = dfs.solve(stat -> stat.nFailures >= 3);

        assert(stats.nSolutions == 0);
        assert(stats.nFailures == 3);

    }


    @Test
    public void testDeepDFS() {
        SearchObserver r = makeSearchObserver();
        StateManager sm = new Trailer();
        StateInt i = sm.makeStateInt(0);
        boolean [] values = new boolean[10000];

        DFSearch dfs = new DFSearch(sm,() -> {
            if (i.getValue() >= values.length) {
                return EMPTY;
            }
            else return branch (
                    ()-> {
                        // left branch
                        values[i.getValue()] = false;
                        i.increment();
                    },
                    ()-> {
                        // right branch
                        values[i.getValue()] = true;
                        i.increment();
                    }
            );
        });
        try {
            // stop search after 1 solutions (only left most branch)
            SearchStatistics stats = dfs.solve(stat -> stat.nSolutions >= 1);
            assert(stats.nSolutions == 1);
        } catch (NotImplementedException e) {
            e.print();
        }

    }


    @Test
    public void testSolveSubjectTo() {
        Solver cp = makeSolver();
        IntVar[] x = makeIntVarArray(cp,3,2);

        DFSearch dfs = makeDfs(cp,firstFail(x));


        SearchStatistics stats1 = dfs.solveSubjectTo(l -> false, () -> {
            equal(x[0],0);
        });

        assertEquals(4,stats1.nSolutions);

        SearchStatistics stats2 = dfs.solve(l -> false);

        assertEquals(8,stats2.nSolutions);


    }



}
