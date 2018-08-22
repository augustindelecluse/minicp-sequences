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
import minicp.search.DFSearch;
import minicp.state.Copier;
import minicp.state.Trailer;
import minicp.util.InconsistencyException;
import minicp.util.Procedure;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class Factory {

    private Factory() {
        throw new UnsupportedOperationException();
    }

    public static Solver makeSolver() {
        return new MiniCP(new Trailer());
    }

    public static Solver makeSolver(boolean byCopy) {
        return new MiniCP(byCopy ? new Copier() : new Trailer());
    }

    public static IntVar mul(IntVar x, int a) {
        if (a == 0) return makeIntVar(x.getSolver(), 0, 0);
        else if (a == 1) return x;
        else if (a < 0) {
            return minus(new IntVarViewMul(x, -a));
        } else {
            return new IntVarViewMul(x, a);
        }
    }

    public static IntVar minus(IntVar x) {
        return new IntVarViewOpposite(x);
    }

    public static IntVar plus(IntVar x, int v) {
        return new IntVarViewOffset(x, v);
    }

    public static IntVar minus(IntVar x, int v) {
        return new IntVarViewOffset(x, -v);
    }

    public static IntVar abs(IntVar x) {
        IntVar r = makeIntVar(x.getSolver(), 0, x.getMax());
        x.getSolver().post(new Absolute(x, r));
        return r;
    }

    /**
     * Create a variable with the elements {0,...,sz-1}
     * as initial domain
     *
     * @param cp
     * @param sz > 0
     */
    public static IntVar makeIntVar(Solver cp, int sz) {
        return new IntVarImpl(cp, sz);
    }

    /**
     * Create a variable with the elements {min,...,max}
     * as initial domain
     *
     * @param cp
     * @param min
     * @param max > min
     */
    public static IntVar makeIntVar(Solver cp, int min, int max) {
        return new IntVarImpl(cp, min, max);
    }

    public static IntVar makeIntVar(Solver cp, Set<Integer> values) {
        return new IntVarImpl(cp, values);
    }

    public static BoolVar makeBoolVar(Solver cp) {
        return new BoolVarImpl(cp);
    }

    // Factory
    public static IntVar[] makeIntVarArray(Solver cp, int n, int sz) {
        return makeIntVarArray(n, i -> makeIntVar(cp, sz));
    }

    // Factory
    public static IntVar[] makeIntVarArray(Solver cp, int n, int min, int max) {
        return makeIntVarArray(n, i -> makeIntVar(cp, min, max));
    }

    public static IntVar[] makeIntVarArray(int n, Function<Integer, IntVar> body) {
        return makeIntVarArray(0, n - 1, body);
    }

    public static IntVar[] makeIntVarArray(int low, int up, Function<Integer, IntVar> body) {
        int sz = up - low + 1;
        IntVar[] t = new IntVar[sz];
        for (int i = low; i <= up; i++)
            t[i - low] = body.apply(i);
        return t;
    }

    public static DFSearch makeDfs(Solver cp, Supplier<Procedure[]> branching) {
        return new DFSearch(cp.getStateManager(), branching);
    }

    // -------------- constraints -----------------------

    public static IntVar maximum(IntVar... x) {
        Solver cp = x[0].getSolver();
        int min = Arrays.stream(x).mapToInt(IntVar::getMin).min().getAsInt();
        int max = Arrays.stream(x).mapToInt(IntVar::getMax).max().getAsInt();
        IntVar y = makeIntVar(cp, min, max);
        cp.post(new Maximum(x, y));
        return y;
    }

    public static IntVar minimum(IntVar... x) {
        IntVar[] minusX = Arrays.stream(x).map(Factory::minus).toArray(IntVar[]::new);
        return minus(maximum(minusX));
    }

    public static void equal(IntVar x, int v) {
        x.assign(v);
        x.getSolver().fixPoint();
    }

    public static void lessOrEqual(IntVar x, int v) {
        x.removeAbove(v);
        x.getSolver().fixPoint();
    }

    public static void notEqual(IntVar x, int v) {
        x.remove(v);
        x.getSolver().fixPoint();
    }

    public static Constraint notEqual(IntVar x, IntVar y) {
        return new NotEqual(x, y);
    }

    public static Constraint notEqual(IntVar x, IntVar y, int c) {
        return new NotEqual(x, y, c);
    }

    public static BoolVar isEqual(IntVar x, final int c) {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        try {
            cp.post(new IsEqual(b, x, c));
        } catch (InconsistencyException e) {
            e.printStackTrace();
        }
        return b;
    }

    public static BoolVar isLessOrEqual(IntVar x, final int c) {
        BoolVar b = makeBoolVar(x.getSolver());
        Solver cp = x.getSolver();
        cp.post(new IsLessOrEqual(b, x, c));
        return b;
    }

    public static BoolVar isLess(IntVar x, final int c) {
        return isLessOrEqual(x, c - 1);
    }

    public static BoolVar isLargerOrEqual(IntVar x, final int c) {
        return isLessOrEqual(minus(x), -c);
    }

    public static BoolVar isLarger(IntVar x, final int c) {
        return isLargerOrEqual(x, c + 1);
    }

    public static Constraint lessOrEqual(IntVar x, IntVar y) {
        return new LessOrEqual(x, y);
    }

    public static Constraint largerOrEqual(IntVar x, IntVar y) {
        return new LessOrEqual(y, x);
    }


    public static IntVar element(int[] array, IntVar y) {
        Solver cp = y.getSolver();
        IntVar z = makeIntVar(cp, IntStream.of(array).min().getAsInt(), IntStream.of(array).max().getAsInt());
        cp.post(new Element1D(array, y, z));
        return z;
    }

    public static IntVar element(int[][] matrix, IntVar x, IntVar y) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                min = Math.min(min, matrix[i][j]);
                max = Math.max(max, matrix[i][j]);
            }
        }
        IntVar z = makeIntVar(x.getSolver(), min, max);
        x.getSolver().post(new Element2D(matrix, x, y, z));
        return z;
    }

    public static IntVar sum(IntVar... x) {
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

    public static Constraint sum(IntVar[] x, IntVar y) {
        return new Sum(x, y);
    }

    public static Constraint sum(IntVar[] x, int y) {
        return new Sum(x, y);
    }

    public static Constraint allDifferent(IntVar[] x) {
        return new AllDifferentBinary(x);
    }

    public static Constraint allDifferentAC(IntVar[] x) {
        return new AllDifferentAC(x);
    }
}
