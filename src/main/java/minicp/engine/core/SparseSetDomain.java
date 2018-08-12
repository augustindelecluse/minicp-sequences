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

import minicp.state.StateManager;
import minicp.state.StateSparseSet;

import java.util.stream.IntStream;
import java.util.stream.Stream;


public class SparseSetDomain implements IntDomain {
    private StateSparseSet domain;

    public int fillArray(int[] dest) {
        return domain.fillArray(dest);
    }

    public SparseSetDomain(StateManager sm, int min, int max) {
        domain = new StateSparseSet(sm, max - min + 1,min);
    }

    public int getMin() {
        return domain.getMin();
    }

    public int getMax() {
        return domain.getMax();
    }

    public int getSize() {
        return domain.getSize();
    }

    public boolean contains(int v) {
        return domain.contains(v);
    }

    public boolean isBound() {
        return domain.getSize() == 1;
    }

    public void remove(int v, DomainListener x) {
        if (domain.contains(v)) {
            boolean maxChanged = getMax() == v;
            boolean minChanged = getMin() == v;
            domain.remove(v);
            if (domain.getSize() == 0)
                x.empty();
            x.change();
            if (maxChanged) x.removeAbove();
            if (minChanged) x.removeBelow();
            if (domain.getSize() == 1) x.bind();
        }
    }

    public void removeAllBut(int v, DomainListener x) {
        if (domain.contains(v)) {
            if (domain.getSize() != 1) {
                boolean maxChanged = getMax() != v;
                boolean minChanged = getMin() != v;
                domain.removeAllBut(v);
                if (domain.getSize() == 0)
                    x.empty();
                x.bind();
                x.change();
                if (maxChanged) x.removeAbove();
                if (minChanged) x.removeBelow();
            }
        } else {
            domain.removeAll();
            x.empty();
        }
    }

    public void removeBelow(int value, DomainListener x) {
        if (domain.getMin() < value) {
            domain.removeBelow(value);
            switch (domain.getSize()) {
                case 0: x.empty();break;
                case 1: x.bind();
                default:
                    x.removeBelow();
                    x.change();
                    break;
            }
        }
    }

    public void removeAbove(int value, DomainListener x) {
        if (domain.getMax() > value) {
            domain.removeAbove(value);
            switch(domain.getSize()) {
                case 0: x.empty();break;
                case 1: x.bind();
                default:
                    x.removeAbove();
                    x.change();
                    break;
            }
        }
    }
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("{");
        for (int i = getMin(); i <= getMax() - 1; i++)
            if (contains((i)))
                b.append(i).append(',');
        if (getSize() > 0) b.append(getMax());
        b.append("}");
        return b.toString();
    }

}
