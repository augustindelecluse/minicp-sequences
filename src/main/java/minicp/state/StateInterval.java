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

public class StateInterval {
    private StateManager     sm;

    private StateInt min;
    private StateInt max;


    public StateInterval(StateManager sm, int min, int max) {
        this.min = sm.makeStateInt(min);
        this.max = sm.makeStateInt(max);
    }

    /**
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        return min.getValue() > max.getValue();
    }
    /**
     * @return the size of the set
     */
    public int getSize() {
        return Math.max(max.getValue() - min.getValue() + 1,0);
    }
    /**
     * @return the minimum value in the set
     */
    public int getMin() {
        return min.getValue();
    }
    /**
     * @return the maximum value in the set
     */
    public int getMax() {
        return max.getValue();
    }
    /**
     * Check if the value val is in the set
     * @param val the original value to check.
     * @return true <-> (val-ofs) IN S
     */
    public boolean contains(int val) {
        return min.getValue() <= val && val <= max.getValue();
    }
    /**
     * set the first values of <code>dest</code> to the ones
     * present in the set
     * @param dest, an array large enough dest.length >= getSize()
     * @return the size of the set
     */
    public int fillArray(int[] dest) {
        int s = getSize();
        int from = getMin();
        for (int i = 0; i < s; i++)
            dest[i] = from + i;
        return s;
    }


    /**
     * Removes all the element from the set except v
     * @param v is an element in the set
     */
    public void removeAllBut(int v) {
        assert (contains(v));
        min.setValue(v);
        max.setValue(v);

    }
    /**
     * Remove all the values in the set
     */
    public void removeAll() {
        min.setValue(max.getValue() + 1);
    }
    /**
     * Remove all the values < value in the set
     * @param value
     */
    public void removeBelow(int value) {
        min.setValue(value);
    }

    /**
     * Remove all the values > value in the set
     */
    public void removeAbove(int value) {
        max.setValue(value);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("{").append(getMin());
        b.append("..").append(getMax()).append("}");
        return b.toString();
    }
}