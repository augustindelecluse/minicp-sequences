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
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import minicp.util.Procedure;

import java.util.Stack;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DFSearch {

    private Supplier<Procedure[]> branching;
    private SearchObserver searchObserver;
    private StateManager sm;

    public DFSearch(StateManager sm, Supplier<Procedure[]> branching) {
        this.sm = sm;
        this.searchObserver = new AbstractSearcher() {
        };
        this.branching = branching;
    }

    public void onSolution(Procedure listener) {
        searchObserver.onSolution(listener);
    }

    public void onFailure(Procedure listener) {
        searchObserver.onFailure(listener);
    }

    private SearchStatistics solve(SearchStatistics statistics, Predicate<SearchStatistics> limit) {
        sm.withNewState(() -> {
            try {
                dfs(statistics, limit);
                statistics.setCompleted();
            } catch (StopSearchException ignored) {
            } catch (StackOverflowError e) {
                throw new NotImplementedException("dfs with explicit stack needed to pass this test");
            }
        });
        return statistics;
    }

    public SearchStatistics optimize(Objective obj) {
        SearchStatistics statistics = new SearchStatistics();
        return optimize(obj, stats -> false);
    }

    public SearchStatistics optimize(Objective obj, Predicate<SearchStatistics> limit) {
        SearchStatistics statistics = new SearchStatistics();
        onSolution(() -> obj.tighten());
        return solve(statistics, limit);
    }


    public SearchStatistics solve() {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, stats -> false);
    }

    public SearchStatistics solve(Predicate<SearchStatistics> limit) {
        SearchStatistics statistics = new SearchStatistics();
        return solve(statistics, limit);
    }

    public SearchStatistics solveSubjectTo(Predicate<SearchStatistics> limit, Procedure subjectTo) {
        SearchStatistics statistics = new SearchStatistics();
        sm.withNewState(() -> {
            try {
                subjectTo.call();
                solve(statistics, limit);
            } catch (InconsistencyException e) {
            }
        });
        return statistics;
    }

    public SearchStatistics optimizeSubjectTo(Objective obj, Predicate<SearchStatistics> limit, Procedure subjectTo) {
        SearchStatistics statistics = new SearchStatistics();
        sm.withNewState(() -> {
            try {
                subjectTo.call();
                optimize(obj, limit);
            } catch (InconsistencyException e) {
            }
        });
        return statistics;
    }


    private void expandNode(Stack<Procedure> alternatives, SearchStatistics statistics) {
        Procedure[] alts = branching.get();
        if (alts.length == 0) {
            statistics.incrSolutions();
            searchObserver.notifySolution();
        } else {
            for (int i = alts.length - 1; i >= 0; i--) {
                Procedure a = alts[i];
                alternatives.push(() -> sm.restoreState());
                alternatives.push(() -> {
                    statistics.incrNodes();
                    a.call();
                    expandNode(alternatives, statistics);
                });
                alternatives.push(() -> sm.saveState());
            }
        }
    }


    private void dfs2(SearchStatistics statistics, Predicate<SearchStatistics> limit) {
        Stack<Procedure> alternatives = new Stack<Procedure>();
        expandNode(alternatives, statistics);
        while (!alternatives.isEmpty()) {
            if (limit.test(statistics)) throw new StopSearchException();
            try {
                alternatives.pop().call();
            } catch (InconsistencyException e) {
                statistics.incrFailures();
                searchObserver.notifyFailure();
            }
        }

    }

    private void dfs(SearchStatistics statistics, Predicate<SearchStatistics> limit) {
        if (limit.test(statistics))
            throw new StopSearchException();
        Procedure[] branches = branching.get();
        if (branches.length == 0) {
            statistics.incrSolutions();
            searchObserver.notifySolution();
        } else {
            for (Procedure b : branches) {
                sm.withNewState(() -> {
                    try {
                        statistics.incrNodes();
                        b.call();
                        dfs(statistics, limit);
                    } catch (InconsistencyException e) {
                        statistics.incrFailures();
                        searchObserver.notifyFailure();
                    }
                });
            }
        }
    }
}
