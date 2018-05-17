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
import minicp.reversible.ReversibleInt;
import minicp.util.GraphUtil;
import minicp.util.GraphUtil.*;
import minicp.util.InconsistencyException;

import java.util.ArrayList;
import java.util.Arrays;

public class AllDifferentAC extends Constraint {

    private IntVar[] x;

    // residual graph
    private ArrayList<Integer>[] in;
    private ArrayList<Integer>[] out;
    private int nNodes;
    Graph g = new Graph() {
        @Override
        public int n() { return nNodes; }

        @Override
        public Iterable<Integer> in(int idx) { return in[idx]; }

        @Override
        public Iterable<Integer> out(int idx) { return out[idx]; }
    };


    private int[] unbound;
    private ReversibleInt nUnBound;

    private int[] match;
    boolean[] matched;

    private int minVal;
    private int maxVal;

    private final MaximumMatching maximumMatching;

    public AllDifferentAC(IntVar... x) {
        super(x[0].getSolver());
        maximumMatching = new MaximumMatching(x);
        match = new int[x.length];
        unbound = new int[x.length];
        for (int i = 0; i < x.length; i++) {
            unbound[i] = i;
        }
        nUnBound = new ReversibleInt(cp.getTrail(),x.length);
        this.x = x;
    }

    @Override
    public void post() throws InconsistencyException {
        for (int i = 0; i < x.length; i++) {
            x[i].propagateOnDomainChange(this);
        }
        updateRange();

        matched = new boolean[maxVal - minVal + 1];
        nNodes = nUnBound.getValue() + (maxVal - minVal + 1) + 1;
        in = new ArrayList[nNodes];
        out = new ArrayList[nNodes];
        for (int i = 0; i < nNodes; i++) {
            in[i] = new ArrayList<>();
            out[i] = new ArrayList<>();
        }

        propagate();
    }

    public void updateRange() throws InconsistencyException {
        minVal = Integer.MAX_VALUE;
        maxVal = Integer.MIN_VALUE;
        int nU = nUnBound.getValue();
        for (int i = 0; i < nU; i++) {
            minVal = Math.min(minVal, x[unbound[i]].getMin());
            maxVal = Math.max(maxVal, x[unbound[i]].getMax());
        }
    }

    public void forwardChecking() throws InconsistencyException {
        int nU = nUnBound.getValue();
        int i = nU-1;
        while (i >= 0) {
            if (x[unbound[i]].isBound()) {
                int value = x[unbound[i]].getMin();
                for (int j = 0; j < nU; j++) {
                    if (j != i) {
                        x[unbound[j]].remove(value);
                    }
                }
                int tmp = unbound[nU-1];
                unbound[nU-1] = unbound[i];
                unbound[i] = tmp;
                nU -= 1;
            }
            i -= 1;
        }
        nUnBound.setValue(nU); // trail
    }

    public void updateGraph() {
        int nU = nUnBound.getValue();
        nNodes = nU + (maxVal - minVal + 1) + 1;
        int sink = nNodes - 1;
        for (int i = 0; i < nNodes; i++) {
            in[i].clear();
            out[i].clear();
        }
        Arrays.fill(matched,0,(maxVal-minVal+1),false);
        for (int i = 0; i < nU; i++) {
            in[i].add(match[unbound[i]] - minVal + nU);
            out[match[unbound[i]] - minVal + nU].add(i);
            matched[match[unbound[i]] - minVal] = true;
        }
        for (int i = 0; i < nU; i++) {
            for (int v = x[unbound[i]].getMin(); v <= x[unbound[i]].getMax(); v++) {
                if (x[unbound[i]].contains(v) && match[unbound[i]] != v) {
                    in[v - minVal + nU].add(i);
                    out[i].add(v - minVal + nU);
                }
            }
        }
        for (int v = minVal; v <= maxVal; v++) {
            if (!matched[v - minVal]) {
                in[sink].add(v - minVal + nU);
                out[v - minVal + nU].add(sink);
            } else {
                in[v - minVal + nU].add(sink);
                out[sink].add(v - minVal + nU);
            }
        }
    }


    @Override
    public void propagate() throws InconsistencyException {
        int size = maximumMatching.compute(match);
        if (size < x.length) {
            throw new InconsistencyException();
        }
        forwardChecking();
        updateRange();
        updateGraph();
        int[] scc = GraphUtil.stronglyConnectedComponents(g);
        int nU = nUnBound.getValue();
        for (int i = 0; i < nU; i++) {
            for (int v = minVal; v <= maxVal; v++) {
                if (match[unbound[i]] != v && scc[i] != scc[v - minVal + nU]) {
                    x[unbound[i]].remove(v);
                }
            }
        }
    }
}
