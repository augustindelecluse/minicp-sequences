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

package minicp.reversible;

import minicp.util.NotImplementedException;

import java.util.NoSuchElementException;

public class ReversibleSparseSet {

    private int [] values;
    private int [] indexes;
    private ReversibleInt size;
    private ReversibleInt min;
    private ReversibleInt max;
    private int n;

    /**
     * Creates a ReversibleSparseSet containing the elements {0,...,n-1}.
     * @param rs
     * @param n > 0
     */
    public ReversibleSparseSet(Trail rs, int n) {
        this.n = n;
        size = new ReversibleInt(rs,n);
        min = new ReversibleInt(rs,0);
        max = new ReversibleInt(rs,n-1);
        values = new int [n];
        indexes = new int [n];
        for (int i = 0; i < n; i++) {
            values[i] = i;
            indexes[i] = i;
        }
    }


    private void exchangePositions(int val1, int val2) {
        assert(checkVal(val1));
        assert(checkVal(val2));
        int v1 = val1;
        int v2 = val2;
        int i1 = indexes[v1];
        int i2 = indexes[v2];
        values[i1] = v2;
        values[i2] = v1;
        indexes[v1] = i2;
        indexes[v2] = i1;
    }

    private boolean checkVal(int val) {
        assert(val <= values.length-1);
        return true;
    }

    /**
     * @return an array representation of values present in the set
     */
    public int[] toArray()  {
        int [] res = new int[getSize()];
        fillArray(res);
        return res;
    }


    /**
     * set the first values of <code>dest</code> to the ones
     * prsent in the set
     * @param dest, an array large enough dest.length >= getSize()
     * @return the size of the set
     */
    public int fillArray(int [] dest) {
        int s = size.getValue();
        System.arraycopy(values, 0, dest, 0, s);
        return s;
    }

    /**
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        return getSize() == 0;
    }

    /**
     * @return the size of the set
     */
    public int getSize() { return size.getValue(); }

    /**
     * @return the minimum value in the set
     */
    public int getMin() {
        if (isEmpty()) throw new NoSuchElementException();
        return min.getValue();
    }

    /**
     * @return the maximum value in the set
     */
    public int getMax() {
        if (isEmpty()) throw new NoSuchElementException();
        else return max.getValue();
    }

    private void updateBoundsValRemoved(int val) {
        updateMaxValRemoved(val);
        updateMinValRemoved(val);
    }

    private void updateMaxValRemoved(int val) {
        if (!isEmpty() && max.getValue() == val) {
            assert(!contains(val));
            //the maximum was removed, search the new one
            for (int v = val-1; v >= min.getValue(); v--) {
                if (contains(v)) {
                    max.setValue(v);
                    return;
                }
            }
        }
    }

    private void updateMinValRemoved(int val) {
        if (!isEmpty() && min.getValue() == val) {
            assert(!contains(val));
            //the minimum was removed, search the new one
            for (int v = val+1; v <= max.getValue(); v++) {
                if (contains(v)) {
                    min.setValue(v);
                    return;
                }
            }
        }
    }

    /**
     * Remove val from the set
     * @param val
     * @return true if val was in the set, false otherwise
     */
    public boolean remove(int val) {
        assert(checkVal(val));
        if (!contains(val)) return false; //the value has already been removed
        int s = getSize();
        exchangePositions(val, values[s-1]);
        size.decrement();
        updateBoundsValRemoved(val);
        return true;
    }

    /**
     * Check is the val is in the set
     * @param val
     * @return
     */
    public boolean contains(int val) {
        if (val < 0 || val >= n) return false;
        return indexes[val] < getSize();
    }

    /**
     * Removes all the element from the set except v
     * @param v is an element in the set
     */
    public void removeAllBut(int v) {
        // we only have to put in first position this value and set the size to 1
        assert(checkVal(v));
        assert(contains(v));
        int val = values[0];
        int index = indexes[v];
        indexes[v] = 0;
        values[0] = v;
        indexes[val] = index;
        values[index] = val;
        min.setValue(v);
        max.setValue(v);
        size.setValue(1);
    }

    /**
     * Remove all the values in the set
     */
    public void removeAll() {
        size.setValue(0);
    }

    /**
     * Remove all the values < value in the set
     * @param value
     * @return the new minimum
     */
    public int removeBelow(int value) {
        if (getMax() < value) {
            removeAll();
            return Integer.MAX_VALUE;
        } else {
            for (int v = getMin(); v < value; v++) {
                remove(v);
            }
            return getMin();
        }
    }

    /**
     * Remove all the values > value in the set
     * @param value
     * @return the new maximum
     */
    public int removeAbove(int value) {
        int max = getMax();
        if (getMin() > value) {
            removeAll();
            return Integer.MIN_VALUE;
        }
        else {
            for (int v = value + 1; v <= max; v++) {
                remove(v);
            }
            return getMax();
        }
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("{");
        for (int i = 0; i < getSize()-1; i++) {
            b.append(values[i]);
            b.append(',');
        }
        if (getSize() > 0) b.append(values[getSize()-1]);
        b.append("}");
        return b.toString();
    }



}

