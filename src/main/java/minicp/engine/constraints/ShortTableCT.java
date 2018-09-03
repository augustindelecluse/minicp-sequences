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

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

import java.util.BitSet;

import static minicp.cp.Factory.minus;

/**
 * Table constraint with short tuples (having {@code *} entries)
 */
public class ShortTableCT extends AbstractConstraint {

    private final IntVar[] x; //variables
    private final int[][] table; //the table
    //supports[i][v] is the set of tuples supported by x[i]=v
    private BitSet[][] supports;

    /**
     * Create a Table constraint with short tuples.
     * <p> </p>Assignment of {@code x_0=v_0, x_1=v_1,...} only valid if there exists a
     * row {@code (v_0|*,v_1|*, ...)} in the table.
     *
     * @param x     the variables to constraint. x must be non empty.
     * @param table the array of valid solutions (second dimension must be of same size as the array x)
     * @param star  the {@code *} symbol representing "any" value in the table
     */
    public ShortTableCT(IntVar[] x, int[][] table, int star) {
        super(x[0].getSolver());
        this.x = x;
        this.table = table;

        // Allocate supportedByVarVal
        supports = new BitSet[x.length][];
        for (int i = 0; i < x.length; i++) {
            this.x[i] = minus(x[i], x[i].min()); // map the variables domain to start at 0
            supports[i] = new BitSet[x[i].max() - x[i].min() + 1];
            for (int j = 0; j < supports[i].length; j++)
                supports[i][j] = new BitSet();
        }

        // Set values in supportedByVarVal, which contains all the tuples supported by each var-val pair
        // TODO: compute the supports (be careful, take into account the star value)
        // STUDENT throw new NotImplementedException("ShortTableCT");
        // BEGIN STRIP
        for (int i = 0; i < table.length; i++) { //i is the index of the tuple (in table)
            for (int j = 0; j < x.length; j++) { //j is the index of the current variable (in x)
                if (table[i][j] == star) {
                    for (int v = 0; v < x[j].size(); v++) {
                        supports[j][v].set(i);
                    }
                } else if (x[j].contains(table[i][j])) {
                    supports[j][table[i][j] - x[j].min()].set(i);
                }
            }
        }
        // END STRIP
    }

    @Override
    public void post() {
        for (IntVar var : x)
            var.propagateOnDomainChange(this);
        propagate();
    }

    @Override
    public void propagate() {
        // TODO: implement the filtering
        // STUDENT throw new NotImplementedException("ShortTableCT");
        // BEGIN STRIP
        // Bit-set of tuple indices all set to 0
        BitSet supportedTuples = new BitSet(table.length);
        supportedTuples.flip(0, table.length);

        for (int i = 0; i < x.length; i++) {
            BitSet supporti = new BitSet();
            for (int v = x[i].min(); v <= x[i].max(); v++) {
                if (x[i].contains(v)) {
                    supporti.or(supports[i][v]);
                }
            }
            supportedTuples.and(supporti);
        }

        for (int i = 0; i < x.length; i++) {
            for (int v = x[i].min(); v <= x[i].max(); v++) {
                if (x[i].contains(v)) {
                    if (!supports[i][v].intersects(supportedTuples)) {
                        x[i].remove(v);
                    }
                }
            }
        }
        // END STRIP
    }
}
