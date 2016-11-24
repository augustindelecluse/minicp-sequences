package minicp.examples.tsptw;

import minicp.util.Procedure;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * run a bunch of experiments for TSPTW
 */
public class TSPBenchmark {

    private static String instancePath = "data/TSPTW/instances"; // path to the instances folder
    private static String bestSolPath = "data/TSPTW/results/best_found"; // where to write the solution found

    private int maxParallel;
    private int nRun = 10; // number of run to perform
    private static int timeout = 300; // timeout in seconds

    // files where the initial solutions to start from are written
    private String[] setToSolve = new String[] {
            "data/TSPTW/best_known_sol/AFG.txt",
            "data/TSPTW/best_known_sol/OhlmannThomas.txt",
            "data/TSPTW/best_known_sol/GendreauDumasExtended.txt",
            "data/TSPTW/best_known_sol/SolomonPesant.txt",
            "data/TSPTW/best_known_sol/SolomonPotvinBengio.txt"
    };

    private class InstanceRun {

        private int[] bestPermutation;
        private double bestObjective = Double.MAX_VALUE;
        private int bestSeed = 0;

        private String instanceSet;
        private String instance;
        private int[] initSol;
        private String initSolString;

        private String details;

        public InstanceRun(String instanceSet, String instance, int[] initSol) {
            this.instanceSet = instanceSet;
            this.instance = instance;
            this.initSol = initSol;
            bestPermutation = initSol;
            initSolString = initSolString();
            details = "";
        }

        public String instancePath() {
            return TSPBenchmark.instancePath + '/' + instanceSet + '/' + instance;
        }

        private String initSolString() {
            return Arrays.stream(initSol)
                    .mapToObj(String::valueOf)
                    .collect(Collectors.joining(" "));
        }

        public void notifySolution(int [] solution, double objective, String details, int seed) {
            if (objective < bestObjective) {
                bestPermutation = solution;
                bestObjective = objective;
                this.details = details;
                bestSeed = seed;
            }
        }

    }

    private record Experiment(InstanceRun instanceParam, int run, int seed) {
        public String[] getArgs() {
            return new String[] {
                    "-f", instanceParam.instancePath(),
                    "-s", instanceParam.initSolString,
                    "-t", String.valueOf(timeout),
                    "-r", String.valueOf(seed)
            };
        }

        public String detail() {
            return instanceParam.instancePath();
        }
    }

    private static boolean isInt(String s) {
        if (s.isEmpty()) return false;
        for(int i = 0; i < s.length(); i++) {
            if(i == 0 && s.charAt(i) == '-') {
                if(s.length() == 1) return false;
                else continue;
            }
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    /**
     * prepare a bunch of instances run from a solPath
     * @param solutionFile path to the file where initial solutions are written
     * @return list of instances to run
     */
    private List<InstanceRun> prepareInstanceRun(String solutionFile) throws IOException {
        ArrayList<InstanceRun> instanceRuns = new ArrayList<>();
        String instanceSet = Paths.get(solutionFile).getFileName().toString().replace(".txt","");
        try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.strip().startsWith("#")) // comment line
                    continue;
                boolean ignoreInt = true; // the first int value is ignored as it corresponds to CV value
                ArrayList<Integer> initalList = new ArrayList<>();
                String instance = null;
                for (String val: line.split("\\s+")) {
                    if (isInt(val)) {
                        if (ignoreInt) { // ignore the first digit
                            ignoreInt = false;
                        } else {
                            int intVal = Integer.parseInt(val);
                            if (intVal != 0) // ignore begin depot
                                initalList.add(intVal);
                        }
                    } else {
                        if (instance == null)
                            instance = val;
                    }
                }
                if (instance != null && !initalList.isEmpty())
                    instanceRuns.add(new InstanceRun(instanceSet, instance, initalList.stream().mapToInt(i -> i).toArray()));
            }
        }
        return instanceRuns;
    }

    public TSPBenchmark() {
        maxParallel = Runtime.getRuntime().availableProcessors() - 1;
    }

    private Semaphore writerSemaphore = new Semaphore(1); //used to write results to file and notify solutions

    public String getCurrentLocalDateTimeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm"));
    }

    /**
     * write the results to file
     * @param instanceRunList
     */
    private void writeBestResults(List<InstanceRun> instanceRunList) {
        StringBuilder results = new StringBuilder();
        String set = null;
        for (InstanceRun instanceRun : instanceRunList) {
            results.append(instanceRun.details + " | seed: " + instanceRun.bestSeed).append("\n");
            if (set == null) {
                set = instanceRun.instanceSet;
            }
        }
        String date = getCurrentLocalDateTimeStamp();
        String filePath = bestSolPath + "/" + set + "_" + date + ".txt";
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(results.toString());
            writer.close();
        } catch (IOException exception) {
            System.err.println("failed to write results to " + filePath);
            System.err.println("results = " + results);
        }
    }

    private void withSemaphore(Procedure procedure) {
        try {
            writerSemaphore.acquire();
            procedure.call();
            writerSemaphore.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void runExperiment(Experiment experiment) {
        String[] args = experiment.getArgs();
        try {
            System.out.println("solving " + experiment.detail());
            Main main = Main.instanciate(args);
            main.solve();
            if (!main.isCrashed()) {
                withSemaphore(() -> {
                    experiment.instanceParam.notifySolution(main.getSolution(), main.getObjective(),
                            main.toString(), experiment.seed);
                });
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * solve instances
     */
    public void solve_lns() {
        int maxListSize = 2 * maxParallel;
        for (String instanceSet: setToSolve) {
            List<InstanceRun> instanceRunsList;
            try {
                instanceRunsList = prepareInstanceRun(instanceSet);
            } catch (IOException e) {
                System.out.println("failed to read " + instanceSet);
                continue;
            }
            // prepare a bunch of experiments
            ArrayList<Experiment> experimentsList = new ArrayList<>();
            for (InstanceRun instanceRun: instanceRunsList) {
                for (int i = 0; i < nRun; ++i) {
                    int seed = new Random().nextInt();
                    experimentsList.add(new Experiment(instanceRun, i, seed));
                    if (experimentsList.size() == maxListSize) { // run experiments
                        experimentsList.stream().parallel().forEach(this::runExperiment);
                        experimentsList.clear();
                    }
                }
            }
            experimentsList.stream().parallel().forEach(this::runExperiment);
            writeBestResults(instanceRunsList);
        }
    }

    public static void main(String[] args) {
        TSPBenchmark benchmark = new TSPBenchmark();
        benchmark.solve_lns();
    }

}
