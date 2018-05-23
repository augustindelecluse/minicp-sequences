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

public class IntVarViewOffset2 implements IntVar {

    private Solver cp;
    private ReversibleStack<VarListener> onDomain;
    private ReversibleStack<VarListener> onBind;
    private ReversibleStack<VarListener> onBounds;

    private final IntVar x;
    private final int o;

    public IntVarViewOffset2(IntVar x, int offset) { // y = x + o
        this.x = x;
        this.o = offset;
        this.cp = x.getSolver();
        cp.registerVar(this);
        onDomain = new ReversibleStack<>(cp.getTrail());
        onBind  = new ReversibleStack<>(cp.getTrail());
        onBounds = new ReversibleStack<>(cp.getTrail());

        x.whenDomainChange(() -> notifyListeners(onDomain));
        x.whenBind(() -> notifyListeners(onBind));
        x.whenBoundsChange(() -> notifyListeners(onBounds));
    }

    @Override
    public Solver getSolver() {
        return x.getSolver();
    }

    @Override
    public void whenBind(VarListener l) {
        onBind.push(l);
    }

    @Override
    public void whenBoundsChange(VarListener l) {
        onBounds.push(l);
    }

    @Override
    public void whenDomainChange(VarListener l) {
        onDomain.push(l);
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


    protected void notifyListeners(ReversibleStack<VarListener> listeners) throws InconsistencyException {
        for (int i = 0; i < listeners.size(); i++)
            listeners.get(i).call();
    }

    @Override
    public int getMin() {
        return x.getMin() + o;
    }

    @Override
    public int getMax() {
        return x.getMax() + o;
    }

    @Override
    public int getSize() {
        return x.getSize();
    }

    @Override
    public int fillArray(int[] dest) {
        int s = x.fillArray(dest);
        for (int i = 0; i < s; i++) {
            dest[i] += o;
        }
        return s;
    }

    @Override
    public boolean isBound() {
        return x.isBound();
    }

    @Override
    public boolean contains(int v) {
        return x.contains(v - o);
    }

    @Override
    public void remove(int v) throws InconsistencyException {
        x.remove(v - o);
    }

    @Override
    public void assign(int v) throws InconsistencyException {
        x.assign(v - o);
    }

    @Override
    public int removeBelow(int v) throws InconsistencyException {
        return x.removeBelow(v - o);
    }

    @Override
    public int removeAbove(int v) throws InconsistencyException {
        return x.removeAbove(v - o);
    }

    @Override
    public String toString() {
            StringBuilder b = new StringBuilder();
            b.append("{");
            for (int i = getMin(); i <= getMax() - 1; i++) {
                if (contains((i))) {
                    b.append(i);
                    b.append(',');
                }
            }
            if (getSize() > 0) b.append(getMax());
            b.append("}");
            return b.toString();

    }
}
