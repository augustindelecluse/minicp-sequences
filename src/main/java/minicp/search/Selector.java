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

package minicp.search;

import java.util.function.Function;
import java.util.function.Predicate;

public class Selector {

    public static final Branch[] EMPTY = new Branch[0];

    public static Branch[] branch(Branch... branches) {
        return branches;
    }

    @FunctionalInterface
    public interface ChoicePoint<T> {
        Branch[] call(T x);
    }

    public static <T,N extends Comparable<N> > BranchingScheme selectMin(T[] x, Predicate<T> p, Function<T,N> f, ChoicePoint<T> body) {
        return () -> {
            T sel = null;
            for (T xi : x) {
                if (p.test(xi)) {
                    sel = sel == null || f.apply(xi).compareTo(f.apply(sel)) < 0 ? xi : sel;
                }
            }
            if (sel == null) {
                return EMPTY;
            } else {
                return body.call(sel);
            }
        };
    }
}
