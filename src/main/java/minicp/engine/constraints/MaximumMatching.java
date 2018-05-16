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

import minicp.engine.core.IntVar;
import minicp.util.InconsistencyException;

import static minicp.util.InconsistencyException.*;

public class MaximumMatching {

    private static final int NONE = -Integer.MIN_VALUE;

    // For each variable, the value it is mached to
    private int[] match;
    private int[] varSeen;

    private int min;
    private int max;

    // Number of values
    private int valSize;
    // For each value, the variable idx matched to this value, -1 if none of them
    private int[] valMatch;
    private int[] valSeen;


    private int sizeMatching;

    private int magic;

    private IntVar [] x;

    public MaximumMatching(IntVar ... x) {
        this.x = x;

        // find value ranges

        min = Integer.MAX_VALUE;
        max = Integer.MIN_VALUE;
        for (int i = 0; i < x.length; i++) {
            min = Math.min(min, x[i].getMin());
            max = Math.max(max, x[i].getMax());
        }
        valSize = max - min + 1;
        valMatch = new int[valSize];
        for (int k = 0; k < valSize; k++)
            valMatch[k] = -1;  // unmatched

        // initialize

        magic = 0;
        match = new int[x.length];
        for (int k = 0; k < x.length; k++) {
            match[k] = NONE; // unmatched
        }
        varSeen = new int[x.length];
        valSeen = new int[valSize];

        findInitialMatching();
    }

    public int [] compute() throws InconsistencyException {
        for (int k = 0; k < x.length; k++) {
            if (match[k] != NONE) {
                if (!x[k].contains(match[k])) {
                    valMatch[match[k] - min] = -1;
                    match[k] = NONE;
                    sizeMatching--;
                }
            }
        }
        int sizeMatching = findMaximalMatching();
        if (sizeMatching < x.length) throw INCONSISTENCY;
        return match;
    }



    private void findInitialMatching() { //returns the size of the maximum matching
        sizeMatching = 0;
        for (int k = 0; k < x.length; k++) {
            int mx = x[k].getMin();
            int Mx = x[k].getMax();
            for (int i = mx; i <= Mx; i++)
                if (valMatch[i - min] < 0) // unmatched
                    if (x[k].contains(i)) {
                        match[k] = i;
                        valMatch[i - min] = k;
                        sizeMatching++;
                        break;
                    }
        }
    }

    private int findMaximalMatching() {
        if (sizeMatching < x.length) {
            for (int k = 0; k < x.length; k++) {
                if (match[k] == NONE) {
                    magic++;
                    if (findAlternatingPath(k)) {
                        sizeMatching++;
                    }
                }
            }
        }
        return sizeMatching;
    }

    private boolean findAlternatingPath(int i) {
        if (varSeen[i] != magic) {
            varSeen[i] = magic;
            int mx = x[i].getMin();
            int Mx = x[i].getMax();
            for (int v = mx; v <= Mx; v++) {
                if (match[i] != v) {
                    if (x[i].contains(v)) {
                        if (findAlternatingPathValue(v)) {
                            match[i] = v;
                            valMatch[v - min] = i;
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean findAlternatingPathValue(int v) {
        if (valSeen[v - min] != magic) {
            valSeen[v - min] = magic;
            if (valMatch[v - min] == -1)
                return true;
            if (findAlternatingPath(valMatch[v - min]))
                return true;
        }
        return false;
    }


}
