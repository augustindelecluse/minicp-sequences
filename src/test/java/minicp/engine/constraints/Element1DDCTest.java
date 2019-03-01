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

package minicp.engine.constraints;

import com.github.guillaumederval.javagrading.GradeClass;
import minicp.engine.SolverTest;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.NotImplementedExceptionAssume;
import minicp.util.Procedure;
import minicp.util.exception.NotImplementedException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.function.Supplier;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;
import static minicp.cp.Factory.notEqual;
import static org.junit.Assert.*;

@GradeClass(totalValue = 1, defaultCpuTimeout = 1000)
public class Element1DDCTest extends SolverTest {

    @Test
    public void domainConsistencyTest() {

        try {
            Solver cp = solverFactory.get();

            Random rand = new Random(678);
            IntVar y = makeIntVar(cp, 0, 1);
            IntVar z = makeIntVar(cp, 0, 15);


            int[] T = new int[15];
            for(int i = 0; i < 15; i++)
                T[i] = rand.nextInt(15);

            cp.post(new Element1D(T, y, z));

            assertTrue(y.max() < T.length);

            Supplier<Procedure[]> branching = () -> {
                if(y.isBound() && z.isBound()) {
                    assertEquals(T[y.min()], z.min());
                    return EMPTY;
                }
                int[] possibleY = new int[y.size()];
                y.fillArray(possibleY);

                int[] possibleZ = new int[z.size()];
                z.fillArray(possibleZ);

                HashSet<Integer> possibleValues = new HashSet<>();
                HashSet<Integer> possibleValues2 = new HashSet<>();
                for(int i = 0; i < possibleZ.length; i++)
                    possibleValues.add(possibleZ[i]);

                for(int i = 0; i < possibleY.length; i++) {
                    assertTrue(possibleValues.contains(T[possibleY[i]]));
                    possibleValues2.add(T[possibleY[i]]);
                }
                //assertEquals(possibleValues.size(), possibleValues2.size());

                if(!y.isBound() && (z.isBound() || rand.nextBoolean())) {
                    //select a random y
                    final int val = possibleY[rand.nextInt(possibleY.length)];
                    return branch(() -> equal(y, val),
                                  () -> notEqual(y, val));
                }
                else {
                    final int val = possibleZ[rand.nextInt(possibleZ.length)];
                    return branch(() -> equal(z, val),
                                  () -> notEqual(z, val));
                }
            };

            DFSearch dfs = makeDfs(cp, branching);

            SearchStatistics stats = dfs.solve();
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }
}
