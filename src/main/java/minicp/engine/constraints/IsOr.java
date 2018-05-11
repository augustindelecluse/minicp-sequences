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

import minicp.engine.core.BoolVar;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.reversible.ReversibleInt;
import minicp.util.InconsistencyException;

public class IsOr extends Constraint { // b <=> x1 or x2 or ... xn

    private final BoolVar b;
    private final BoolVar[] x;
    private final int n;

    private int[] unBounds;
    private ReversibleInt nUnBounds;

    private final Or or;

    public IsOr(BoolVar b, BoolVar[] x) {
        super(b.getSolver());
        this.b = b;
        this.x = x;
        this.n = x.length;
        or = new Or(x);

        nUnBounds = new ReversibleInt(cp.getTrail(), n);
        unBounds = new int[n];
        for (int i = 0; i < n; i++) {
            unBounds[i] = i;
        }
    }

    @Override
    public void post() throws InconsistencyException {
        b.propagateOnBind(this);
        for (BoolVar xi : x) {
            xi.propagateOnBind(this);
        }
    }

    @Override
    public void propagate() throws InconsistencyException {
        if (b.isTrue()) {
            deactivate();
            cp.post(or, false);
        } else if (b.isFalse()) {
            for (BoolVar xi : x) {
                xi.assign(false);
            }
            deactivate();
        } else {
            int nU = nUnBounds.getValue();
            for (int i = nU - 1; i >= 0; i--) {
                int idx = unBounds[i];
                BoolVar y = x[idx];
                if (y.isBound()) {
                    if (y.isTrue()) {
                        b.assign(true);
                        deactivate();
                        return;
                    }
                    // Swap the variable
                    unBounds[i] = unBounds[nU - 1];
                    unBounds[nU - 1] = idx;
                    nU--;
                }
            }
            if (nU == 0) {
                b.assign(false);
                deactivate();
            }
            nUnBounds.setValue(nU);
        }

    }
}
