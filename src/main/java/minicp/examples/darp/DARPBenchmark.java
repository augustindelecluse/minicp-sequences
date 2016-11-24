package minicp.examples.darp;

import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Semaphore;

/**
 * run the registered solvers for all instances found in the folder
 * save the performances of each solver within a text file
 */
public class DARPBenchmark {

    private final double stateOfTheArtMaxTime = 300 * 3; // they have used 15 minutes instead of 5 minutes in the LNSFFPA paper

    public InstanceFile[] stateOfTheArtInstanceFiles = new InstanceFile[] {
            new InstanceFile("data/darp/Cordeau2003/a3-24.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a4-36.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a5-48.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a6-72.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a7-72.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a8-108.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a9-96.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a10-144.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a11-120.txt", stateOfTheArtMaxTime),
            new InstanceFile("data/darp/Cordeau2003/a13-144.txt", stateOfTheArtMaxTime),
    };

    // --------------------------------------------------------- parameters for runs

    public int nRun = 10; // number of runs to use on each instance
    public InstanceFile[] instanceFiles = new InstanceFile[] {
            //new InstanceFile("data/darp/Cordeau2003/a8-108.txt", 5.0),
            new InstanceFile("data/darp/simpleInstance2.txt", 60.0),
    };
    public SolverProvider[] solverToUses = new SolverProvider[] {
            DARPLnsFfpa::new,
            //DARPInsertion::new,
    };
    private boolean allVersion = false;
    private boolean parallelRun = true;

    final static String experimentFolder = "data/darp/results";  // where to write the results of the experiments
    final static String initSolFolder = "data/darp/init_solutions"; // where to write / get a set of initial solutions

    // --------------------------------------------------------- end of parameters for runs
    public void setAllVersion(boolean b) {
        allVersion = b;
    }

    public boolean getAllVersion() {
        return allVersion;
    }

    public void setParallelRun(boolean b) {
        parallelRun = b;
    }

    public boolean getParallelRun() {
        return parallelRun;
    }

    public int getnRun() {
        return nRun;
    }

    public void setnRun(int nRun) {
        this.nRun = nRun;
    }

    public InstanceFile[] getInstanceFiles() {
        return instanceFiles;
    }

    public void setInstanceFiles(InstanceFile[] instanceFiles) {
        this.instanceFiles = instanceFiles;
    }

    public SolverProvider[] getSolverToUses() {
        return solverToUses;
    }

    public void setSolverToUses(SolverProvider[] solverToUses) {
        this.solverToUses = solverToUses;
    }

    // concurrent parameters
    private final Semaphore writerSemaphore = new Semaphore(1);
    private final HashMap<String, Integer> instanceRun = new HashMap<>();
    private final HashMap<String, String> initSolutions = new HashMap<>();

    public DARPBenchmark() {
        File[] initSolFiles = new File(initSolFolder).listFiles();
        if (initSolFiles != null) {
            for (File file : initSolFiles) {
                initSolutions.put(file.getName().replace(".txt", ""), file.getAbsolutePath());
            }
        }
    }

    // instance to solve and corresponding max time allowed (in seconds)
    public record InstanceFile(String instancePath, Double maxTime) {

        String getName() {
            return Paths.get(instancePath).getFileName().toString().replace(".txt","");
        }

    }

    private record FullExperiment(DARPSolver solver, String experiment_name, InstanceFile file, DARPInstance darpInstance) { }

    public interface SolverProvider {
        DARPSolver get(DARPInstance instance);
    }

    private DARPInstance.DARPSolution getInitSolution(InstanceFile file, DARPInstance instance) {
        String name = file.getName();
        if (initSolutions.containsKey(name)) {
            try {
                return instance.firstSolutionFromFile(initSolutions.get(name));
            } catch (IOException e) {
                System.err.println("cannot read solution for " + name);
                return null;
            }
        }
        return null;
    }

    private int getRun(String instance) {
        int run = instanceRun.getOrDefault(instance, 1);
        instanceRun.put(instance, run + 1);
        return run;
    }

