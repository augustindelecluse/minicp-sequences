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

    private StateSparseSet sparse;
    private StateInterval interval;


    private StateBool intervalRep;
    private boolean switched = false;

    private boolean isInterval() {
        return intervalRep.getValue();
    }

    /**
     * Creates a StateSparseSet containing the elements {ofs,...,ofs + n - 1}.
     * @param sm
     * @param n
     * @param ofs
     */

    public StateLazySparseSet(StateManager sm, int n, int ofs) {
        this.sm = sm;
        interval = new StateInterval(sm, ofs, ofs + n - 1);
        intervalRep = sm.makeStateBool(true);

        // optimization to avoid trashing with the creation of sparse rep
        sm.onRestore(() -> {
            if (switched && isInterval()) buildSparse();
        });
    }

    private void buildSparse() {
        sparse = new StateSparseSet(sm, getMax() - getMin() + 1, getMin());
        intervalRep.setValue(false);
        switched = true;
    }

    /**
     * @return true if the set is empty
     */
    public boolean isEmpty() {
        return isInterval() ? interval.isEmpty() : sparse.isEmpty();
    }
    /**
     * @return the size of the set
     */
    public int getSize() {
        return isInterval() ? interval.getSize() : sparse.getSize();
    }
    /**
     * @return the minimum value in the set
     */
    public int getMin() {
        if (isInterval()) {
            return interval.getMin();
        } else {
            return sparse.getMin();
        }
    }
    /**
     * @return the maximum value in the set
     */
    public int getMax() {
        if (isInterval()) {
            return interval.getMax();
        } else {
            return sparse.getMax();
        }
    }
    /**
     * Check if the value val is in the set
     * @param val the original value to check.
     * @return true <-> (val-ofs) IN S
     */
    public boolean contains(int val) {
        if (isInterval()) {
            return interval.contains(val);
        }
        else {
            return sparse.contains(val);
        }
    }
    /**
     * set the first values of <code>dest</code> to the ones
     * present in the set
     * @param dest, an array large enough dest.length >= getSize()
     * @return the size of the set
     */
    public int fillArray(int [] dest) {
        if (isInterval()) {
            int s = getSize();
            int from = getMin();
            for(int i=0;i < s;i++)
                dest[i] = from+i;
            return s;
        } else return sparse.fillArray(dest);
    }

    /**
     * Remove val from the set
     * @param val
     * @return true if val was in the set, false otherwise
     */
    public boolean remove(int val) {
        if (isInterval()) {
            if (!interval.contains(val)) {
                return false;
            } else if (val == interval.getMin()) {
                interval.removeBelow(val + 1);
                return true;
            } else if (val == interval.getMax()) {
                interval.removeAbove(val - 1);
                return true;
            } else {
                buildSparse();
                return sparse.remove(val);
            }
        } else return sparse.remove(val);
    }
    /**
     * Removes all the element from the set except v
     * @param v is an element in the set
     */
    public void removeAllBut(int v) {
        if (isInterval()) {
            interval.removeAllBut(v);
        } else {
            sparse.removeAllBut(v);
        }
    }
    /**
     * Remove all the values in the set
     */
    public void removeAll() {
        if (isInterval()) {
            interval.removeAll();
        } else {
            sparse.removeAll();
        }
    }
    /**
     * Remove all the values < value in the set
     * @param value
     */
    public void removeBelow(int value) {
        if (isInterval()) {
            interval.removeBelow(value);
        } else {
            sparse.removeBelow(value);
        }
    }

    /**
     * Remove all the values > value in the set
     */
    public void removeAbove(int value) {
        if (isInterval()) {
            interval.removeAbove(value);
        } else {
            sparse.removeAbove(value);
        }
    }

    @Override public String toString() {
        if (isInterval()) {
            return interval.toString();
        } else {
            return sparse.toString();
        }
    }
}