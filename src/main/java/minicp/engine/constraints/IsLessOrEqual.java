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

public class IsLessOrEqual extends Constraint { // b <=> x <= c

    private final BoolVar b;
    private final IntVar x;
    private final int c;

    public IsLessOrEqual(BoolVar b, IntVar x, int c) {
        super(x.getSolver());
        this.b = b;
        this.x = x;
        this.c = c;
    }

    @Override
    public void post() throws InconsistencyException {
        if (b.isTrue()) {
            x.removeAbove(c);
        } else if (b.isFalse()) {
            x.removeBelow(c+1);
        } else if (x.getMax() <= c) {
            b.assign(1);
        } else if (x.getMin() > c) {
            b.assign(0);
        } else {
            b.whenBind(() -> {
                // should deactivate the constraint as it is entailed
                if (b.isTrue()) {
                    x.removeAbove(c);

                } else {
                    x.removeBelow(c+1);
                }
            });
            x.whenBoundsChange(() -> {
                if (x.getMax() <= c) {
                    // should deactivate the constraint as it is entailed
                    b.assign(1);
                } else if (x.getMin() > c) {
                    // should deactivate the constraint as it is entailed
                    b.assign(0);
                }
            });
        }
    }
}