    private int getRunConcurrent(String instance) {
        int run = -1;
        try {
            writerSemaphore.acquire();
            run = instanceRun.getOrDefault(instance, 1);
            instanceRun.put(instance, run + 1);
            writerSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return run;
    }

    private void WithWriterSemaphore(Procedure procedure) {
        try {
            writerSemaphore.acquire();
            procedure.call();
            writerSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(String filePath, String detail) {
        writeToFile(filePath, detail, true);
    }

    private void writeToFile(String filePath, String detail, boolean append) {
        try {
            FileWriter writer = new FileWriter(filePath, append);
            writer.write(detail);
            writer.close();
        } catch (IOException e) {
            System.out.println("error when writing to file " + filePath);
        }
    }

    private void writeToFileConcurrent(String filePath, String detail, boolean append) {
        try {
            writerSemaphore.acquire();
            try {
                FileWriter writer = new FileWriter(filePath, append);
                writer.write(detail);
                writer.close();
            } catch (IOException e) {
                System.out.println("error when writing to file " + filePath);
            }
            writerSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void writeToFileConcurrent(String filePath, String detail) {
        writeToFileConcurrent(filePath, detail, true);
    }

    /**
     * run benchmark for
     * - initial solution
     * - lns solution
     * - lns solution with initial solution provided
     */
    public void allBenchmark() {
        benchMark_solve();
        benchMark_solveLNS();
        benchMark_solveLNSInitSol();
    }

    public void benchMark_solve() {
        if (parallelRun)
            new RunSolve().concurrentExperiment();
        else
            new RunSolve().experiment();
    }

    public void benchMark_solveAll() {
        if (parallelRun)
            new RunSolveAll().concurrentExperiment();
        else
            new RunSolveAll().experiment();
    }

    public void benchMark_solveLNS() {
        if (parallelRun)
            new RunSolveLNS().concurrentExperiment();
        else
            new RunSolveLNS().experiment();
    }

    public void benchMark_solveLNSInitSol() {
        if (parallelRun)
            new RunSolveLNSInitSol().concurrentExperiment();
        else
            new RunSolveLNSInitSol().experiment();
    }

    public void findInitSol() {
        if (parallelRun)
            new FindInitSolutions().concurrentExperiment();
        else
            new FindInitSolutions().experiment();
    }

    public String getCurrentLocalDateTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm"));
    }

    private abstract class Run {

        private int maxParallel;

        public Run() {
            maxParallel = Runtime.getRuntime().availableProcessors() - 1;
        }

        protected abstract String additionalInfo();

        protected boolean useInitSolution() { return false; }

        protected boolean detail() {
            return true;
        }

        // run a bunch of experiments on the current thread
        protected void experiment() {
            String experimentPattern = experimentFolder + '/' + getCurrentLocalDateTimeStamp() + "_%s_%s.txt";
            int maxVersion = 0;
            for (SolverProvider solverToUse: solverToUses) {
                DARPSolver baseSolver = null;
                int i = 0;
                for (InstanceFile instanceFile: instanceFiles) {
                    DARPInstance baseInstance = new DARPInstance(instanceFile.instancePath);
                    for (int v = 0; v <= maxVersion; ++v) {
                        for (int j = 0; j < nRun; ++j) {
                            DARPInstance instance = baseInstance.copy();
                            DARPSolver solver = solverToUse.get(instance);
                            if (baseSolver == null) {
                                baseSolver = solver;
                                maxVersion = allVersion ? baseSolver.getMaxVersion() : 0;
                            }
                            if (allVersion)
                                solver.setVersion(v);
                            String experiment = String.format(experimentPattern, instanceFile.getName(), solver.toString());
                            concurrentRun(new FullExperiment(solver, experiment, instanceFile, instance));
                        }
                    }
                }
            }
        }

        // run a bunch of experiments on as many threads as possible
        protected void concurrentExperiment() {
            String experimentPattern = experimentFolder + '/' + getCurrentLocalDateTimeStamp() + "_%s_%s.txt";
            int maxVersion = 0;
            int maxListSize = 2 * maxParallel;
            System.out.println("using " + maxParallel + " concurrent threads at disposal");
            for (SolverProvider solverToUse: solverToUses) {
                DARPSolver baseSolver = null;
                // prepare the data
                ArrayList<FullExperiment> allExperiments = new ArrayList<>();
                int i = 0;
                for (InstanceFile instanceFile: instanceFiles) {
                    DARPInstance baseInstance = new DARPInstance(instanceFile.instancePath);
                    for (int v = 0; v <= maxVersion; ++v) {
                        for (int j = 0; j < nRun; ++j) {
                            DARPInstance instance = baseInstance.copy();
                            DARPSolver solver = solverToUse.get(instance);
                            if (nRun > 1) {
                                int seed = new Random().nextInt();
                                solver.setSeed(seed);
                            }
                            if (baseSolver == null) {
                                baseSolver = solver;
                                maxVersion = allVersion ? baseSolver.getMaxVersion() : 0;
                            }
                            if (allVersion)
                                solver.setVersion(v);
                            String experiment = String.format(experimentPattern, instanceFile.getName(), solver.toString());
                            allExperiments.add(new FullExperiment(solver, experiment, instanceFile, instance));
                            if (allExperiments.size() == maxListSize) { // run buffer of simulations to prevent heap space explosion
                                System.out.println("running bunch of " + allExperiments.size() + " solvers");
                                allExperiments.stream().parallel().forEach(this::concurrentRun);
                                System.out.println("bunch of solvers ended");
                                allExperiments.clear();
                            }
                        }
                    }
                }
                // run the simulations
                System.out.println("running bunch of " + allExperiments.size() + " solvers");
                allExperiments.stream().parallel().forEach(this::concurrentRun);
                System.out.println("bunch of solvers ended");
            }
        }

        protected void concurrentRun(FullExperiment experiment) {
            DARPSolveStatistics solveStatistics = new DARPSolveStatistics(experiment.file.maxTime);
            DARPSolver solver = experiment.solver;
            int seed = solver.getSeed();
            String seedInfo = seed != 0 ? String.format(" with seed = %11d", seed) : "";
            //solver.setSeed(seed);
            DARPInstance.DARPSolution initSolution = useInitSolution() ? getInitSolution(experiment.file, experiment.darpInstance) : null;
            String additionalInfo = "";
            try {
                solve(solver, experiment.darpInstance, solveStatistics, initSolution);
            } catch (InconsistencyException e) {
                additionalInfo = "\n\tthrew inconsistency";
            }
            String finalAdditionalInfo = additionalInfo;
            WithWriterSemaphore(() -> {
                int run = getRun(experiment.experiment_name());
                String info = "";
                if (run == 1) { // first run
                    info += solver.toString() + additionalInfo() +  '\n';
                }
                info += String.format("run %d / %d%s: %s\n", run, nRun, seedInfo, solveStatistics.toString().replace('\n',  ' '));
                if (detail())
                    info += solveStatistics.solOverTime() + '\n';
                info += finalAdditionalInfo;
                System.out.println("solving ended for run " + run + " on " + experiment.file.getName());
                writeToFile(experiment.experiment_name , info);
            });
        }

        protected abstract void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                                      DARPInstance.DARPSolution initSolution);
    }

    private class RunSolve extends Run {

        @Override
        protected String additionalInfo() {
            return " first solution only ";
        }

        @Override
        protected void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                             DARPInstance.DARPSolution initSolution) {
            solver.solve(instance, solveStatistics);
        }

    }

    protected class RunSolveAll extends Run {

        @Override
        protected String additionalInfo() {
            return " all solutions ";
        }

        @Override
        protected boolean detail() {
            return false;
        }

        @Override
        protected void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                             DARPInstance.DARPSolution initSolution) {
            solver.solveAll(instance, solveStatistics);
        }
    }

    protected class RunSolveLNS extends Run {

        @Override
        protected String additionalInfo() {
            return " lns without initial solution ";
        }

        @Override
        protected void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                             DARPInstance.DARPSolution initSolution) {
            solver.solveLns(instance, solveStatistics);
        }
    }

