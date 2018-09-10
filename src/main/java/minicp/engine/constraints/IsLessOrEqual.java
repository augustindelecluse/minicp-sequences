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
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

/**
 * Reified less or equal constraint.
 */
public class IsLessOrEqual extends AbstractConstraint { // b <=> x <= c

    private final BoolVar b;
    private final IntVar x;
    private final int c;

    /**
     * Creates a constraint that
     * link a boolean variable representing
     * whether one variable is less or equal to the given constant.
     * @param b a boolean variable that is true if and only if
     *         x takes a value less or equal to c
     * @param x the variable
     * @param c the constant
     * @see minicp.cp.Factory#isLessOrEqual(IntVar, int)
     */
    public IsLessOrEqual(BoolVar b, IntVar x, int c) {
        super(b.getSolver());
        this.b = b;
        this.x = x;
        this.c = c;
    }

    @Override
    public void post() {
        // TODO
        // STUDENT throw new NotImplementedException("IsLessOrEqual");
        // BEGIN STRIP
        if (b.isTrue()) {
            x.removeAbove(c);
        } else if (b.isFalse()) {
            x.removeBelow(c + 1);
        } else if (x.max() <= c) {
            b.assign(1);
        } else if (x.min() > c) {
            b.assign(0);
        } else {
            b.whenBind(() -> {
                // should deactivate the constraint as it is entailed
                if (b.isTrue()) {
                    x.removeAbove(c);

                } else {
                    x.removeBelow(c + 1);
                }
            });
            x.whenBoundsChange(() -> {
                if (x.max() <= c) {
                    // should deactivate the constraint as it is entailed
                    b.assign(1);
                } else if (x.min() > c) {
                    // should deactivate the constraint as it is entailed
                    b.assign(0);
                }
            });
        }
        // END STRIP
    }
}
