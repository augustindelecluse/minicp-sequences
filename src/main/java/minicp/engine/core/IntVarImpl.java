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

import minicp.reversible.ReversibleStack;
import minicp.util.InconsistencyException;

import java.security.InvalidParameterException;
import java.util.Set;

public class IntVarImpl implements IntVar {

    private Solver cp;
    private IntDomain domain;
    private ReversibleStack<Constraint> onDomain;
    private ReversibleStack<Constraint> onBind;
    private ReversibleStack<Constraint> onBounds;

    protected DomainListener domListener = new DomainListener() {
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
        if (min > max) throw new InvalidParameterException("at least one value in the domain");
        this.cp = cp;
        cp.registerVar(this);
        domain = new SparseSetDomain(cp, min, max);
        onDomain = new ReversibleStack<>(cp);
        onBind = new ReversibleStack<>(cp);
        onBounds = new ReversibleStack<>(cp);
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
        if (values.isEmpty()) throw new InvalidParameterException("at least one value in the domain");
        for (int i = getMin(); i <= getMax(); i++) {
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
    public void whenBind(BasicConstraintClosure.Filtering f) {
        onBind.push(new BasicConstraintClosure(cp, f));
    }

    @Override
    public void whenBoundsChange(BasicConstraintClosure.Filtering f) {
        onBounds.push(new BasicConstraintClosure(cp, f));
    }

    @Override
    public void whenDomainChange(BasicConstraintClosure.Filtering f) {
        onDomain.push(new BasicConstraintClosure(cp, f));
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


    protected void scheduleAll(ReversibleStack<Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++)
            constraints.get(i).schedule();
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

    @Override
    public int fillArray(int[] dest) {
        return domain.fillArray(dest);
    }

    public boolean contains(int v) {
        return domain.contains(v);
    }

    public void remove(int v)  {
        domain.remove(v, domListener);
    }
    public void assign(int v) {
        domain.removeAllBut(v, domListener);
    }
    public void removeBelow(int v)  {
        domain.removeBelow(v, domListener);
    }
    public void removeAbove(int v) {
        domain.removeAbove(v, domListener);
    }

}