    protected class RunSolveLNSInitSol extends Run {

        @Override
        protected String additionalInfo() {
            return " lns with initial solution ";
        }

        @Override
        protected boolean useInitSolution() {
            return true;
        }

        @Override
        protected void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                             DARPInstance.DARPSolution initSolution) {
            solver.solveLns(instance, initSolution, solveStatistics);
        }
    }

    protected class FindInitSolutions extends Run {

        @Override
        protected String additionalInfo() {
            return " used for first solution only ";
        }

        @Override
        protected void concurrentExperiment() {
            String experimentPattern = initSolFolder + '/' + "%s.txt";
            // prepare the data
            ArrayList<FullExperiment> allExperiments = new ArrayList<>();
            for (InstanceFile instanceFile: stateOfTheArtInstanceFiles) {
                DARPInstance baseInstance = new DARPInstance(instanceFile.instancePath);
                DARPInstance instance = baseInstance.copy();
                DARPSolver solver = new DARPLnsFfpa(instance);
                String experiment = String.format(experimentPattern, instanceFile.getName());
                allExperiments.add(new FullExperiment(solver, experiment, instanceFile, instance));
            }
            // run the simulations
            allExperiments.stream().parallel().forEach(this::concurrentRun);
        }

        @Override
        protected void concurrentRun(FullExperiment experiment) {
            DARPSolveStatistics solveStatistics = new DARPSolveStatistics(experiment.file.maxTime);
            DARPSolver solver = experiment.solver;
            solve(solver, experiment.darpInstance, solveStatistics, null);
            DARPInstance.DARPSolution initSol = solveStatistics.firstSolution();
            String info = initSol.detailedRoute();
            System.out.println("found solution for " + experiment.file.getName());
            writeToFileConcurrent(experiment.experiment_name , info, false);
        }

        @Override
        protected void solve(DARPSolver solver, DARPInstance instance, DARPSolveStatistics solveStatistics,
                             DARPInstance.DARPSolution initSolution) {
            solver.solve(instance, solveStatistics);
        }
    }

    public static void main(String[] args) {
        DARPBenchmark benchmark = new DARPBenchmark();
        benchmark.setInstanceFiles(benchmark.stateOfTheArtInstanceFiles);
        benchmark.setAllVersion(true); // use all versions of all solvers
        benchmark.setParallelRun(true); // 1 experiment per thread
        benchmark.setnRun(10); // number of run per instance
        benchmark.setSolverToUses(new SolverProvider[] {
                //DARPLnsFfpa::new,
                DARPInsertion::new,
        });

        benchmark.benchMark_solveLNS();
        benchmark.benchMark_solveLNSInitSol();
        /*
        benchmark.setInstanceFiles(new InstanceFile[] {
                new InstanceFile("data/darp/simpleInstance2.txt", 60.0),
                new InstanceFile("data/darp/Cordeau/a2-16.txt", 60.0),
                new InstanceFile("data/darp/Cordeau/a2-20.txt", 3600.0),
        });
        benchmark.benchMark_solveAll();

         */
    }

}
