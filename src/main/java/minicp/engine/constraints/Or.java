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
import minicp.engine.core.BoolVar;
import minicp.state.StateInt;

import static minicp.util.InconsistencyException.*;

public class Or extends AbstractConstraint { // x1 or x2 or ... xn

    private final BoolVar [] x;
    private final int n;
    private StateInt wL ; // watched literal left
    private StateInt wR ; // watched literal right


    public Or(BoolVar [] x) {
        super(x[0].getSolver());
        this.x = x;
        this.n = x.length;
        wL = cp.getStateManager().makeStateInt(0);
        wR = cp.getStateManager().makeStateInt(n-1);
    }

    @Override
    public void post()  {
        propagate();
    }


    @Override
    public void propagate()  {
        // update watched literals
        int i = wL.getValue();
        while (i < n && x[i].isBound()) {
            if (x[i].isTrue()) {
                setActive(false);
                return;
            }
            i += 1;
        }
        wL.setValue(i);
        i = wR.getValue();
        while (i >= 0 && x[i].isBound() && i >= wL.getValue()) {
            if (x[i].isTrue()) {
                setActive(false);
                return;
            }
            i -= 1;
        }
        wR.setValue(i);

        if (wL.getValue() > wR.getValue()) {
            throw INCONSISTENCY;
        }
        else if (wL.getValue() == wR.getValue()) { // only one unassigned var
            x[wL.getValue()].assign(true);
            setActive(false);
        }
        else {
            assert(wL.getValue() != wR.getValue());
            assert(!x[wL.getValue()].isBound());
            assert(!x[wR.getValue()].isBound());
            x[wL.getValue()].propagateOnBind(this);
            x[wR.getValue()].propagateOnBind(this);
        }
    }
}
