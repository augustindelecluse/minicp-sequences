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


package minicp.engine.constraints;

import minicp.cp.Factory;
import minicp.engine.core.IntVar;

public class Element1D extends Element2D {

    private static int[][] to2d(int[] t) {
        int[][] t2 = new int[1][t.length];
        System.arraycopy(t, 0, t2[0], 0, t.length);
        return t2;
    }

    /**
     * T[y] = z
     *
     * @param T
     * @param y
     * @param z
     */
    public Element1D(int[] T, IntVar y, IntVar z) {
        super(to2d(T), Factory.makeIntVar(y.getSolver(), 0, 0), y, z);
    }

}
