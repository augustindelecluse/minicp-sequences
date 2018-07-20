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

import static minicp.cp.Factory.*;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.state.StateInt;

public class Circuit extends AbstractConstraint {

    private final IntVar [] x;
    private final StateInt[] dest;
    private final StateInt[] orig;
    private final StateInt[] lengthToDest;

    /**
     * x represents an Hamiltonian circuit on the cities {0..x.length-1}
     * where x[i] is the city visited after city i
     * @param x
     */
    public Circuit(IntVar [] x) {
        super(x[0].getSolver());
        assert (x.length > 0);
        this.x = x;
        dest = new StateInt[x.length];
        orig = new StateInt[x.length];
        lengthToDest = new StateInt[x.length];
        for (int i = 0; i < x.length; i++) {
            dest[i] = cp.getStateManager().makeStateInt(i);
            orig[i] = cp.getStateManager().makeStateInt(i);
            lengthToDest[i] = cp.getStateManager().makeStateInt(0);
        }
    }


    @Override
    public void post()  {
        cp.post(allDifferent(x));
        if (x.length == 1) {
            x[0].assign(0);
            return;
        }
        for (int i = 0; i < x.length; i++) {
            x[i].remove(i);
        }
        for (int i = 0; i < x.length; i++) {
            if (x[i].isBound()) bind(i);
            else {
                final int fi = i;
                x[i].whenBind(() -> bind(fi));
            }
        }
    }


    private void bind(int i) {
        int j = x[i].getMin();
        int orig_i = orig[i].getValue();
        int dest_j = dest[j].getValue();
        // orig[i] *-> i -> j *-> dest[j]
        dest[orig_i].setValue(dest_j);
        orig[dest_j].setValue(orig_i);
        int length = lengthToDest[orig_i].getValue() +
                lengthToDest[j].getValue() + 1;
        lengthToDest[orig_i].setValue(length);

        if (length < x.length - 1) {
            // avoid inner loops
            x[dest_j].remove(orig_i); // avoid inner loops
        }

    }
}
