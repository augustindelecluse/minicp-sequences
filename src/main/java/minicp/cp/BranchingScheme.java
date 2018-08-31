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

import minicp.engine.core.IntVar;
import minicp.search.LimitedDiscrepancyBranching;
import minicp.search.Sequencer;
import minicp.util.Procedure;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static minicp.cp.Factory.equal;
import static minicp.cp.Factory.notEqual;

public final class BranchingScheme {

    private BranchingScheme() {
        throw new UnsupportedOperationException();
    }


    public static final Procedure[] EMPTY = new Procedure[0];

    public static Procedure[] branch(Procedure... branches) {
        return branches;
    }

    public static <T, N extends Comparable<N>> T selectMin(T[] x, Predicate<T> p, Function<T, N> f) {
        T sel = null;
        for (T xi : x) {
            if (p.test(xi)) {
                sel = sel == null || f.apply(xi).compareTo(f.apply(sel)) < 0 ? xi : sel;
            }
        }
        return sel;
    }

    public static Supplier<Procedure[]> firstFail(IntVar... x) {
        return () -> {
            IntVar xs = selectMin(x,
                    xi -> xi.size() > 1,
                    xi -> xi.size());
            if (xs == null)
                return EMPTY;
            else {
                int v = xs.min();
                return branch(() -> equal(xs, v),
                        () -> notEqual(xs, v));
            }
        };
    }

    public static Supplier<Procedure[]> and(Supplier<Procedure[]>... choices) {
        return new Sequencer(choices);
    }

    public static Supplier<Procedure[]> limitedDiscrepancy(Supplier<Procedure[]> branching, int maxDiscrepancy) {
        return new LimitedDiscrepancyBranching(branching, maxDiscrepancy);
    }

}
