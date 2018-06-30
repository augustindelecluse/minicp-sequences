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

import minicp.engine.core.BasicConstraint;
import minicp.engine.core.IntVar;

public class LessOrEqual extends BasicConstraint { // x <= y
    private final IntVar x;
    private final IntVar y;

    public LessOrEqual(IntVar x, IntVar y) {
        super(x.getSolver());
        this.x = x;
        this.y = y;
    }
    @Override
    public void post()  {
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        propagate();
    }
    @Override
    public void propagate()  {
        x.removeAbove(y.getMax());
        y.removeBelow(x.getMin());
        if (x.getMax() <= y.getMin()) 
            cp.deactivate(this);        
    }
}
