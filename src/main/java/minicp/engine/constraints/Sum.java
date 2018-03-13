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
import minicp.engine.core.IntVarImpl;
import minicp.reversible.ReversibleInt;
import minicp.util.InconsistencyException;

import java.util.Arrays;

public class Sum extends Constraint {

    private  int[] unBounds;
    private ReversibleInt nUnBounds;
    private ReversibleInt sumBounds;
    private IntVar [] x;
    private int n;

    public Sum(IntVar [] x, IntVar y) {
        this(Arrays.copyOf(x, x.length + 1));
        this.x[x.length] = Factory.minus(y);
    }


    public Sum(IntVar [] x, int y) {
        this(Arrays.copyOf(x, x.length + 1));
        this.x[x.length] = new IntVarImpl(cp,-y,-y);
    }

    /**
     * Create a sum constraint that holds iff
     * x[0]+x[1]+...+x[x.length-1] = 0
     * @param x
     */
    public Sum(IntVar [] x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        nUnBounds = new ReversibleInt(cp.getTrail(),n);
        sumBounds = new ReversibleInt(cp.getTrail(),0);
        unBounds = new int[n];
        for (int i = 0; i < n; i++) {
            unBounds[i] = i;
        }
    }

    @Override
    public void post() throws InconsistencyException {


        for (IntVar var: x) {
            var.propagateOnBoundChange(this);
            //var.propagateOnDomainChange(this);
        }
        propagate();
    }

    @Override
    public void propagate() throws InconsistencyException {
        // Filter the unbound vars and update the partial sum
        int nU = nUnBounds.getValue();
        int partialSum = sumBounds.getValue();
        for (int i = nU - 1; i >= 0; i--) {
            int idx = unBounds[i];
            IntVar y = x[idx];
            if (y.isBound()) {
                // Update partial sum
                partialSum += y.getMin();
                // Swap the variable
                unBounds[i] = unBounds[nU - 1];
                unBounds[nU - 1] = idx;
                nU--;
            }
        }
        sumBounds.setValue(partialSum);
        nUnBounds.setValue(nU);

        int sumMax = partialSum;
        int sumMin = partialSum;
        for (int i = nU - 1; i >= 0; i--) {
            int idx = unBounds[i];
            sumMax += x[idx].getMax();
            sumMin += x[idx].getMin();
        }
        if (sumMin > 0 || sumMax < 0)
            throw new InconsistencyException();
        for (int i = nU - 1; i >= 0; i--) {
            int idx = unBounds[i];
            x[idx].removeAbove(-(sumMin-x[idx].getMin()));
            x[idx].removeBelow(-(sumMax-x[idx].getMax()));
        }

    }

}
