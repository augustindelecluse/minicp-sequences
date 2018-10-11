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

package minicp.util;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static minicp.util.GraphUtil.Graph;
import static minicp.util.GraphUtil.pathExists;
import static minicp.util.GraphUtil.stronglyConnectedComponents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class GraphUtilTest {


    @Test
    public void simpleTestSCC0() {

        Integer[][] out = new Integer[][]{{1}, {2}, {0}, {}, {7}, {4}, {4}, {6, 8}, {7}};
        Integer[][] in = inFromOut(out);
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

        int[] scc = stronglyConnectedComponents(g);

        assertEquals(scc[0], scc[1]);
        assertEquals(scc[0], scc[2]);

        assertNotEquals(scc[0], scc[3]);
        assertNotEquals(scc[0], scc[4]);
        assertNotEquals(scc[0], scc[5]);
        assertNotEquals(scc[4], scc[3]);
        assertNotEquals(scc[4], scc[5]);
        assertNotEquals(scc[5], scc[3]);

        assertEquals(scc[4], scc[6]);
        assertEquals(scc[4], scc[7]);
        assertEquals(scc[4], scc[8]);
    }

    @Test
    public void simpleTestSCC1() {

        Integer[][] out = new Integer[][]{{1}, {2}, {0}, {}, {7}, {4}, {}, {8}, {7}};
        Integer[][] in = inFromOut(out);
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

        int[] scc = stronglyConnectedComponents(g);

        assertEquals(scc[0], scc[1]);
        assertEquals(scc[0], scc[2]);

        assertNotEquals(scc[0], scc[3]);
        assertNotEquals(scc[4], scc[5]);
        assertNotEquals(scc[4], scc[6]);
        assertNotEquals(scc[5], scc[6]);

        assertEquals(scc[7], scc[8]);

    }

    @Test
    public void simpleTestSCC2() {

        Integer[][] out = new Integer[][]{{11, 12, 14}, {}, {8}, {7, 9, 11, 12}, {}, {3}, {15}, {15}, {4}, {0}, {2}, {15}, {15},{1},{15},{5, 8, 9, 10, 13}};
        Integer[][] in = inFromOut(out);
        Graph g = new Graph() {
            @Override
            public int n() {
                return out.length;
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

        int[] scc = stronglyConnectedComponents(g);

        for (int start = 0; start < g.n(); start++) {
            for (int end = 0; end < g.n(); end++) {
                if (start != end) {
                    assertEquals(scc[start] == scc[end], pathExists(g, start, end) && pathExists(g, end, start));
                }
            }
        }
    }


    public static Integer[][] inFromOut(Integer[][] out) {
        ArrayList<Integer>[] in = new ArrayList[out.length];
        for (int i = 0; i < out.length; i++) {
            in[i] = new ArrayList<Integer>();
        }
        for (int i = 0; i < out.length; i++) {
            for (int j = 0; j < out[i].length; j++) {
                in[out[i][j]].add(i);
            }
        }

        Integer[][] inA = new Integer[out.length][];
        for (int i = 0; i < out.length; i++) {
            inA[i] = in[i].toArray(new Integer[0]);
        }

        return inA;
    }

}