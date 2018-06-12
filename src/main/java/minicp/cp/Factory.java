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

import minicp.reversible.RevInt;
import minicp.reversible.RevBool;
import minicp.engine.constraints.*;
import minicp.engine.core.*;
import minicp.reversible.StateManager;
import minicp.search.BranchingScheme;
import minicp.search.DFSearch;
import minicp.util.InconsistencyException;


import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

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

    static public IntVar abs(IntVar x) {
        IntVar r = makeIntVar(x.getSolver(), 0, x.getMax());
        x.getSolver().post(new Absolute(x, r));
        return r;
    }

    static public RevInt makeRevInt(StateManager m,int initialValue) {
        return m.getTrail().makeRevInt(initialValue);
    }

    static public RevBool makeRevBool(StateManager m,boolean initValue) {
        return m.getTrail().makeRevBool(initValue);
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
        IntVar call(int i);
    }

    static public IntVar[] makeIntVarArray(Solver cp, int n, BodyClosure body) {
        IntVar[] rv = new IntVar[n];
        for (int i = 0; i < n; i++)
            rv[i] = body.call(i);
        return rv;
    }

    static public IntVar[] all(int low, int up, BodyClosure body) {
        int sz = up - low + 1;
        IntVar[] t = new IntVar[sz];
        for (int i = low; i <= up; i++)
            t[i - low] = body.call(i);
        return t;
    }

    static public DFSearch makeDfs(Solver cp, BranchingScheme branching) {
        return new DFSearch(cp,branching);
    }


    // -------------- constraints -----------------------

    static public IntVar maximum(IntVar ... x) {
        Solver cp = x[0].getSolver();
        int min = Arrays.stream(x).mapToInt(IntVar::getMin).min().getAsInt();
        int max = Arrays.stream(x).mapToInt(IntVar::getMax).max().getAsInt();
        IntVar y = makeIntVar(cp,min,max);
        cp.post(new Maximum(x,y));
        return y;
    }

    static public IntVar minimum(IntVar ... x)  {
        IntVar[] minusX = Arrays.stream(x).map(Factory::minus).toArray(IntVar[]::new);
        return minus(maximum(minusX));
    }

    static public void equal(IntVar x, int v) {
        x.assign(v);
        x.getSolver().fixPoint();
    }

    static public void lessOrEqual(IntVar x, int v)  {
        x.removeAbove(v);
        x.getSolver().fixPoint();
    }

    static public void notEqual(IntVar x, int v) {
        x.remove(v);
        x.getSolver().fixPoint();
    }

    static public Constraint notEqual(IntVar x, IntVar y) {
        return new NotEqual(x,y);
    }
    static public Constraint notEqual(IntVar x, IntVar y,int c) {
        return new NotEqual(x,y,c);
    }

    static public BoolVar isEqual(IntVar x, final int c)   {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        try {
            cp.post(new IsEqual(b,x,c));
        } catch (InconsistencyException e) {
            e.printStackTrace();
        }
        return b;
    }

    static public BoolVar isLessOrEqual(IntVar x, final int c)  {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        cp.post(new IsLessOrEqual(b,x,c));
        return b;
    }

    static public BoolVar isLess(IntVar x, final int c)   {
        return isLessOrEqual(x,c-1);
    }

    static public BoolVar isLargerOrEqual(IntVar x, final int c)  {
        return isLessOrEqual(minus(x),-c);
    }

    static public BoolVar isLarger(IntVar x, final int c)  {
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

    static public IntVar element(int[] T, IntVar y) {
        Solver cp = y.getSolver();
        IntVar z = makeIntVar(cp,IntStream.of(T).min().getAsInt(),IntStream.of(T).max().getAsInt());
        cp.post(new Element1D(T,y,z));
        return z;
    }

    static public IntVar element(int[][] T, IntVar x, IntVar y) {
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

    static public IntVar sum(IntVar... x) {
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

    static public Constraint sum(IntVar[] x, IntVar y)  {
        return new Sum(x,y);
    }

    static public Constraint sum(IntVar[] x, int y)  {
        return new Sum(x,y);
    }

    static public Constraint allDifferent(IntVar[] x)  {
        return new AllDifferentBinary(x);
    }
    static public Constraint allDifferentAC(IntVar[] x)  {
        return new AllDifferentAC(x);
    }
}
