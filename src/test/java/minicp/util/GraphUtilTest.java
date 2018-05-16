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

package minicp.util;

import minicp.engine.constraints.Absolute;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import org.junit.Test;

import java.util.Arrays;

import static minicp.util.GraphUtil.*;

import static minicp.cp.Factory.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class GraphUtilTest {



    @Test
    public void simpleTest0() {

        Integer [][] out = new Integer[][] {{1},{2},{0},{},{7},{4},{4},{6,8},{7}};
        Integer [][] in  = new Integer[][] {{2},{0},{1},{},{5},{6},{7},{4,8},{7}};
        Graph g = new Graph() {
            @Override
            public int n() {
                return 9;
            }

            @Override
            public Iterable<Integer> in(int idx) {
                return Arrays.asList(in[idx]);
            }

            @Override
            public Iterable<Integer> out(int idx) {
                return Arrays.asList(out[idx]);
            }
        };

        int [] scc = stronglyConnectedComponents(g);

        System.out.println(Arrays.toString(scc));
        assertEquals(scc[0], scc[1]);
        assertEquals(scc[0], scc[2]);

        assertNotEquals(scc[0], scc[3]);
        assertNotEquals(scc[4], scc[3]);

        assertEquals(scc[4], scc[5]);
        assertEquals(scc[4], scc[6]);
        assertEquals(scc[4], scc[7]);
        assertEquals(scc[4], scc[8]);

    }

    @Test
    public void simpleTest1() {

        Integer [][] out = new Integer[][] {{1},{2},{0},{},{7},{4},{},{8},{7}};
        Integer [][] in  = new Integer[][] {{2},{0},{1},{},{5},{6},{},{4,8},{7}};
        Graph g = new Graph() {
            @Override
            public int n() {
                return 9;
            }

            @Override
            public Iterable<Integer> in(int idx) {
                return Arrays.asList(in[idx]);
            }

            @Override
            public Iterable<Integer> out(int idx) {
                return Arrays.asList(out[idx]);
            }
        };

        int [] scc = stronglyConnectedComponents(g);

        System.out.println(Arrays.toString(scc));
        assertEquals(scc[0], scc[1]);
        assertEquals(scc[0], scc[2]);

        assertNotEquals(scc[0], scc[3]);
        assertNotEquals(scc[4], scc[5]);
        assertNotEquals(scc[4], scc[6]);
        assertNotEquals(scc[5], scc[6]);

        assertEquals(scc[7], scc[8]);

    }




}