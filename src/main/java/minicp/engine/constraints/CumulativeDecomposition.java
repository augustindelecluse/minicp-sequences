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

import minicp.engine.core.BoolVar;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;

import java.util.Arrays;

import static minicp.cp.Factory.*;

public class CumulativeDecomposition implements Constraint {

    private final IntVar[] start;
    private final int[] duration;
    private final IntVar[] end;
    private final int[] demand;
    private final int capa;
    private final Solver cp;


    public CumulativeDecomposition(IntVar[] start, int[] duration, int[] demand, int capa)  {
        this.cp = start[0].getSolver();
        this.start = start;
        this.duration = duration;
        this.end = makeIntVarArray(cp,start.length, i -> plus(start[i],duration[i]));
        this.demand = demand;
        this.capa = capa;
    }

    @Override
    public void post()  {

        int min = Arrays.stream(start).map(s-> s.getMin()).min(Integer::compare).get();
        int max = Arrays.stream(end).map(e-> e.getMax()).max(Integer::compare).get();

        for (int t = min; t < max; t++) {

            BoolVar[] overlaps = new BoolVar[start.length];
            for (int i = 0; i < start.length; i++) {
                overlaps[i] = makeBoolVar(cp);
                BoolVar startsBefore = makeBoolVar(cp);
                BoolVar endsAfter = makeBoolVar(cp);

                cp.post(new IsLessOrEqual(startsBefore,start[i],t));

                cp.post(new IsLessOrEqual(endsAfter,minus(plus(start[i],duration[i]-1)),-t));

                // overlaps = endsAfter & startsBefore
                cp.post(new IsLessOrEqual(overlaps[i],minus(sum(new IntVar[]{startsBefore,endsAfter})),-2));
            }

            IntVar[] overlapHeights = makeIntVarArray(cp,start.length,i -> mul(overlaps[i],demand[i]));
            IntVar cumHeight = sum(overlapHeights);
            cumHeight.removeAbove(capa);

        }

    }

}
