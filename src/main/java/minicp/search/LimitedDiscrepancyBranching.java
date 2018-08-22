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

package minicp.search;


import minicp.cp.BranchingScheme;
import minicp.util.Procedure;

import java.util.function.Supplier;

/**
 * Branching wrapper that ensures that
 * that the alternatives created are always within the
 * discrepancy limit
 */

public class LimitedDiscrepancyBranching implements Supplier<Procedure[]> {

    private int curD;
    private final int maxD;
    private final Supplier<Procedure[]> bs;

    public LimitedDiscrepancyBranching(Supplier<Procedure[]> bs, int maxDiscrepancy) {
        if (maxDiscrepancy < 0) throw new IllegalArgumentException("max discrepancy should be >= 0");
        this.bs = bs;
        this.maxD = maxDiscrepancy;
    }

    @Override
    public Procedure[] get() {
        // Hint:
        // Filter-out alternatives from that would exceed maxD
        // Therefore wrap each alternative
        // such that the call method of the wrapped alternatives
        // augment the curD depending on its position
        // +0 for alts[0], ..., +i for alts[i]
        //throw new NotImplementedException();

        Procedure[] branches = bs.get();

        int k = Math.min(maxD - curD + 1, branches.length);

        if (k == 0) return BranchingScheme.EMPTY;

        Procedure[] branches_k = new Procedure[k];
        for (int i = 0; i < k; i++) {
            int bi = i;
            int d = curD + bi; // branch index
            branches_k[i] = () -> {
                curD = d; // update discrepancy
                branches[bi].call();
            };
        }

        return branches_k;

    }
}
