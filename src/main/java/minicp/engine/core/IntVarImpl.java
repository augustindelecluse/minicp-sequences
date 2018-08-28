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

import minicp.state.StateStack;
import minicp.util.exception.InconsistencyException;

import java.security.InvalidParameterException;
import java.util.Set;

public class IntVarImpl implements IntVar {

    private Solver cp;
    private IntDomain domain;
    private StateStack<Constraint> onDomain;
    private StateStack<Constraint> onBind;
    private StateStack<Constraint> onBounds;

    private DomainListener domListener = new DomainListener() {
        @Override
        public void empty() {
            throw InconsistencyException.INCONSISTENCY; // Integer Vars cannot be empty
        }

        @Override
        public void bind() {
            scheduleAll(onBind);
        }

        @Override
        public void change() {
            scheduleAll(onDomain);
        }

        @Override
        public void removeBelow() {
            scheduleAll(onBounds);
        }

        @Override
        public void removeAbove() {
            scheduleAll(onBounds);
        }
    };

    /**
     * Create a variable with the elements {0,...,n-1}
     * as initial domain
     *
     * @param cp
     * @param n  > 0
     */
    public IntVarImpl(Solver cp, int n) {
        this(cp, 0, n - 1);
    }

    /**
     * Create a variable with the elements {min,...,max}
     * as initial domain
     *
     * @param cp
     * @param min
     * @param max >= min
     */
    public IntVarImpl(Solver cp, int min, int max) {
        if (min > max) throw new InvalidParameterException("at least one setValue in the domain");
        this.cp = cp;
        cp.registerVar(this);
        domain = new SparseSetDomain(cp.getStateManager(), min, max);
        onDomain = new StateStack<>(cp.getStateManager());
        onBind = new StateStack<>(cp.getStateManager());
        onBounds = new StateStack<>(cp.getStateManager());
    }

    public Solver getSolver() {
        return cp;
    }


    /**
     * Create a variable with values as initial domain
     *
     * @param cp
     * @param values
     */
    public IntVarImpl(Solver cp, Set<Integer> values) {
        this(cp, values.stream().min(Integer::compare).get(), values.stream().max(Integer::compare).get());
        if (values.isEmpty()) throw new InvalidParameterException("at least one setValue in the domain");
        for (int i = min(); i <= max(); i++) {
            if (!values.contains(i)) {
                try {
                    this.remove(i);
                } catch (InconsistencyException e) {
                }
            }
        }
    }

    public boolean isBound() {
        return domain.getSize() == 1;
    }

    @Override
    public String toString() {
        return domain.toString();
    }

    @Override
    public void whenBind(ConstraintClosure.Filtering f) {
        onBind.push(constraintClosure(f));
    }

    @Override
    public void whenBoundsChange(ConstraintClosure.Filtering f) {
        onBounds.push(constraintClosure(f));
    }

    @Override
    public void whenDomainChange(ConstraintClosure.Filtering f) {
        onDomain.push(constraintClosure(f));
    }

    private Constraint constraintClosure(ConstraintClosure.Filtering f) {
        Constraint c = new ConstraintClosure(cp, f);
        getSolver().post(c, false);
        return c;
    }

    @Override
    public void propagateOnDomainChange(Constraint c) {
        onDomain.push(c);
    }

    @Override
    public void propagateOnBind(Constraint c) {
        onBind.push(c);
    }

    @Override
    public void propagateOnBoundChange(Constraint c) {
        onBounds.push(c);
    }


    protected void scheduleAll(StateStack<Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++)
            cp.schedule(constraints.get(i));
    }

    @Override
    public int min() {
        return domain.getMin();
    }

    @Override
    public int max() {
        return domain.getMax();
    }

    @Override
    public int size() {
        return domain.getSize();
    }

    @Override
    public int fillArray(int[] dest) {
        return domain.fillArray(dest);
    }

    @Override
    public boolean contains(int v) {
        return domain.contains(v);
    }

    @Override
    public void remove(int v) {
        domain.remove(v, domListener);
    }

    @Override
    public void assign(int v) {
        domain.removeAllBut(v, domListener);
    }

    @Override
    public void removeBelow(int v) {
        domain.removeBelow(v, domListener);
    }

    @Override
    public void removeAbove(int v) {
        domain.removeAbove(v, domListener);
    }
}
