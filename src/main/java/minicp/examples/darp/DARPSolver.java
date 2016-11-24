package minicp.examples.darp;

import java.util.Random;

public abstract class DARPSolver {

    /**
     * return the first solution found within a given max run time.
     * @param instance instance to solve
     * @param darpSolveStatistics solve statistics, containing the maximum allowed run time before the search
     *                            and statistics related to the search after
     * @return best solution found within maxRunTime. null if no solution found. Set the value for elapsedTime
     */
    public abstract DARPInstance.DARPSolution solve(DARPInstance instance, DARPSolveStatistics darpSolveStatistics);

    /**
     * solve an instance using LNS
     * @param instance instance to solve
     * @param darpSolveStatistics solve statistics, containing the maximum allowed run time before the search
     *                            and statistics related to the search after
     * @return best solution found within maxRunTime. null if no solution found
     */
    public abstract DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPSolveStatistics darpSolveStatistics);

    /**
     * solve an instance using LNS and a first feasible solution
     * @param instance instance to solve
     * @param initSolution feasible solution that can be used to find a better one using LNS
     * @param darpSolveStatistics solve statistics, containing the maximum allowed run time before the search
     *                            and statistics related to the search after
     * @return best solution found within maxRunTime
     */
    public abstract DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPInstance.DARPSolution initSolution, DARPSolveStatistics darpSolveStatistics);

    /**
     * name used when benchmarking the solver. Should not contain space, / or .
     * @return name used when benchmarking the solver
     */
    public abstract String description();

    /**
     * completely solve an instance to optimality
     * @param instance instance to solve
     * @param darpSolveStatistics solve statistics, containing the maximum allowed run time before the search
     *                            and statistics related to the search after
     */
    public abstract void solveAll(DARPInstance instance, DARPSolveStatistics darpSolveStatistics);

    private int seed;
    private Random random = new Random();

    public void setSeed(int seed) {
        this.seed = seed;
        this.random = new Random(seed);
    };

    public int getSeed() {
        return seed;
    };

    public int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public float nextFloat() {
        return random.nextFloat();
    }

    protected int version = 0;
    protected int maxVersion = 0;
    private int verbosity = 0;
    private boolean versionSet = false;

    public int defaultVersion() {
        return 0;
    }

    public int getVersion() {
        if (versionSet)
            return version;
        return defaultVersion();
    }

    public void setVersion(int version) {
        assert version >= 0 && version <= getMaxVersion();
        versionSet = true;
        this.version = version;
    }

    /**
     * maximum version, included
     * @return maximum version
     */
    public int getMaxVersion() {
        return maxVersion;
    }

    public String toString() {
        return description() + '_' + getVersion();
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

}
