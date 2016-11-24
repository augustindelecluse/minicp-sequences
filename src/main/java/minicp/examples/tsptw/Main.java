package minicp.examples.tsptw;

import org.apache.commons.cli.*;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.stream.IntStream;

public class Main {

    private final String fname;
    private final int[]  initial;
    private final int    timeout;

    private final long   start;
    private long         time;
    private double       objective;
    private int[]        solution;
    private boolean      improved;
    private boolean      crashed;
    private boolean      closed;
    private String       error;
    private int          seed;

    public boolean isCrashed() {
        return crashed;
    }

    public double getObjective() {
        return objective;
    }

    public int[] getSolution() {
        return solution;
    }

    public Main(final String fname, final int timeout, final int[] initial, final double icost, final int seed) {
        this.fname     = fname;
        this.timeout   = timeout;
        this.initial   = initial;

        this.start     = System.currentTimeMillis();
        this.time      = this.start;
        this.objective = icost;
        this.solution  = initial == null ? null : initial.clone();
        this.improved  = false;
        this.crashed   = false;
        this.error     = null;
        this.closed    = false;
        this.seed      = seed;
    }

    public static Main instanciate(String[] args) throws Exception {
        CommandLine cli = cli(args);
        String fname   = cli.getOptionValue("f");
        int    timeout = Integer.parseInt(cli.getOptionValue("t", "600"));
        int[]  initial = initial(cli.getOptionValue("s"));
        String doubleMaxVal = String.valueOf(Double.MAX_VALUE);
        double icost   = Double.parseDouble(cli.getOptionValue("c", doubleMaxVal));
        int seed = Integer.parseInt(cli.getOptionValue("r", String.valueOf(42)));

        return new Main(fname, timeout, initial, icost, seed);
    }

    /**
     * example of usage:
     * minicp.examples.tsptw.Main -f data/TSPTW/instances/AFG/rbg132.tw -s "2 1 4 6 5 3 13 12 7 9 8 11 10 17 15 14 16 19 18 21 20 22 24 23 28 25 29 27 26 30 33 34 32 31 39 38 36 41 35 37 43 42 45 44 40 46 47 48 49 50 51 52 54 53 61 57 56 55 62 60 58 59 65 63 64 75 66 74 72 70 71 68 69 67 78 73 79 77 81 83 76 80 85 82 84 92 88 86 87 93 91 89 90 94 95 96 98 97 102 99 104 101 100 103 106 105 109 107 108 112 110 113 111 118 115 116 114 121 124 117 120 123 122 119 129 128 125 130 127 126"
     * @param args -f PATH_TO_INSTANCE -s INITAL_SOLUTION -t TIMEOUT
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        CommandLine cli = cli(args);

        String fname   = cli.getOptionValue("f");
        int    timeout = Integer.parseInt(cli.getOptionValue("t", "600"));
        int[]  initial = initial(cli.getOptionValue("s"));
        String doubleMaxVal = String.valueOf(Double.MAX_VALUE);
        double icost   = Double.parseDouble(cli.getOptionValue("c", doubleMaxVal));
        int seed = Integer.parseInt(cli.getOptionValue("r", String.valueOf(42)));

        // get the solution from the provided file
        //initial = TsptwParser.solutionFromFile("data/TSPTWSolution/Langevin.txt", "N20ft309.dat");

        Main main = new Main(fname, timeout, initial, icost, seed);
        main.solve();
        
        System.out.println(main.toString());
    }

    private String status() {
        if (crashed) {
            return "crashed";
        }

        if (closed && improved) {
            return "closed (improved)";
        }
        if (closed && !improved) {
            return "closed (initial)";
        }
        if (improved) {
            return "open (improved)";
        } else {
            return "open (initial)";
        }
    }

    public void solve() {
        try {
            TsptwInstance instance = TsptwParser.fromFile(fname);
            TsptwSolver solver     = new TsptwSolver(instance, timeout, initial);
            solver.setVerbosity(1);
            solver.setSeed(seed);
            if (initial != null && objective == Double.MAX_VALUE) { // solution provided but cost omitted
                this.objective = ((double)instance.cost(initial)) / TsptwInstance.PRECISION; // compute the cost associated to the solution
            }

            solver.addObserver((solution,objective) -> {
                // System.out.println(objective);
                this.time       = System.currentTimeMillis();
                this.objective  = (((double)objective) / TsptwInstance.PRECISION);
                this.improved   = true;
                if (this.solution == null) {
                    this.solution = new int[instance.nbNodes];
                }
                System.arraycopy(solution, 0, this.solution, 0, this.solution.length);
            });

            TsptwResult result = solver.optimize();
            this.closed  = result.isOptimum;
        } catch (Throwable e) {
            this.crashed = true;
            this.error   = e.getMessage();
        }
    }

    private static final String instanceName(final String fname) {
        String[] chunks = fname.split("/");
        if (chunks.length < 2) {
            return chunks[0];
        } else {
            return String.format("%s/%s", chunks[chunks.length-2], chunks[chunks.length-1]);
        }
    }

    private static Options options() {
        Options options = new Options();
        options.addOption("f", true, "instance file");
        options.addOption("s", true, "initial solution");
        options.addOption("c", true, "initial solution cost");
        options.addOption("t", true, "timeout");
        options.addOption("r", true, "seed");
        return options;
    }

    private static CommandLine cli(final String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options(), args);

        return cmd;
    }

    private static int[] initial(final String solution) {
        if (solution == null)
            return null;
        return IntStream.concat(
                IntStream.of(0),
                Arrays.stream(solution.split("\s+"))
                    .mapToInt(x -> Integer.parseInt(x))
            ).toArray();
    }

    @Override
    public String toString() {
        StringJoiner join = new StringJoiner(" ");
        Arrays.stream(solution)
                .skip(1)
                .forEach(x -> join.add(""+x));

        String solution = join.toString();

        return String.format("%10s | %10s | %10s | %10.2f | %10.2f | %s",
                instanceName(fname),
                "sequence",
                status(),
                objective,
                (time - start) / 1000.0,
                crashed ? error : solution);
    }
}
