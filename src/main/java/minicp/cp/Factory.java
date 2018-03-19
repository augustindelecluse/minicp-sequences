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

package minicp.cp;

import minicp.engine.constraints.*;
import minicp.engine.core.*;
import minicp.search.Choice;
import minicp.search.DFSearch;
import minicp.util.InconsistencyException;


import java.util.Set;

public class Factory {

    static public Solver makeSolver() {
        return new Solver();
    }

    static public IntVar mul(IntVar x, int a) {
        if (a == 0) return makeIntVar(x.getSolver(),0,0);
        else if (a == 1) return x;
        else if (a < 0) {
            return minus(new IntVarViewMul(x,-a));
        } else {
            return new IntVarViewMul(x,a);
        }
    }

    static public IntVar minus(IntVar x) {
        return new IntVarViewOpposite(x);
    }

    static public IntVar plus(IntVar x, int v) {
        return new IntVarViewOffset(x,v);
    }

    static public IntVar minus(IntVar x, int v) {
        return new IntVarViewOffset(x,-v);
    }

    static public IntVar abs(IntVar x) throws InconsistencyException {
        IntVar r = makeIntVar(x.getSolver(), 0, x.getMax());
        x.getSolver().post(new Absolute(x, r));
        return r;
    }

    /**
     * Create a variable with the elements {0,...,n-1}
     * as initial domain
     * @param cp
     * @param n > 0
     */
    static public IntVar makeIntVar(Solver cp, int n) {
        return new IntVarImpl(cp,n);
    }

    /**
     * Create a variable with the elements {min,...,max}
     * as initial domain
     * @param cp
     * @param min
     * @param max > min
     */
    static public IntVar makeIntVar(Solver cp, int min, int max) {
        return new IntVarImpl(cp,min,max);
    }

    static public IntVar makeIntVar(Solver cp, Set<Integer> values) {
        return new IntVarImpl(cp,values);
    }

    static public BoolVar makeBoolVar(Solver cp) {
        return new BoolVarImpl(cp);
    }

    // Factory
    static public IntVar[] makeIntVarArray(Solver cp, int n, int sz) {
        IntVar[] rv = new IntVar[n];
        for (int i = 0; i < n; i++)
            rv[i] = makeIntVar(cp, sz);
        return rv;
    }

    @FunctionalInterface
    public interface BodyClosure {
        IntVar call(int i) throws InconsistencyException ;
    }

    static public IntVar[] makeIntVarArray(Solver cp, int n, BodyClosure body) throws InconsistencyException {
        IntVar[] rv = new IntVar[n];
        for (int i = 0; i < n; i++)
            rv[i] = body.call(i);
        return rv;
    }

    static public IntVar[] all(int low, int up, BodyClosure body) throws InconsistencyException {
        int sz = up - low + 1;
        IntVar[] t = new IntVar[sz];
        for (int i = low; i <= up; i++)
            t[i - low] = body.call(i);
        return t;
    }

    static public DFSearch makeDfs(Solver cp, Choice branching) {
        return new DFSearch(cp.getTrail(),branching);
    }


    // -------------- constraints -----------------------

    static public void equal(IntVar x, int v) throws InconsistencyException {
        x.assign(v);
        x.getSolver().fixPoint();
    }

    static public void notEqual(IntVar x, int v) throws InconsistencyException {
        x.remove(v);
        x.getSolver().fixPoint();
    }

    static public Constraint notEqual(IntVar x, IntVar y) {
        return new NotEqual(x,y);
    }
    static public Constraint notEqual(IntVar x, IntVar y,int c) {
        return new NotEqual(x,y,c);
    }

    static public BoolVar isEqual(IntVar x, final int c)  throws InconsistencyException  {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        cp.post(new IsEqual(b,x,c));
        return b;
    }

    static public BoolVar isLessOrEqual(IntVar x, final int c)  throws InconsistencyException  {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        cp.post(new IsLessOrEqual(b,x,c));
        return b;
    }

    static public BoolVar isLess(IntVar x, final int c)  throws InconsistencyException  {
        return isLessOrEqual(x,c-1);
    }

    static public BoolVar isLargerOrEqual(IntVar x, final int c)  throws InconsistencyException  {
        return isLessOrEqual(minus(x),-c);
    }

    static public BoolVar isLarger(IntVar x, final int c)  throws InconsistencyException  {
        return isLargerOrEqual(x,c+1);
    }

    static public Constraint lessOrEqual(IntVar x, IntVar y) {
        return new LessOrEqual(x,y);
    }

    static public Constraint largerOrEqual(IntVar x, IntVar y) {
        return new LessOrEqual(y,x);
    }

    static public Constraint minimize(IntVar x, DFSearch dfs) {
        return new Minimize(x,dfs);
    }

    static public Constraint maximize(IntVar x, DFSearch dfs) {
        return new Minimize(minus(x),dfs);
    }

    static public IntVar element(int[][] T, IntVar x, IntVar y) throws InconsistencyException {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < T.length; i++) {
            for (int j = 0; j < T[i].length; j++) {
                min = Math.min(min, T[i][j]);
                max = Math.max(max, T[i][j]);
            }
        }
        IntVar z = makeIntVar(x.getSolver(), min, max);
        x.getSolver().post(new Element2D(T, x, y, z));
        return z;
    }

    static public IntVar sum(IntVar... x) throws InconsistencyException {
        int sumMin = 0;
        int sumMax = 0;
        for (int i = 0; i < x.length; i++) {
            sumMin += x[i].getMin();
            sumMax += x[i].getMax();
        }
        Solver cp = x[0].getSolver();
        IntVar s = makeIntVar(cp, sumMin, sumMax);
        cp.post(new Sum(x, s));
        return s;
    }

    static public Constraint sum(IntVar[] x, IntVar y) throws InconsistencyException  {
        return new Sum(x,y);
    }

    static public Constraint sum(IntVar[] x, int y) throws InconsistencyException  {
        return new Sum(x,y);
    }

    static public Constraint allDifferent(IntVar[] x) throws InconsistencyException  {
        return new AllDifferentBinary(x);
    }
}
