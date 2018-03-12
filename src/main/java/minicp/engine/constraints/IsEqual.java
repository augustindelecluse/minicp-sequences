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
import minicp.util.InconsistencyException;

public class IsEqual extends Constraint { // b <=> x == c

    private final BoolVar b;
    private final IntVar x;
    private final int c;

    public IsEqual(BoolVar b, IntVar x, int c) {
        super(x.getSolver());
        this.b = b;
        this.x = x;
        this.c = c;
    }

    @Override
    public void post() throws InconsistencyException {
        if (b.isTrue()) {
            x.assign(c);
        } else if (b.isFalse()) {
            x.remove(c);
        } else if (x.isBound()) {
            b.assign(x.getMin() == c);
        } else if (!x.contains(c)) {
            b.assign(0);
        } else {
            b.whenBind(() -> {
                if (b.isTrue()) x.assign(c);
                else {
                    // should deactivate the constraint as it is entailed
                    x.remove(c);
                }
            });
            x.whenBind(() ->
                b.assign(x.getMin() == c)
            );
            x.whenDomainChange(() -> {
                if (!x.contains(c))
                    b.assign(0);
            });
        }
    }
}
