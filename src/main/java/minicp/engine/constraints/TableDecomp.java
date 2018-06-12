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
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.engine.constraints;

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.InconsistencyException;

public class TableDecomp extends Constraint {
    private IntVar[] x;
    private int[][] table;

    /**
     * Table constraint. Assignment of x_0=v_0, x_1=v_1,... only valid if there exists a
     * row (v_0, v_1, ...) in the table.
     *
     * @param x     variables to constraint. x.length must be > 0.
     * @param table array of valid solutions (second dimension must be of same size as the array x)
     */
    public TableDecomp(IntVar[] x, int[][] table) {
        super(x[0].getSolver());
        this.x = x;
        this.table = table;
    }

    @Override
    public void post()  {
        for (IntVar var : x)
            var.propagateOnDomainChange(this);
        propagate();
    }

    @Override
    public void propagate() {
        for (int i = 0; i < x.length; i++) {
            for (int v = x[i].getMin(); v <= x[i].getMax(); v++) {
                if (x[i].contains(v)) {
                    boolean valueIsSupported = false;
                    for (int tupleIdx = 0; tupleIdx < table.length && !valueIsSupported; tupleIdx++) {
                        if (table[tupleIdx][i] == v) {
                            boolean allValueVariableSupported = true;
                            for (int j = 0; j < x.length && allValueVariableSupported; j++) {
                                if (!x[j].contains(table[tupleIdx][j])) {
                                    allValueVariableSupported = false;
                                }
                            }
                            valueIsSupported = allValueVariableSupported;
                        }
                    }
                    if (!valueIsSupported)
                        x[i].remove(v);
                }
            }
        }
    }
}
