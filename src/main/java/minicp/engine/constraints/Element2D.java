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

import minicp.cp.Factory;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.reversible.RevInt;
import minicp.util.InconsistencyException;

import java.util.ArrayList;
import java.util.Collections;

public class Element2D implements Constraint {


    private final int[][] T;
    private final Solver cp;
    private final IntVar x, y, z;
    private int n, m;
    private final RevInt[] nRowsSup;
    private final RevInt[] nColsSup;

    private final RevInt low;
    private final RevInt up;
    private final ArrayList<Tripple> xyz;

    private class Tripple implements Comparable<Tripple> {
        protected final int x,y,z;

        private Tripple(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int compareTo(Tripple t) {
            return z - t.z;
        }
    }

    /**
     * T[x][y] = z
     * @param T
     * @param x
     * @param y
     * @param z
     */
    public Element2D(int[][] T, IntVar x, IntVar y, IntVar z) {
        this.cp = x.getSolver();
        this.T = T;
        this.x = x;
        this.y = y;
        this.z = z;
        n = T.length;
        m = T[0].length;

        this.xyz = new ArrayList<Tripple>();
        for (int i = 0; i < T.length; i++) {
            assert (T[i].length == m); // n x m matrix expected
            for (int j = 0; j < T[i].length; j++) {
                xyz.add(new Tripple(i,j,T[i][j]));
            }
        }
        Collections.sort(xyz);
        low = Factory.makeRevInt(cp,0);
        up = Factory.makeRevInt(cp,xyz.size()-1);

        nColsSup = new RevInt[n];
        nRowsSup = new RevInt[m];
        for (int i = 0; i < n; i++) {
            nColsSup[i] = Factory.makeRevInt(cp,m);
        }
        for (int j = 0; j < m; j++) {
            nRowsSup[j] = Factory.makeRevInt(cp,n);
        }
    }

    @Override
    public void post()  {
        x.removeBelow(0);
        x.removeAbove(n-1);
        y.removeBelow(0);
        y.removeAbove(m-1);

        x.propagateOnDomainChange(this);
        y.propagateOnDomainChange(this);
        z.propagateOnBoundChange(this);
        propagate();
    }

    private void updateSupports(int lostPos)  {
        if (nColsSup[xyz.get(lostPos).x].decrement() == 0) {
            x.remove(xyz.get(lostPos).x);
        }
        if (nRowsSup[xyz.get(lostPos).y].decrement() == 0) {
            y.remove(xyz.get(lostPos).y);
        }
    }

    @Override
    public void propagate() {
        int l = low.getValue();
        int u = up.getValue();
        int zMin = z.getMin();
        int zMax = z.getMax();

        while (xyz.get(l).z < zMin || !x.contains(xyz.get(l).x) || !y.contains(xyz.get(l).y)) {
            updateSupports(l);
            l++;
            if (l > u) throw new InconsistencyException();
        }
        while (xyz.get(u).z > zMax || !x.contains(xyz.get(u).x) || !y.contains(xyz.get(u).y)) {
            updateSupports(u);
            u--;
            if (l > u) throw new InconsistencyException();
        }
        z.removeBelow(xyz.get(l).z);
        z.removeAbove(xyz.get(u).z);
        low.setValue(l);
        up.setValue(u);
    }

}
