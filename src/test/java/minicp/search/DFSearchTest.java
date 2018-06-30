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

import minicp.cp.Factory;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.reversible.*;

import java.util.Arrays;

import static minicp.cp.Factory.makeSolver;
import static minicp.cp.Heuristics.firstFail;
import static minicp.search.Selector.*;

import minicp.util.Counter;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Test;
import static org.junit.Assert.*;


import static minicp.cp.Factory.*;


public class DFSearchTest {

    public static StateManager makeStateManager() {
        Trail tr = new TrailImpl();
        return  new StateManager() {
            @Override
            public Trail getTrail() {
                return tr;
            }

            @Override
            public int registerVar(IntVar x) {
                return 0;
            }
            @Override
            public int registerConstraint(Constraint c) {
                return 0;
            }
        };
    }

    @Test
    public void testExample1() {
        StateManager sm = makeStateManager();
        RevInt i = Factory.makeRevInt(sm,0);
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

        DFSearch dfs = new DFSearch(cp,() -> {
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
        StateManager sm = makeStateManager();
        RevInt i = Factory.makeRevInt(sm,0);
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


        dfs.onSolution(() -> {
            assert(Arrays.stream(values).allMatch(v -> v == 1));
        });


        SearchStatistics stats = dfs.solve(stat -> stat.nSolutions >= 1);

        assert(stats.nSolutions == 1);
    }




    @Test
    public void testDFS() {
        StateManager sm = makeStateManager();
        RevInt i = Factory.makeRevInt(sm,0);
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


        assert(nSols.getValue() == 16);
        assert(stats.nSolutions == 16);
        assert(stats.nFailures == 0);
        assert(stats.nNodes == (16+8+4+2));

    }

    @Test
    public void testDFSSearchLimit() {
        StateManager sm = makeStateManager();
        RevInt i = Factory.makeRevInt(sm,0);
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
        dfs.onFail(() -> {
            nFails.incr();
        });


        // stop search after 2 solutions
        SearchStatistics stats = dfs.solve(stat -> stat.nFailures >= 3);

        assert(stats.nSolutions == 0);
        assert(stats.nFailures == 3);

    }


    @Test
    public void testDeepDFS() {
        StateManager sm = makeStateManager();
        RevInt i = Factory.makeRevInt(sm,0);
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
