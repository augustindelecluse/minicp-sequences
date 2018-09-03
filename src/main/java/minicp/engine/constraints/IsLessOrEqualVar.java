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
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

import static minicp.cp.Factory.lessOrEqual;
import static minicp.cp.Factory.plus;

/**
 * Reified is less or equal constraint {@code b <=> x <= y}.
 */
public class IsLessOrEqualVar extends AbstractConstraint {

    private final BoolVar b;
    private final IntVar x;
    private final IntVar y;

    // STUDENT
    // BEGIN STRIP
    private final Constraint lEqC;
    private final Constraint grC;
    // END STRIP

    /**
     * Creates a reified is less or equal constraint {@code b <=> x <= y}.
     * @param b the truth value that will be set to true if {@code x <= y}, false otherwise
     * @param x left hand side of less or equal operator
     * @param y right hand side of less or equal operator
     */
    public IsLessOrEqualVar(BoolVar b, IntVar x, IntVar y) {
        super(x.getSolver());
        this.b = b;
        this.x = x;
        this.y = y;
        // STUDENT
        // BEGIN STRIP
        lEqC = lessOrEqual(x, y);
        grC = lessOrEqual(plus(y, 1), x);
        // END STRIP
    }

    @Override
    public void post() {
        // TODO
        // STUDENT throw new NotImplementedException();
        // BEGIN STRIP
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        b.propagateOnBind(this);
        propagate();
        // END STRIP
    }

    @Override
    public void propagate() {
        // TODO
        // STUDENT throw new NotImplementedException();
        // BEGIN STRIP
        if (b.isTrue()) {
            cp.post(lEqC, false);
            setActive(false);
        } else if (b.isFalse()) {
            cp.post(grC, false);
            setActive(false);
        } else {
            if (x.max() <= y.min()) {
                b.assign(1);
                setActive(false);
            } else if (x.min() > y.max()) {
                b.assign(0);
                setActive(false);
            }
        }
        // END STRIP
    }
}
