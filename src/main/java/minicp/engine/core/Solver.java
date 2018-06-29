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

public interface Solver extends StateManager {
    void post(Constraint c);
    void schedule(Constraint c);
    void post(Constraint c, boolean enforceFixPoint);
    void fixPoint();
    // ugly
    void push();
    void pop();
    void post(BoolVar b);
}

