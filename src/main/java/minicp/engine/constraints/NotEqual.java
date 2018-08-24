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
import minicp.engine.core.IntVar;

public class NotEqual extends AbstractConstraint {
    private final IntVar x, y;
    private final int c;

    public NotEqual(IntVar x, IntVar y, int c) { // x != y + c
        super(x.getSolver());
        this.x = x;
        this.y = y;
        this.c = c;
    }

    public NotEqual(IntVar x, IntVar y) { // x != y
        this(x, y, 0);
    }

    @Override
    public void post() {
        if (y.isBound())
            x.remove(y.min() + c);
        else if (x.isBound())
            y.remove(x.min() - c);
        else {
            x.propagateOnBind(this);
            y.propagateOnBind(this);
        }
    }

    @Override
    public void propagate() {
        if (y.isBound())
            x.remove(y.min() + c);
        else y.remove(x.min() - c);
        setActive(false);
    }
}
