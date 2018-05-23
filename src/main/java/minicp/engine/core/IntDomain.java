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

import minicp.util.InconsistencyException;


public abstract class IntDomain {

    public abstract int getMin();

    public abstract  int getMax();

    public abstract  int getSize();

    public abstract  boolean contains(int v);

    public abstract  boolean isBound();

    public abstract  void remove(int v, DomainListener x);

    public abstract  void removeAllBut(int v, DomainListener x);

    public abstract  void removeBelow(int value, DomainListener x);

    public abstract  void removeAbove(int value, DomainListener x);

    /**
     * Copy the values of the domain
     * @param dest, an array large enough dest.length >= getSize()
     * @return the size of the domain and dest[0,...,getSize-1] contains
     *         the values in the domain in an arbitrary order
     */
    public abstract int fillArray(int [] dest);

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("{");
        for (int i = getMin(); i <= getMax() - 1; i++) {
            if (contains((i))) {
                b.append(i);
                b.append(',');
            }
        }
        if (getSize() > 0) b.append(getMax());
        b.append("}");
        return b.toString();
    }
}
