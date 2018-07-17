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
import java.util.function.Supplier;

public class DFSearch {

    private Supplier<Procedure[]> branching;
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

    public DFSearch(StateManager sm, Supplier<Procedure[]> branching) {
        this.sm    = sm;
        this.trail = sm.getTrail();
        this.branching = branching;
    }

    private SearchStatistics solve(SearchStatistics statistics,SearchLimit limit) {
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
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics,stats -> false);
    }
    public SearchStatistics solve(SearchLimit limit) {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics,limit);
    }

    public SearchStatistics solveSubjectTo(SearchLimit limit, Procedure subjectTo) {
        SearchStatistics statistics = new SearchStatistics();
        sm.withNewState(() -> {
            try {
                subjectTo.call();
                solve(statistics,limit);
            } catch (InconsistencyException e) {}
        });
        return  statistics;
    }


    private void expandNode(Stack<Procedure> alternatives, SearchStatistics statistics) {
        Procedure[] alts = branching.get();
        if (alts.length == 0) {
            statistics.nSolutions++;
            notifySolutionFound();
        } else {
            for (int i = alts.length-1; i >= 0; i--) {
                Procedure a = alts[i];
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
        Stack<Procedure> alternatives = new Stack<Procedure>();
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
        Procedure[] branches = branching.get();
        if (branches.length == 0) {
            statistics.nSolutions++;
            notifySolutionFound();
        }
        else {
            for (Procedure b : branches) {
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



