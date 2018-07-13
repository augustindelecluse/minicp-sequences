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

import minicp.util.Procedure;
import minicp.reversible.StateManager;
import minicp.reversible.Trail;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;

import java.util.*;

public class DFSearch {

    private BranchingScheme branching;
    private Trail trail;
    private StateManager sm;
    private List<Procedure> solutionListeners = new LinkedList<Procedure>();
    private List<Procedure> failListeners = new LinkedList<Procedure>();

    public DFSearch onSolution(Procedure listener) {
        solutionListeners.add(listener);
        return this;
    }

    public void notifySolutionFound() {
        solutionListeners.forEach(s -> s.call());
    }

    public DFSearch onFail(Procedure listener) {
        failListeners.add(listener);
        return this;
    }
    public void notifyFailure() {
        failListeners.forEach(s -> s.call());
    }

    public DFSearch(StateManager sm, BranchingScheme branching) {
        this.sm    = sm;
        this.trail = sm.getTrail();
        this.branching = branching;
    }

    public SearchStatistics solve(SearchLimit limit) {
        SearchStatistics statistics = new SearchStatistics();
        int level = trail.getLevel();

        try {
            dfs(statistics,limit);
            statistics.completed = true;
        }
        catch (StopSearchException ignored) {}
        catch (StackOverflowError e) {
            throw new NotImplementedException("dfs with explicit stack needed to pass this test");
        }
        trail.popUntil(level);
        return statistics;
    }

    public SearchStatistics solve() {
        return solve(statistics -> false);
    }

    public SearchStatistics solveSubjectTo(SearchLimit limit, Procedure subjectTo) {
        SearchStatistics statistics = new SearchStatistics();
        trail.push();
        try {
            subjectTo.call();
            statistics = solve(limit);
        } catch (InconsistencyException e) {}
        trail.pop();
        return  statistics;
    }


    private void expandNode(Stack<Branch> alternatives, SearchStatistics statistics) {
        Branch[] alts = branching.call();
        if (alts.length == 0) {
            statistics.nSolutions++;
            notifySolutionFound();
        } else {
            for (int i = alts.length-1; i >= 0; i--) {
                Branch a = alts[i];
                alternatives.push(() -> trail.pop());
                alternatives.push(() -> {
                    statistics.nNodes++;
                    a.call();
                    expandNode(alternatives, statistics);
                });
                alternatives.push(() -> trail.push());
            }
        }
    }

    private void dfs(SearchStatistics statistics, SearchLimit limit) {
        Stack<Branch> alternatives = new Stack<Branch>();
        expandNode(alternatives,statistics);
        while (!alternatives.isEmpty()) {
            if (limit.stopSearch(statistics)) throw new StopSearchException();
            try {
                alternatives.pop().call();
            } catch (InconsistencyException e) {
                notifyFailure();
                statistics.nFailures++;
            }
        }

    }

    private void dfs2(SearchStatistics statistics, SearchLimit limit) {
        if (limit.stopSearch(statistics))
            throw new StopSearchException();
        Branch[] branches = branching.call();
        if (branches.length == 0) {
            statistics.nSolutions++;
            notifySolutionFound();
        }
        else {
            for (Branch b : branches) {
                sm.withNewState( ()-> {
                        try {
                            statistics.nNodes++;
                            b.call();
                            dfs2(statistics,limit);
                        } catch (InconsistencyException e) {
                            notifyFailure();
                            statistics.nFailures++;
                        }
                    });
            }
        }
    }
}



