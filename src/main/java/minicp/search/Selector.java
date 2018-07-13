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

import minicp.util.Procedure;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Selector {

    public static final Procedure[] EMPTY = new Procedure[0];

    public static Procedure[] branch(Procedure... branches) {
        return branches;
    }
    
    public static <T,N extends Comparable<N> > Supplier<Procedure[]> selectMin(T[] x, Predicate<T> p, Function<T,N> f, Function<T,Procedure[]> body) {
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
                return body.apply(sel);
            }
        };
    }
}
