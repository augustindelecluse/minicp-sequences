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
import minicp.reversible.ReversibleSparseSet;
import minicp.util.InconsistencyException;
import static minicp.util.InconsistencyException.INCONSISTENCY;


public class SparseSetDomain extends IntDomain {
    private ReversibleSparseSet domain;
    private int offset;

    public int fillArray(int [] dest) {
        int s = domain.fillArray(dest);
        for (int i = 0; i < s; i++) {
            dest[i] += offset;
        }
        return s;
    }

    public SparseSetDomain(StateManager sm, int min, int max) {
        offset = min;
        domain = new ReversibleSparseSet(sm, max-min+1);
    }

    public int getMin() {
        return domain.getMin() + offset;
    }

    public int getMax() {
        return domain.getMax() + offset;
    }

    public int getSize() {
        return domain.getSize();
    }

    public boolean contains(int v) {
        return domain.contains(v - offset);
    }

    public boolean isBound() {
        return domain.getSize() == 1;
    }

    public void remove(int v, DomainListener x) {
        if (domain.contains(v - offset)) {
            boolean maxChanged = getMax() == v;
            boolean minChanged = getMin() == v;
            domain.remove(v - offset);
            if (domain.getSize() == 0) return;
            x.change(domain.getSize());
            if (maxChanged) x.removeAbove(domain.getSize());
            if (minChanged) x.removeBelow(domain.getSize());
            if (domain.getSize() == 1) x.bind();
        }
    }

    public void removeAllBut(int v, DomainListener x) {
        if (domain.contains(v - offset)) {
            if (domain.getSize() != 1) {
                boolean maxChanged = getMax() != v;
                boolean minChanged = getMin() != v;
                domain.removeAllBut(v - offset);
                x.bind();
                x.change(domain.getSize());
                if (maxChanged) x.removeAbove(domain.getSize());
                if (minChanged) x.removeBelow(domain.getSize());
            }
        }
        else {
            domain.removeAll();
        }
    }

    public void removeBelow(int value, DomainListener x) {
        if (domain.getMin() + offset < value) {
            domain.removeBelow(value - offset);
            x.removeBelow(domain.getSize());
            x.change(domain.getSize());
            if (domain.getSize() == 1) x.bind();
        }
    }

    public void removeAbove(int value, DomainListener x) {
        if (domain.getMax() + offset > value) {
            domain.removeAbove(value - offset);
            x.removeAbove(domain.getSize());
            x.change(domain.getSize());
            if (domain.getSize() == 1) x.bind();
        }
    }
}
