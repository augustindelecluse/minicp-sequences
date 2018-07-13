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

package minicp.examples;

import minicp.cp.Factory;
import minicp.engine.constraints.IsOr;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.InputReader;
import minicp.util.Procedure;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.search.Selector.branch;
import static minicp.search.Selector.selectMin;


public class Steel {


    public static void main(String[] args)  {

        // Reading the data

        InputReader reader = new InputReader("data/steel/bench_20_0");
        int nCapa = reader.getInt();
        int[] capa = new int[nCapa];
        for (int i = 0; i < nCapa; i++) {
            capa[i] = reader.getInt();
        }
        int maxCapa = capa[capa.length - 1];
        int[] loss = new int[maxCapa + 1];
        int capaIdx = 0;
        for (int i = 0; i < maxCapa; i++) {
            loss[i] = capa[capaIdx] - i;
            if (loss[i] == 0) capaIdx++;
        }
        loss[0] = 0;

        int nCol = reader.getInt();
        int nSlab = reader.getInt();
        int nOrder = nSlab;
        int[] w = new int[nSlab];
        int[] c = new int[nSlab];
        for (int i = 0; i < nSlab; i++) {
            w[i] = reader.getInt();
            c[i] = reader.getInt()-1;
        }

        // ---------------------------

        try {


            Solver cp = makeSolver();
            IntVar[] x = makeIntVarArray(cp, nOrder, nSlab);
            IntVar[] l = makeIntVarArray(cp, nSlab, maxCapa + 1);

            BoolVar[][] inSlab = new BoolVar[nSlab][nOrder]; // inSlab[j][i] = 1 if order i is placed in slab j

            for (int j = 0; j < nSlab; j++) {
                for (int i = 0; i < nOrder; i++) {
                    inSlab[j][i] = isEqual(x[i], j);
                }
            }


            for (int j = 0; j < nSlab; j++) {
                // for each color, is it present in the slab
                IntVar[] presence = new IntVar[nCol];

                for (int col = 0; col < nCol; col++) {
                    presence[col] = makeBoolVar(cp);

                    ArrayList<BoolVar> inSlabWithColor = new ArrayList<>();
                    for (int i = 0; i < nOrder; i++) {
                        if (c[i] == col) inSlabWithColor.add(inSlab[j][i]);
                    }

                    // TODO: model that presence[col] is true iff at least one order with color col is placed in slab j

                    cp.post(new IsOr((BoolVar) presence[col],inSlabWithColor.toArray(new BoolVar[0])));

                }
                // TODO: restrict the number of colors present in slab j to be <= 2
                lessOrEqual(sum(presence),2);
            }


            // bin packing constraint
            for (int j = 0; j < nSlab; j++) {
                IntVar[] wj = new IntVar[nSlab];
                for (int i = 0; i < nOrder; i++) {
                    wj[i] = mul(inSlab[j][i], w[i]);
                }
                cp.post(sum(wj, l[j]));
            }

            // TODO: add the redundant constraint that the sum of the loads is equal to the sum of elements
            cp.post(sum(l, IntStream.of(w).sum()));
            System.out.println("total weights of items:"+IntStream.of(w).sum());


            // TODO: model the objective function using element constraint + a sum constraint
            IntVar[] losses = Factory.makeIntVarArray(nSlab, j -> element(loss, l[j]));
            IntVar totLoss = sum(losses);

            //DFSearch dfs = makeDfs(cp,firstFail(x));


            // TODO add static symmetry breaking constraint

            // TODO implement a dynamic symmetry breaking during search
            DFSearch dfs = makeDfs(cp,
                    selectMin(x,
                            xi -> xi.getSize() > 1,
                            xi -> xi.getSize(),
                            xi -> {
                                int maxUsed = -1;
                                for (IntVar x_ : x) {
                                    if (x_.isBound() && x_.getMin() > maxUsed) {
                                        maxUsed = x_.getMin();
                                    }
                                }
                                Procedure[] branches = new Procedure[maxUsed+2];
                                for (int i = 0; i <= maxUsed+1; i++) {
                                    int slab = i;
                                    branches[i] = () -> equal(xi,slab);
                                }
                                return branch(branches);
                            }
                    )
            );

            cp.post(minimize(totLoss, dfs));

            dfs.onSolution(() -> {
                System.out.println("---");
                System.out.println(totLoss);

                Set<Integer>[] colorsInSlab = new Set[nSlab];
                for (int j = 0; j < nSlab; j++) {
                    colorsInSlab[j] = new HashSet<>();
                }
                for (int i = 0; i < nOrder; i++) {
                    colorsInSlab[x[i].getMin()].add(c[i]);
                }
                for (int j = 0; j < nSlab; j++) {
                    if (colorsInSlab[j].size() > 2) {
                        System.out.println("problem, "+colorsInSlab[j].size()+" colors in slab "+j+" should be <= 2");
                    }
                }
            });

            SearchStatistics statistics = dfs.solve();
            System.out.println(statistics);

        } catch (InconsistencyException e) {
            e.printStackTrace();

        }
    }
}
