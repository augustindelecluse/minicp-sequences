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

import minicp.state.StateManager;
import minicp.util.Procedure;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;

import java.util.*;
import java.util.function.Supplier;

public class DFSearch {

    private Supplier<Procedure[]> branching;
    private SearchObserver searchObserver;
    private StateManager sm;

    public DFSearch(StateManager sm, Supplier<Procedure[]> branching) {
        this.sm = sm;
        this.searchObserver = new AbstractSearcher() {};
        this.branching = branching;
    }

    public void onSolution(Procedure listener) {
        searchObserver.onSolution(listener);
    }

    public void onFailure(Procedure listener) {
        searchObserver.onFailure(listener);
    }

    private SearchStatistics solve(SearchStatistics statistics,SearchLimit limit) {
        sm.withNewState( ()-> {
                try {
                    dfs(statistics,limit);
                    statistics.completed = true;
                }
                catch (StopSearchException ignored) {}
                catch (StackOverflowError e) {
                    throw new NotImplementedException("dfs with explicit stack needed to pass this test");
                }
            });
        return statistics;
    }

    public SearchStatistics optimize(Objective obj) {
        SearchStatistics statistics = new SearchStatistics();
        return optimize(obj,stats -> false);
    }

    public SearchStatistics optimize(Objective obj, SearchLimit limit) {
        SearchStatistics statistics = new SearchStatistics();
        onSolution(() -> obj.tighten());
        return solve(statistics,limit);
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

    public SearchStatistics optimizeSubjectTo(Objective obj, SearchLimit limit, Procedure subjectTo) {
        SearchStatistics statistics = new SearchStatistics();
        sm.withNewState(() -> {
            try {
                subjectTo.call();
                optimize(obj,limit);
            } catch (InconsistencyException e) {}
        });
        return  statistics;
    }


    private void expandNode(Stack<Procedure> alternatives, SearchStatistics statistics) {
        Procedure[] alts = branching.get();
        if (alts.length == 0) {
            statistics.nSolutions++;
            searchObserver.notifySolution();
        } else {
            for (int i = alts.length-1; i >= 0; i--) {
                Procedure a = alts[i];
                alternatives.push(() -> sm.restore());
                alternatives.push(() -> {
                    statistics.nNodes++;
                    a.call();
                    expandNode(alternatives, statistics);
                });
                alternatives.push(() -> sm.save());
            }
        }
    }



    private void dfs2(SearchStatistics statistics, SearchLimit limit) {
        Stack<Procedure> alternatives = new Stack<Procedure>();
        expandNode(alternatives,statistics);
        while (!alternatives.isEmpty()) {
            if (limit.stopSearch(statistics)) throw new StopSearchException();
            try {
                alternatives.pop().call();
            } catch (InconsistencyException e) {
                statistics.nFailures++;
                searchObserver.notifyFailure();
            }
        }

    }

    private void dfs(SearchStatistics statistics, SearchLimit limit) {
        if (limit.stopSearch(statistics))
            throw new StopSearchException();
        Procedure[] branches = branching.get();
        if (branches.length == 0) {
            statistics.nSolutions++;
            searchObserver.notifySolution();
        }
        else {
            for (Procedure b : branches) {
                sm.withNewState( ()-> {
                        try {
                            statistics.nNodes++;
                            b.call();
                            dfs(statistics,limit);
                        } catch (InconsistencyException e) {
                            statistics.nFailures++;
                            searchObserver.notifyFailure();
                        }
                    });
            }
        }
    }
}
