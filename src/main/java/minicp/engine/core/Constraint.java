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
import minicp.reversible.RevBool;
import minicp.util.InconsistencyException;

public abstract class Constraint {

    protected boolean scheduled = false;
    protected final Solver cp;
    protected final RevBool active;

    public Constraint(Solver cp) {
        this.cp = cp;
        active = cp.getTrail().makeRevBool(true);
    }

    public boolean isActive() {
        return active.getValue();
    }

    public void deactivate() {
        active.setValue(false);
    }

    public abstract void post() throws InconsistencyException;
    public void propagate() throws InconsistencyException {}

    public void schedule() {
        cp.schedule(this);
    }
}
