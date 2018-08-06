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
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.util.InconsistencyException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.*;

public class Element2D extends AbstractConstraint {
    private final int[][] T;
    private final IntVar x, y, z;
    private int n, m;
    private final StateInt[] nRowsSup;
    private final StateInt[] nColsSup;

    private final StateInt low;
    private final StateInt up;
    private final ArrayList<Triple> xyz;

    private class Triple implements Comparable<Triple> {
        protected final int x,y,z;
        private Triple(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        @Override public int compareTo(Triple t) {
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
        super(x.getSolver());
        this.T = T;
        this.x = x;
        this.y = y;
        this.z = z;
        n = T.length;
        m = T[0].length;
        this.xyz = new ArrayList<Triple>();
        for (int i = 0; i < T.length; i++) 
            for (int j = 0; j < T[i].length; j++) 
                xyz.add(new Triple(i,j,T[i][j]));                    
        Collections.sort(xyz);
        StateManager sm = cp.getStateManager();
        low = sm.makeStateInt(0);
        up  = sm.makeStateInt(xyz.size()-1);
        nColsSup = IntStream.range(0,n).mapToObj(i -> sm.makeStateInt(m)).toArray(StateInt[]::new);
        nRowsSup = IntStream.range(0,m).mapToObj(i -> sm.makeStateInt(n)).toArray(StateInt[]::new);
    }
    @Override public void post()  {
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
       if (nColsSup[xyz.get(lostPos).x].decrement() == 0) 
          x.remove(xyz.get(lostPos).x);        
       if (nRowsSup[xyz.get(lostPos).y].decrement() == 0) 
          y.remove(xyz.get(lostPos).y);        
    }
    @Override public void propagate() {
       int l    = low.getValue(),u = up.getValue();
       int zMin = z.getMin(),zMax  = z.getMax();

       while (xyz.get(l).z < zMin || !x.contains(xyz.get(l).x) || !y.contains(xyz.get(l).y)) {
          updateSupports(l++);
          if (l > u) throw new InconsistencyException();
       }
       while (xyz.get(u).z > zMax || !x.contains(xyz.get(u).x) || !y.contains(xyz.get(u).y)) {
          updateSupports(u--);
          if (l > u) throw new InconsistencyException();
       }
       z.removeBelow(xyz.get(l).z);
       z.removeAbove(xyz.get(u).z);
       low.setValue(l);
       up.setValue(u);
    }
}
