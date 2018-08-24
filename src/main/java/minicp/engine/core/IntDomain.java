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

public interface IntDomain {

    int getMin();

    int getMax();

    int getSize();

    boolean contains(int v);

    boolean isBound();

    void remove(int v, DomainListener x);

    void removeAllBut(int v, DomainListener x);

    void removeBelow(int value, DomainListener x);

    void removeAbove(int value, DomainListener x);

    /**
     * Copy the values of the domain
     *
     * @param dest, an array large enough dest.length >= size()
     * @return the size of the domain and dest[0,...,size-1] contains
     * the values in the domain in an arbitrary order
     */
    int fillArray(int[] dest);

    String toString();
}
