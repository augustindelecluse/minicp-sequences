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

import minicp.engine.core.*;

import static minicp.cp.Factory.*;

public class IsLessOrEqualVar extends AbstractConstraint { // b <=> x <= y

    private final BoolVar b;
    private final IntVar x;
    private final IntVar y;

    private final Constraint lEqC;
    private final Constraint grC;

    public IsLessOrEqualVar(BoolVar b, IntVar x, IntVar y) {
        super(x.getSolver());
        this.b = b;
        this.x = x;
        this.y = y;
        lEqC = lessOrEqual(x,y);
        grC = lessOrEqual(plus(y,1),x);
    }

    @Override
    public void post()  {
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        b.propagateOnBind(this);
        propagate();
        // TODO
    }

    @Override
    public void propagate()  {
        if (b.isTrue()) {
            cp.post(lEqC,false);
            setActive(false);
        } else if (b.isFalse()) {
            cp.post(grC,false);
            setActive(false);
        } else {
            if (x.getMax() <= y.getMin()) {
                b.assign(1);
                setActive(false);
            } else if (x.getMin() > y.getMax()) {
                b.assign(0);
                setActive(false);
            }
        }
    }
}
