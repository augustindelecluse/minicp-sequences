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

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.GraphUtil;
import minicp.util.GraphUtil.*;
import minicp.util.InconsistencyException;

import java.util.ArrayList;
import java.util.Arrays;

public class AllDifferentAC extends Constraint {

    private IntVar [] x;
    private int [] unbound;
    private int nUnBound;

    private int [] match;

    private final MaximumMatching maximumMatching;

    public AllDifferentAC(IntVar ... x) {
        super(x[0].getSolver());
        maximumMatching = new MaximumMatching(x);
        match = new int[x.length];
        unbound = new int[x.length];
        this.x = x;
    }

    @Override
    public void post() throws InconsistencyException {
        for (int i = 0; i < x.length; i++) {
            x[i].propagateOnDomainChange(this);
        }
        propagate();
    }

    @Override
    public void propagate() throws InconsistencyException {
        int size = maximumMatching.compute(match);
        if (size < x.length) {
            throw new InconsistencyException();
        }

        int minVal = Integer.MAX_VALUE;
        int maxVal = Integer.MIN_VALUE;
        nUnBound = 0;
        for (int i = 0; i < x.length; i++) {
            if (!x[i].isBound()) {
                minVal = Math.min(minVal, x[i].getMin());
                maxVal = Math.max(maxVal, x[i].getMax());
                unbound[nUnBound] = i;
                nUnBound++;
            }
        }


        final int nNodes = nUnBound+(maxVal-minVal+1) +1 ;
        int sink = nNodes -1;

        ArrayList<Integer> [] in = new ArrayList[nNodes];
        ArrayList<Integer> [] out = new ArrayList[nNodes];
        for (int i = 0; i < nNodes; i++) {
            in[i] = new ArrayList<>();
            out[i] = new ArrayList<>();
        }

        boolean [] matched = new boolean[maxVal-minVal+1];
        for (int i = 0; i < nUnBound; i++) {
            in[i].add(match[unbound[i]]-minVal+nUnBound);
            out[match[unbound[i]]-minVal+nUnBound].add(i);
            matched [match[unbound[i]]-minVal] = true;
        }
        for (int i = 0; i < nUnBound; i++) {
            for (int v = x[unbound[i]].getMin(); v <= x[unbound[i]].getMax(); v++) {
                if (x[unbound[i]].contains(v) && match[unbound[i]] != v) {
                    in[v-minVal+nUnBound].add(i);
                    out[i].add(v-minVal+nUnBound);
                }
            }
        }
        for (int v = minVal; v < maxVal; v++) {
            if (!matched[v-minVal]) {
                in[sink].add(v-minVal+nUnBound);
                out[v-minVal+nUnBound].add(sink);
            } else {
                in[v-minVal+nUnBound].add(sink);
                out[sink].add(v-minVal+nUnBound);
            }
        }


        Graph g = new Graph() {
            @Override
            public int n() {
                return nNodes;
            }

            @Override
            public Iterable<Integer> in(int idx) {
                return in[idx];
            }

            @Override
            public Iterable<Integer> out(int idx) {
                return out[idx];
            }
        };

        int [] scc = GraphUtil.stronglyConnectedComponents(g);
        System.out.println(Arrays.toString(scc));

        for (int i = 0; i < nUnBound; i++) {
            for (int v = minVal; v <= maxVal; v++) {
                if (scc[i] != scc[v-minVal+nUnBound]) {
                    x[unbound[i]].remove(v);
                }
            }
        }


    }
}
