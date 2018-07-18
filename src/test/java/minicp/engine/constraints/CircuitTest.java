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
import org.junit.Test;

import static minicp.cp.Factory.*;
import static minicp.cp.Factory.notEqual;
import static minicp.cp.Heuristics.firstFail;
import static org.junit.Assert.*;


public class CircuitTest {


    int [] circuit1ok = new int []{1,2,3,4,5,0};
    int [] circuit2ok = new int []{1,2,3,4,5,0};

    int [] circuit1ko = new int []{1,2,3,4,5,2};
    int [] circuit2ko = new int []{1,2,0,4,5,3};

    public static boolean checkHamiltonian(int [] circuit) {
        int [] count = new int[circuit.length];
        for (int v: circuit) {
            count[v]++;
            if (count[v] > 1) return false;
        }
        boolean [] visited = new boolean[circuit.length];
        int c = circuit[0];
        for (int i = 0; i < circuit.length; i++) {
            visited[c] = true;
            c = circuit[c];
        }
        for (int i = 0; i < circuit.length; i++) {
            if (!visited[i]) return false;
        }
        return true;
    }

    public static IntVar[] instanciate(Solver cp, int [] circuit) {
        IntVar[] x = new IntVar[circuit.length];
        for (int i = 0; i < circuit.length; i++) {
            x[i] = makeIntVar(cp,circuit[i],circuit[i]);
        }
        return x;
    }

    @Test
    public void testCircuitOk() {
        try {
            try {
                Solver cp = makeSolver();
                cp.post(new Circuit(instanciate(cp,circuit1ok)));
                cp.post(new Circuit(instanciate(cp,circuit2ok)));
            } catch (InconsistencyException e) {
                fail("should not fail");
            }
        } catch (NotImplementedException e) {
            e.print();
        }
    }


    @Test
    public void testCircuitKo() {
        try {
            try {
                Solver cp = makeSolver();
                cp.post(new Circuit(instanciate(cp,circuit1ko)));
                fail("should fail");
            } catch (InconsistencyException e) {}
            try {
                Solver cp = makeSolver();
                cp.post(new Circuit(instanciate(cp,circuit2ko)));
                fail("should fail");
            } catch (InconsistencyException e) {}
        } catch (NotImplementedException e) {
            e.print();
        }
    }


    @Test
    public void testAllSolutions() {
        try {
            try {
                Solver cp = makeSolver();
                IntVar [] x = makeIntVarArray(cp,5,5);
                cp.post(new Circuit(x));

                cp.onSolution(() -> {
                            int[] sol = new int[x.length];
                            for (int i = 0; i < x.length; i++) {
                                sol[i] = x[i].getMin();
                            }
                            assertTrue("Solution is not an hamiltonian Circuit", checkHamiltonian(sol));
                        }
                );
                SearchStatistics stats = makeDfs(cp,firstFail(x)).solve();
            } catch (InconsistencyException e) { fail("should not fail");}
        } catch (NotImplementedException e) {
            e.print();
        }
    }


}
