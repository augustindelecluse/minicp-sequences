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
import minicp.engine.constraints.Profile.Rectangle;
import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.InconsistencyException;

import java.util.ArrayList;

import static minicp.cp.Factory.minus;
import static minicp.cp.Factory.plus;

public class Cumulative extends AbstractConstraint {

    private final IntVar[] start;
    private final int[] duration;
    private final IntVar[] end;
    private final int[] demand;
    private final int capa;
    private final boolean postMirror;


    public Cumulative(IntVar[] start, int[] duration, int[] demand, int capa) {
        this(start, duration, demand, capa, true);
    }

    private Cumulative(IntVar[] start, int[] duration, int[] demand, int capa, boolean postMirror) {
        super(start[0].getSolver());
        this.start = start;
        this.duration = duration;
        this.end = Factory.makeIntVarArray(start.length, i -> plus(start[i], duration[i]));
        this.demand = demand;
        this.capa = capa;
        this.postMirror = postMirror;
    }


    @Override
    public void post() {
        for (int i = 0; i < start.length; i++) {
            start[i].propagateOnBoundChange(this);
        }

        if (postMirror) {
            IntVar[] startMirror = Factory.makeIntVarArray(start.length, i -> minus(end[i]));
            cp.post(new Cumulative(startMirror, duration, demand, capa, false), false);
        }

        propagate();
    }

    @Override
    public void propagate() {
        Profile profile = buildProfile();
        for (int i = 0; i < profile.size(); i++) {
            if (profile.get(i).height > capa) {
                throw InconsistencyException.INCONSISTENCY;
            }
        }

        for (int i = 0; i < start.length; i++) {
            if (!start[i].isBound()) {
                int j = profile.rectangleIndex(start[i].getMin());
                int t = start[i].getMin();
                while (j < profile.size()
                        && profile.get(j).start < Math.min(t + duration[i], start[i].getMax())) {
                    if (capa - demand[i]
                          <  profile.get(j).height) {
                        t = Math.min(profile.get(j).end, start[i].getMax());
                    }
                    j++;
                }
                start[i].removeBelow(t);
            }
        }
    }

    public Profile buildProfile() {
        ArrayList<Rectangle> mandatoryParts = new ArrayList<Rectangle>();
        for (int i = 0; i < start.length; i++) {
            if (end[i].getMin() > start[i].getMax()) {
                int s = start[i].getMax();
                int e = end[i].getMin();
                int d = demand[i];
                mandatoryParts.add(new Rectangle(s, e, d));
            }
        }
        return new Profile(mandatoryParts.toArray(new Profile.Rectangle[0]));
    }

}
