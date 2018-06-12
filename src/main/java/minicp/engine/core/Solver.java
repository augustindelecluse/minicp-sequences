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

import minicp.reversible.StateManager;
import minicp.reversible.Trail;
import minicp.reversible.TrailImpl;
import minicp.util.InconsistencyException;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Stack;
import java.util.Vector;

public class Solver implements StateManager {

    private Trail trail = new TrailImpl();
    private Queue<Constraint> propagationQueue = new ArrayDeque<>();
    private Vector<IntVar>  vars = new Vector<>(2);
    public void registerVar(IntVar x) {
        vars.add(x);
    }

    public void push() { trail.push();}
    public void pop()  { trail.pop();}

    public Trail getTrail() { return trail;}

    public void schedule(Constraint c) {
        if (!c.scheduled && c.isActive()) {
            c.scheduled = true;
            propagationQueue.add(c);
        }
    }

    public void fixPoint() {
        try {
            while (!propagationQueue.isEmpty()) {
                Constraint c = propagationQueue.remove();
                c.scheduled = false;
                if (c.isActive()) {
                    c.propagate();
                }
            }
        } catch (InconsistencyException e) {
            // empty the queue and unset the scheduled status
            while (propagationQueue.size() > 0)
                propagationQueue.remove().scheduled = false;
            throw new InconsistencyException();
        }
    }

    public void post(Constraint c) {
        post(c,true);
    }

    public void post(Constraint c, boolean enforceFixPoint)  {
        c.post();
        if (enforceFixPoint) fixPoint();
    }

    public void post(BoolVar b) {
        b.assign(true);
        fixPoint();
    }

}

