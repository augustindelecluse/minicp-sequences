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

import minicp.engine.constraints.Profile.Rectangle;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class DisjunctiveTest {


    private static void decomposeDisjunctive(IntVar[] start, int[] duration) throws InconsistencyException {
        Solver cp = start[0].getSolver();
        for (int i = 0; i < start.length; i++) {
            IntVar end_i = plus(start[i],duration[i]);
            for (int j = i+1; j < start.length; j++) {
                // i before j or j before i

                IntVar end_j = plus(start[j],duration[j]);
                BoolVar iBeforej = makeBoolVar(cp);
                BoolVar jBeforei = makeBoolVar(cp);

                cp.post(new IsLessOrEqualVar(iBeforej,end_i,start[j]));
                cp.post(new IsLessOrEqualVar(jBeforei,end_j,start[i]));
                cp.post(new NotEqual(iBeforej,jBeforei),false);

            }
        }

    }

    @Test
    public void testAllDiffDisjunctive() {

        try {

            Solver cp = makeSolver();

            IntVar[] s = makeIntVarArray(cp, 5, 5);
            int[] d = new int[5];
            Arrays.fill(d,1);

            cp.post(new Disjunctive(s,d));

            SearchStatistics stats = makeDfs(cp,firstFail(s)).start();
            assertEquals("disjunctive alldiff expect all permutations",120, stats.nSolutions);

        } catch (InconsistencyException e) {
            assert (false);
        }

    }

    @Test
    public void testNotRemovingSolutions() {

        try {

            Solver cp = makeSolver();

            IntVar[] s = makeIntVarArray(cp, 4, 20);
            int[] d = new int[] {5,4,6,7};
            DFSearch dfs = makeDfs(cp, firstFail(s));

            cp.push();

            cp.post(new Disjunctive(s,d));

            SearchStatistics stat1 = dfs.start();

            cp.pop();

            decomposeDisjunctive(s,d);

            SearchStatistics stat2 = dfs.start();

            assertEquals(stat1.nSolutions, stat2.nSolutions);

            System.out.println(stat1.nSolutions+" "+stat2.nSolutions);

        } catch (InconsistencyException e) {
            assert (false);
        }
    }



}
