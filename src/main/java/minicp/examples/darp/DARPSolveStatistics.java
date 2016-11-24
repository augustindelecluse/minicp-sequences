package minicp.examples.darp;

import minicp.search.SearchStatistics;
import minicp.util.Procedure;

import java.util.*;
import java.util.stream.Collectors;

import static  java.util.Arrays.asList;

public class DARPSolveStatistics {

    private double initTime;
    private final double maxRunTime;
    private double elapsedTime = .0;
    private boolean finished = false;
    private SearchStatistics searchStatistics = null;
    private ArrayList<SolutionWithTime> solutions = new ArrayList<>();

    private class SolutionWithTime {

        DARPInstance.DARPSolution sol;
        double timeFound;

        private SolutionWithTime(DARPInstance.DARPSolution sol, double timeFound) {
            this.sol = sol;
            this.timeFound = timeFound;
        }

        @Override
        public String toString() {
            return String.format("\tt = %.3f [s] \t obj = %.3f", timeFound - DARPSolveStatistics.this.initTime, sol.computeNoExcept());
        }
    };

    public DARPSolveStatistics() {
        this(Double.MAX_VALUE);
    }

    public DARPSolveStatistics(double maxRunTime) {
        this.maxRunTime = maxRunTime;
    }

    public double getMaxRunTime() {
        return maxRunTime;
    }

    private double getCurrentTime() {
        return (double) System.currentTimeMillis() / 1000;
    }

    public void init() {
        initTime = getCurrentTime();
        elapsedTime = 0;
    }

    public boolean isFinished() {
        return finished || getCurrentTime() - initTime >= maxRunTime;
    }

    public void finish() {
        elapsedTime = getCurrentTime() - initTime;
        finished = true;
    }

    public void setElapsedTime(double elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public double getElapsedTime() {
        return getCurrentTime();
    }

    public void addSolution(DARPInstance.DARPSolution solution) {
        solutions.add(new SolutionWithTime(solution, getElapsedTime()));
    }

    public void resetSolution() {
        this.solutions.clear();
    }

    public int getNUniqueSolution() {
        return solutions.stream().map(solutionWithTime -> solutionWithTime.sol.orderedRoute()).collect(Collectors.toSet()).size();
    }

    public int getNSolutions() {return solutions.size();}

    public void setSearchStatistics(SearchStatistics searchStatistics) {
        this.searchStatistics = searchStatistics;
    }

    public SearchStatistics getSearchStatistics() {
        return searchStatistics;
    }

    public DARPInstance.DARPSolution bestSolution() {
        return solutions.stream().map(solutionWithTime -> solutionWithTime.sol).min(DARPInstance.DARPSolution::compareTo).orElse(null);
    }

    public DARPInstance.DARPSolution firstSolution() {
        return solutions.size() == 0 ? null : solutions.get(0).sol;
    }

    @Override
    public String toString() {
        String informations;
        String maxTime = maxRunTime == Double.MAX_VALUE ? "" : String.format(" / %.3f", maxRunTime);
        DARPInstance.DARPSolution bestSol = bestSolution();
        String bestObj = bestSol == null ? "" : String.format("\n\tBest objective: %.3f", bestSol.computeNoExcept());;
        informations = String.format("\n\tTime: %.3f%s [s]%s\n\t#Solutions found: %d\n\tUnique solutions found: %d",
                elapsedTime, maxTime, bestObj, getNSolutions(), getNUniqueSolution());
        if (searchStatistics != null) {
            return String.format("%s\n%s", informations, searchStatistics);
        }
        return informations;
    }

    public String solOverTime() {
        return solutions.stream().map(Object::toString).collect(Collectors.joining("\n"));
    }

    public void solveWithProc(Procedure f) {
        init();
        f.call();
        finish();
    }

}
