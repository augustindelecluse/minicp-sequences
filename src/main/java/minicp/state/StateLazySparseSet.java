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

package minicp.state;

public class StateLazySparseSet {
    private StateManager     sm;
    private StateSparseSet lazy;

    private StateInt min;
    private StateInt max;
    private int n;
    private int ofs;

    /**
     * Creates a StateSparseSet containing the elements {ofs,...,ofs + n - 1}.
     * @param sm
     * @param n
     * @param ofs
     */

    public StateLazySparseSet(StateManager sm, int n, int ofs) {
        this.sm = sm;
        this.lazy = null;
        this.n = n;
        this.ofs = ofs;
        min = sm.makeStateInt(0);
        max = sm.makeStateInt(n-1);
    }
    /**
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        return lazy==null ? min.getValue() > max.getValue() : lazy.isEmpty();
    }
    /**
     * @return the size of the set
     */
    public int getSize() {
        return lazy==null ? max.getValue() - min.getValue() + 1 : lazy.getSize();
    }
    /**
     * @return the minimum value in the set
     */
    public int getMin() {
        if (lazy==null) {
            return min.getValue();
        } else return lazy.getMin();
    }
    /**
     * @return the maximum value in the set
     */
    public int getMax() {
        if (lazy==null)
            return max.getValue();
        else return lazy.getMax();
    }
    /**
     * Check if the value val is in the set
     * @param val the original value to check.
     * @return true <-> (val-ofs) IN S
     */
    public boolean contains(int val) {
        if (lazy==null)
            return min.getValue() <= val && val <= max.getValue();
        else return lazy.contains(val);
    }
    /**
     * set the first values of <code>dest</code> to the ones
     * present in the set
     * @param dest, an array large enough dest.length >= getSize()
     * @return the size of the set
     */
    public int fillArray(int [] dest) {
        if (lazy==null) {
            int s = getSize();
            int from = getMin();
            for(int i=0;i < s;i++)
                dest[i] = from+i;
            return s;
        } else return lazy.fillArray(dest);
    }

    /**
     * Remove val from the set
     * @param val
     * @return true if val was in the set, false otherwise
     */
    public boolean remove(int val) {
        if (lazy==null) {
            if (val < min.getValue() || val > max.getValue())
                return false;
            if (val == min.getValue()) {
                min.increment();
                return true;
            } else if (val == max.getValue()) {
                max.decrement();
                return true;
            } else {  // punching a hole!
                StateSparseSet lazySet = new StateSparseSet(sm,n,ofs);
                lazySet.removeAbove(getMax());
                lazySet.removeBelow(getMin());
                boolean rv = lazySet.remove(val);
                lazy = lazySet;
                return rv;
            }
        } else return lazy.remove(val);
    }
    /**
     * Removes all the element from the set except v
     * @param v is an element in the set
     */
    public void removeAllBut(int v) {
        if (lazy==null) {
            min.setValue(v);
            max.setValue(v);
        } else
            lazy.removeAllBut(v);
    }
    /**
     * Remove all the values in the set
     */
    public void removeAll() {
        if (lazy==null)
            min.setValue(max.getValue()+1);
        else lazy.removeAll();
    }
    /**
     * Remove all the values < value in the set
     * @param value
     */
    public void removeBelow(int value) {
        if (lazy==null)
            min.setValue(value);
        else
            lazy.removeBelow(value);
    }

    /**
     * Remove all the values > value in the set
     */
    public void removeAbove(int value) {
        if (lazy==null)
            max.setValue(value);
        else lazy.removeAbove(value);
    }

    @Override public String toString() {
        if (lazy == null) {
            StringBuilder b = new StringBuilder();
            b.append("{").append(getMin());
            b.append("..").append(getMax()).append("}");
            return b.toString();
        } else {
            return lazy.toString();
        }
    }
}