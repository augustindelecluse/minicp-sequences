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

import minicp.engine.constraints.TableDecomp;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.InconsistencyException;
import minicp.util.InputReader;

import java.util.Arrays;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.cp.Heuristics.firstFail;


public class Steel {


    public static void main(String[] args) throws InconsistencyException {

        // Reading the data

        InputReader reader = new InputReader("data/steel/bench_13_14");
        int nCapa = reader.getInt();
        int [] capa = new int[nCapa];
        for (int i = 0; i < nCapa; i++) {
            capa[i] = reader.getInt();
        }
        int maxCapa = capa[capa.length-1];
        int [] loss =  new int [maxCapa+1];
        int capaIdx = 0;
        for (int i = 0; i < maxCapa; i++) {
            loss[i] = capa[capaIdx]-i;
            if (loss[i] == 0) capaIdx++;
        }


        int nCol = reader.getInt();
        int nSlab = reader.getInt();
        int [] w = new int[nSlab];
        int [] c = new int[nSlab];
        for (int i = 0; i < nSlab; i++) {
            w[i] = reader.getInt();
            c[i] = reader.getInt();
        }

        // ---------------------------

        Solver cp = makeSolver();
        IntVar [] x = makeIntVarArray(cp,nSlab,nSlab);
        IntVar [] l = makeIntVarArray(cp,nSlab,maxCapa+1);

        for (int j = 0; j < nSlab; j++) {
            IntVar [] wj = new IntVar[nSlab];
            for (int i = 0; i < nSlab; i++) {
                wj[i] = mul(isEqual(x[i],j),w[i]);
            }
            cp.post(sum(wj,l[j]));
        }
        cp.post(sum(l,IntStream.of(w).sum()));

        System.out.println(Arrays.toString(loss));
        System.out.println(Arrays.toString(l));
        IntVar [] losses = makeIntVarArray(cp,nSlab,i -> element(loss,l[i]));
        IntVar totLoss = sum(losses);

        DFSearch dfs = makeDfs(cp,firstFail(x));

        cp.post(minimize(totLoss,dfs));

        dfs.onSolution(() -> {
            System.out.println("---");
            System.out.println(totLoss);
        });

        dfs.start();


    }
}