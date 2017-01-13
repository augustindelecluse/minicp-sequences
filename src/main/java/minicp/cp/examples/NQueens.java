/*
 * This file is part of mini-cp.
 *
 * mini-cp is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mini-cp.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2016 L. Michel, P. Schaus, P. Van Hentenryck
 */

package minicp.cp.examples;

import minicp.cp.Factory;
import minicp.cp.constraints.DifferentVal;
import minicp.cp.constraints.EqualVal;
import minicp.cp.core.IntVar;
import minicp.cp.core.Solver;
import minicp.util.InconsistencyException;
import minicp.util.Box;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import static minicp.search.Selector.*;

public class NQueens {

    public static void main(String[] args) {
        Solver cp = new Solver();
        int n = 8;
        IntVar [] q = Factory.makeIntVarArray(cp,n,n);

        try {
            for (int i = 0; i < n; i++)
                for (int j = i + 1; j < n; j++) {
                    cp.add(Factory.makeDifferentVar(q[i], q[j]));
                    cp.add(Factory.makeDifferentVar(q[i], q[j],i-j));
                    cp.add(Factory.makeDifferentVar(q[i], q[j],j-i));
                }

            // count the number of solution (manually)
            Box<Integer>  nbSols = new Box<>(0);



            SearchStatistics stats = new DFSearch(cp.getContext(),
                    selectMin(q,
                        qi -> qi.getSize() > 1,
                        qi -> qi.getSize(),
                        qi -> {
                            int v = qi.getMin();
                            return branch(
                                    () -> { cp.add(new EqualVal(qi,v));},
                                    () -> { cp.add(new DifferentVal(qi,v));}
                            );
                        }
                    )
            ).onSolution( () ->
                    nbSols.set(nbSols.get() + 1)
            ).start();

            System.out.format("#Solutions: %s\n",nbSols);
            System.out.format("Statistics: %s\n",stats);
        } catch(InconsistencyException c) {
            System.out.println("inconsistency detected in the model");
        }
    }
}