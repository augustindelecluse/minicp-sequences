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
import minicp.util.InconsistencyException;


import java.util.Arrays;
import java.util.Comparator;

import static minicp.cp.Factory.*;

public class Disjunctive extends Constraint {

    private final IntVar[] start;
    private final int[] duration;
    private final IntVar[] end;

    private final Integer [] permEst;
    private final int[] rankEst;
    private final int[] min_;
    private final Integer [] permLct;
    private final Integer [] permLst;
    private final Integer [] permEct;

    private final ThetaTree thetaTree;

    private final boolean postMirror;

    public Disjunctive(IntVar[] start, int[] duration) throws InconsistencyException {
        this(start,duration,true);
    }

    private Disjunctive(IntVar[] start, int[] duration, boolean postMirror) throws InconsistencyException {
        super(start[0].getSolver());
        this.postMirror = postMirror;
        this.start = start;
        this.duration = duration;
        this.end = makeIntVarArray(cp,start.length, i -> plus(start[i],duration[i]));
        permEst = new Integer[start.length];
        rankEst = new int[start.length];
        permLct = new Integer[start.length];
        permLst = new Integer[start.length];
        permEct = new Integer[start.length];
        for (int i = 0; i < start.length; i++) {
            permEst[i] = i;
            permLct[i] = i;
            permLst[i] = i;
            permEct[i] = i;
        }
        thetaTree = new ThetaTree(start.length);

        min_ = new int[start.length];
    }


    @Override
    public void post() throws InconsistencyException {

        int [] demands = new int[start.length];
        for (int i = 0; i < start.length; i++) {
            demands[i] = 1;
        }
        cp.post(new Cumulative(start,duration,demands,1),false);

        // TODO 1: replace by  posting  binary decomposition using IsLessOrEqualVar

        for (int i = 0; i < start.length; i++) {
            start[i].propagateOnBoundChange(this);
        }

        if (postMirror) {
            for (int i = 0; i < start.length; i++) {
                IntVar end_i = plus(start[i], duration[i]);
                for (int j = i + 1; j < start.length; j++) {
                    IntVar end_j = plus(start[j], duration[j]);
                    BoolVar iBeforej = makeBoolVar(cp);
                    BoolVar jBeforei = makeBoolVar(cp);

                    cp.post(new IsLessOrEqualVar(iBeforej, end_i, start[j]));
                    cp.post(new IsLessOrEqualVar(jBeforei, end_j, start[i]));
                    cp.post(new NotEqual(iBeforej, jBeforei), false);

                    // TODO i before j or j before i
                }
            }


            IntVar[] startMirror = makeIntVarArray(cp, start.length, i -> minus(end[i]));
            cp.post(new Disjunctive(startMirror, duration, false), false);
        }

        // TODO 2: replace by adding propagation OverLoadCheck, NotFirst-NotLast, Detectable Precedences, Edge Finding ...
    }

    @Override
    public void propagate() throws InconsistencyException {
        //System.out.println("=============");
        Arrays.sort(permEst, Comparator.comparingInt(i -> start[i].getMin()));
        for (int i = 0; i < start.length; i++) {
            //System.out.println(start[permEst[i]].getMin());
            rankEst[permEst[i]] = i;
            min_[i] = start[i].getMin();
        }


        // overload checker
        Arrays.sort(permLct,Comparator.comparingInt(i -> end[i].getMax()));
        thetaTree.reset();
        for (int i = 0; i < start.length; i++) {
            int activity = permLct[i];
            thetaTree.insert(rankEst[activity],end[activity].getMin(),duration[activity]);
            if (thetaTree.getECT() > end[activity].getMax()) {
                throw new InconsistencyException();
            }
        }

        // detectable precedences

        Arrays.sort(permLst,Comparator.comparingInt(i -> start[i].getMax()));
        Arrays.sort(permEct,Comparator.comparingInt(i -> end[i].getMin()));
        boolean [] inserted = new boolean[start.length];
        int idx_j = 0;
        int j = permLst[idx_j];
        thetaTree.reset();
        for (int act_i: permEct) {
            while (idx_j < start.length && end[act_i].getMin() > start[permLst[idx_j]].getMax()) {
                j = permLst[idx_j];
                inserted[j] = true;
                thetaTree.insert(rankEst[j],end[j].getMin(),duration[j]);
                idx_j++;
            }
            if (inserted[act_i]) {
                thetaTree.remove(rankEst[act_i]);
                min_[act_i] = Math.max(min_[act_i],thetaTree.getECT());
                thetaTree.insert(rankEst[act_i],end[act_i].getMin(),duration[act_i]);
            } else {
                min_[act_i] = Math.max(min_[act_i],thetaTree.getECT());
            }
        }

        for (int i = 0; i < start.length; i++) {
            start[i].removeBelow(min_[i]);
        }


        super.propagate();


    }
}
