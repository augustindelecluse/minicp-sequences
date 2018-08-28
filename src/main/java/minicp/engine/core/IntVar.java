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

import minicp.util.exception.InconsistencyException;

public interface IntVar {

    /**
     * Return the solver in which this variable was created
     *
     * @return the solver in which this variable was created
     */
    Solver getSolver();

    /**
     * Ask that the closure is called whenever the domain
     * of this variable is reduced to a single setValue
     *
     * @param f
     */
    void whenBind(ConstraintClosure.Filtering f);

    /**
     * Ask that the closure is called whenever
     * the max or min setValue of the domain of this variable changes
     *
     * @param f
     */
    void whenBoundsChange(ConstraintClosure.Filtering f);

    /**
     * Ask that the closure is called whenever the domain change
     * of this variable changes
     *
     * @param f
     */
    void whenDomainChange(ConstraintClosure.Filtering f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable changes.
     * We say that a <i>change</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on change events of this variable.
     */
    void propagateOnDomainChange(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable is reduced to a singleton.
     * In such a state the variable is bind and we say that a <i>bind</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bind events of this variable.
     */
    void propagateOnBind(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the
     * bound (maximum or minimum values) of the domain
     * of this variable is changes.
     * We say that a <i>bound change</i> event occurs in this case.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bound change events of this variable.
     */
    void propagateOnBoundChange(Constraint c);


    /**
     * Returns the minimum of the domain of the variable
     *
     * @return the minimum of the domain of the variable
     */
    int min();

    /**
     * Returns the maximum of the domain of the variable
     *
     * @return the maximum of the domain of the variable
     */
    int max();

    /**
     * Returns the size of the domain of the variable
     *
     * @return the size of the domain of the variable
     */
    int size();

    /**
     * Copy the values of the domain in an array in arbitrary order.
     *
     * @param dest, an array large enough to contain the current domain
     * @return the size of the domain and {@code dest[0],...,dest[size-1]}
     *         now contains the values in the domain in an arbitrary order
     */
    int fillArray(int[] dest);

    /**
     * Returns true if the domain of the variable has a single value.
     *
     * @return true if the domain of the variable is a singleton.
     */
    boolean isBound();

    /**
     * Returns true if the domain contains the specified value.
     * @param v the value whose presence in the domain is to be tested
     * @return true if the domain contains the specified value
     */
    boolean contains(int v);

    /**
     * Removes the specified value.
     * @param v the value to remove
     * @exception InconsistencyException
     *            is thrown if the domain becomes empty
     */
    void remove(int v);

    /**
     * Assigns the specified value
     *
     * @param v
     * @exception InconsistencyException
     *            is thrown if the domain becomes empty
     */
    void assign(int v);

    /**
     * Remove all the values < va
     *
     * @param v
     * @return the new minimum
     */
    void removeBelow(int v);

    /**
     * Remove all the values > v
     *
     * @param v
     * @return the new maximum
     */
    void removeAbove(int v);
}
