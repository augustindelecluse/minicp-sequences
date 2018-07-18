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

package minicp.search;

import minicp.engine.core.Constraint;
import minicp.reversible.TrailImpl;
import minicp.util.Procedure;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class AbstractSearchNode implements SearchNode {

    private TrailImpl trail = new TrailImpl();
    private List<Procedure> solutionListeners = new LinkedList<Procedure>();
    private List<Procedure> failureListeners = new LinkedList<Procedure>();


    public void notifySolution() {
        solutionListeners.forEach(s -> s.call());
    }

    @Override
    public void onSolution(Procedure listener) {
        solutionListeners.add(listener);
    }

    public void notifyFailure() {
        failureListeners.forEach(s -> s.call());
    }

    @Override
    public void onFailure(Procedure listener) {
        failureListeners.add(listener);
    }

    @Override
    public TrailImpl getTrail() {
        return trail;
    }
}
