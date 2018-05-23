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
import minicp.util.NotImplementedException;
import org.junit.Assume;
import org.junit.Test;

import java.util.Random;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;
import static org.junit.Assert.*;

public class ShortTableTest {



    private int[][] randomTuples(Random rand, int arity, int nTuples, int minvalue, int maxvalue) {
        int[][] r = new int[nTuples][arity];
        for (int i = 0; i < nTuples; i++)
            for (int j = 0; j < arity; j++)
                r[i][j] = rand.nextInt(maxvalue - minvalue) + minvalue;
        return r;
    }

    @Test
    public void simpleTest0() {
        try {
            try {
                Solver cp = makeSolver();
                IntVar[] x = makeIntVarArray(cp, 2, 1);
                int[][] table = new int[][]{{0, 0}};
                cp.post(new ShortTableCT(x, table,-1));

            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            Assume.assumeNoException(e);
        }
    }



    @Test
    public void simpleTest3() {
        try {
            try {
                Solver cp = makeSolver();
                IntVar[] x = makeIntVarArray(cp, 3, 12);
                int[][] table = new int[][]{{0, 0, 2},
                                            {3, 5, 7},
                                            {6, 9, 10},
                                            {1, 2, 3}};
                cp.post(new ShortTableCT(x, table,0));

                assertEquals(12, x[0].getSize());
                assertEquals(12, x[1].getSize());
                assertEquals(4, x[2].getSize());

                assertEquals(0,x[0].getMin());
                assertEquals(11,x[0].getMax());
                assertEquals(0,x[1].getMin());
                assertEquals(11,x[1].getMax());
                assertEquals(2,x[2].getMin());
                assertEquals(10,x[2].getMax());


            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            // pass
        }
    }

    @Test
    public void randomTest() {
        Random rand = new Random(67292);

        for (int i = 0; i < 50; i++) {
            int[][] tuples1 = randomTuples(rand, 3, 50, 2, 8);
            int[][] tuples2 = randomTuples(rand, 3, 50, 1, 7);
            int[][] tuples3 = randomTuples(rand, 3, 50, 0, 6);
            int star = 3;
            try {
                testTable(tuples1, tuples2, tuples3, star);
            } catch (NotImplementedException e) {
                Assume.assumeNoException(e);
            }
        }
    }


    public void testTable(int[][] t1, int[][] t2, int[][] t3, int star) {

        SearchStatistics statsDecomp;
        SearchStatistics statsAlgo;

        try {
            Solver cp = makeSolver();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new ShortTableDecomp(new IntVar[]{x[0], x[1], x[2]}, t1, star));
            cp.post(new ShortTableDecomp(new IntVar[]{x[2], x[3], x[4]}, t2, star));
            cp.post(new ShortTableDecomp(new IntVar[]{x[0], x[2], x[4]}, t3, star));
            statsDecomp = makeDfs(cp, firstFail(x)).solve();
        } catch (InconsistencyException e) {
            statsDecomp = null;
        }

        try {
            Solver cp = makeSolver();
            IntVar[] x = makeIntVarArray(cp, 5, 9);
            cp.post(allDifferent(x));
            cp.post(new ShortTableCT(new IntVar[]{x[0], x[1], x[2]}, t1, star));
            cp.post(new ShortTableCT(new IntVar[]{x[2], x[3], x[4]}, t2, star));
            cp.post(new ShortTableCT(new IntVar[]{x[0], x[2], x[4]}, t3, star));
            statsAlgo = makeDfs(cp, firstFail(x)).solve();
        } catch (InconsistencyException e) {
            statsAlgo = null;
        }

        assertTrue((statsDecomp == null && statsAlgo == null) || (statsDecomp != null && statsAlgo != null));
        if (statsDecomp != null) {
            assertEquals(statsDecomp.nSolutions, statsAlgo.nSolutions);
            assertEquals(statsDecomp.nFailures, statsAlgo.nFailures);
            assertEquals(statsDecomp.nNodes, statsAlgo.nNodes);
        }
    }
}