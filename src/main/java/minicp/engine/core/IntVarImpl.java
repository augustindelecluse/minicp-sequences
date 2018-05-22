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
    private ReversibleStack<Watcher> onDomain;
    private ReversibleStack<Watcher> onBind;
    private ReversibleStack<Watcher> onBounds;

    protected DomainListener domListener = new DomainListener() {
        @Override
        public void bind() throws InconsistencyException {
            awakeAll(onBind);
        }

        @Override
        public void change(int domainSize) throws InconsistencyException{
            awakeAll(onDomain);
        }

        @Override
        public void removeBelow(int domainSize) throws InconsistencyException {
            awakeAll(onBounds);
        }

        @Override
        public void removeAbove(int domainSize) throws InconsistencyException {
            awakeAll(onBounds);
        }
    };

    /**
     * Create a variable with the elements {0,...,n-1}
     * as initial domain
     * @param cp
     * @param n > 0
     */
    public IntVarImpl(Solver cp, int n) {
        this(cp,0,n-1);
    }

    /**
     * Create a variable with the elements {min,...,max}
     * as initial domain
     * @param cp
     * @param min
     * @param max >= min
     */
    public IntVarImpl(Solver cp, int min, int max) {
        if (min > max) throw new InvalidParameterException("at least one value in the domain");
        this.cp = cp;
        cp.registerVar(this);
        domain = new SparseSetDomain(cp.getTrail(),min,max);
        onDomain = new ReversibleStack<>(cp.getTrail());
        onBind  = new ReversibleStack<>(cp.getTrail());
        onBounds = new ReversibleStack<>(cp.getTrail());
    }

    public Solver getSolver() {
        return cp;
    }



    /**
     * Create a variable with values as initial domain
     * @param cp
     * @param values
     */
    public IntVarImpl(Solver cp, Set<Integer> values) {
        this(cp,values.stream().min(Integer::compare).get(),values.stream().max(Integer::compare).get());
        if (values.isEmpty()) throw new InvalidParameterException("at least one value in the domain");
        for (int i = getMin(); i < getMax(); i++) {
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
    public void whenBind(WatcherClosure.Awake w) {
        onBind.push(new WatcherClosure(cp,w));
    }

    @Override
    public void whenBoundsChange(WatcherClosure.Awake w) {
        onBounds.push(new WatcherClosure(cp,w));
    }

    @Override
    public void whenDomainChange(WatcherClosure.Awake w) {
        onDomain.push(new WatcherClosure(cp,w));
    }

    public void propagateOnDomainChange(ConstraintClosure.Filtering c) {
        onDomain.push(new ConstraintClosure(cp,c));
    }

    public void propagateOnBind(ConstraintClosure.Filtering c) {
        onBind.push(new ConstraintClosure(cp,c));
    }

    public void propagateOnBoundChange(ConstraintClosure.Filtering c) {
        onBounds.push(new ConstraintClosure(cp,c));
    }


    public void propagateOnDomainChange(Constraint c) {
        onDomain.push(c);
    }

    public void propagateOnBind(Constraint c) {
        onBind.push(c);
    }

    public void propagateOnBoundChange(Constraint c) { onBounds.push(c);}




    protected void awakeAll(ReversibleStack<Watcher> watchers) throws InconsistencyException {
        for (int i = 0; i < watchers.size(); i++)
            watchers.get(i).awake();
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

    public void remove(int v) throws InconsistencyException {
        domain.remove(v, domListener);
    }

    public void assign(int v) throws InconsistencyException {
        domain.removeAllBut(v, domListener);
    }

    public int removeBelow(int v) throws InconsistencyException {
        return domain.removeBelow(v, domListener);
    }

    public int removeAbove(int v) throws InconsistencyException {
        return domain.removeAbove(v, domListener);
    }

}
