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

package minicp.engine.core;
import minicp.cp.Factory;
import minicp.reversible.RevBool;

public abstract class BasicConstraint implements Constraint {

    protected boolean scheduled = false;
    protected final Solver cp;
    protected final RevBool active;

    public BasicConstraint(Solver cp) {
        this.cp = cp;
        active = Factory.makeRevBool(cp,true);
    }

    public void post()      {}
    public void propagate() {}

    public void deactivate() {
        active.setValue(false);
    }
    public void schedule() {
        if (!scheduled && active.getValue()) {
            scheduled = true;
            cp.schedule(this);
        }
    }
    public void discard() {
        scheduled = false;
    }
    public void process() {
        scheduled = false;
        if (active.getValue())
            propagate();
    }
}
