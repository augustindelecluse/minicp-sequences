package minicp.state;

public interface IntSparseSet {

    /**
     * Returns an array with the values present in the set.
     *
     * @return an array representation of the values present in the set
     */
    public int[] toArray();

    /**
     * Sets the first values of <code>dest</code> to the ones
     * present in the set.
     *
     * @param dest, an array large enough {@code dest.length >= size()}
     * @return the size of the set
     */
    public int fillArray(int[] dest);

    /**
     * Checks if the set is empty
     *
     * @return true if the set is empty
     */
    public boolean isEmpty();

    /**
     * Returns the size of the set.
     *
     * @return the size of the set
     */
    public int size();

    /**
     * Returns the minimum value in the set.
     *
     * @return the minimum value in the set
     */
    public int min();

    /**
     * Returns the maximum value in the set.
     *
     * @return the maximum value in the set
     */
    public int max();

    /**
     * Removes the given value from the set.
     *
     * @param val the value to remove.
     * @return true if val was in the set, false otherwise
     */
    public boolean remove(int val);

    /**
     * Checks if a value is in the set.
     *
     * @param val the value to check
     * @return true if val is in the set
     */
    public boolean contains(int val);

    /**
     * Removes all the element from the set except the given value.
     *
     * @param v is an element in the set
     */
    public void removeAllBut(int v);

    /**
     * Removes all the values in the set.
     */
    public void removeAll();

    /**
     * Remove all the values less than the given value from the set
     *
     * @param value a value such that all the ones smaller are removed
     */
    public void removeBelow(int value);

    /**
     * Remove all the values larger than the given value from the set
     *
     * @param value a value such that all the ones greater are removed
     */
    public void removeAbove(int value);

}
