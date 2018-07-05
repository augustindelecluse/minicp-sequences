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
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.BoolVar;
import minicp.engine.core.Constraint;
import minicp.engine.core.Solver;
import minicp.reversible.RevInt;

public class IsOr extends AbstractConstraint { // b <=> x1 or x2 or ... xn

    private final BoolVar b;
    private final BoolVar[] x;
    private final int n;

    private int[] unBounds;
    private RevInt nUnBounds;

    private final Or or;

    public IsOr(BoolVar b, BoolVar[] x) {
        super(b.getSolver());
        this.b = b;
        this.x = x;
        this.n = x.length;
        or = new Or(x);

        nUnBounds = Factory.makeRevInt(cp,n);
        unBounds = new int[n];
        for (int i = 0; i < n; i++) {
            unBounds[i] = i;
        }
    }

    @Override
    public void post()  {
        b.propagateOnBind(this);
        for (BoolVar xi : x) {
            xi.propagateOnBind(this);
        }
    }

    @Override
    public void propagate() {
        if (b.isTrue()) {
            setActive(false);
            cp.post(or, false);
        } else if (b.isFalse()) {
            for (BoolVar xi : x) {
                xi.assign(false);
            }
            setActive(false);
        } else {
            int nU = nUnBounds.getValue();
            for (int i = nU - 1; i >= 0; i--) {
                int idx = unBounds[i];
                BoolVar y = x[idx];
                if (y.isBound()) {
                    if (y.isTrue()) {
                        b.assign(true);
                        setActive(false);
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
                setActive(false);
            }
            nUnBounds.setValue(nU);
        }

    }
}
