package minicp.examples.tsptw;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public final class TsptwParser {
    public static TsptwInstance fromFile(final String fname) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fname))) {
            int lc                   = 0;
            int nb_nodes             = 0;
            int[][] distances        = new int[0][0];
            TimeWindow[] timewindows = new TimeWindow[0];

            int twc = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                // skip comment lines
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                // First line is the number of nodes
                if (lc == 0) {
                    nb_nodes   = Integer.parseInt(line.split("\\s+")[0]);
                    distances  = new int[nb_nodes][nb_nodes];
                    timewindows= new TimeWindow[nb_nodes];
                }
                // The next 'nb_nodes' lines represent the distances matrix
            else if (lc >= 1 && lc <=nb_nodes) {
                    int i = (lc - 1);
                    int j = 0;
                    for (String distance : line.split("\\s+")) {
                        float fdist     = Float.parseFloat(distance);
                        int   idist     = (int) Math.rint(TsptwInstance.PRECISION * fdist);
                        distances[i][j] = idist;
                        j += 1;
                    }
                }
                // Finally, the last 'nb_nodes' lines impose the time windows constraints
            else {
                    String[] tokens = line.split("\\s+");
                    double fearliest    = Double.parseDouble(tokens[0]);
                    double flatest      = Double.parseDouble(tokens[1]);

                    int iearliest      = (int) Math.rint(fearliest * TsptwInstance.PRECISION);
                    int ilatest        = (int) Math.rint(flatest   * TsptwInstance.PRECISION);

                    timewindows[twc++] = new TimeWindow(iearliest, ilatest);
                }

                lc += 1;
            }

            return new TsptwInstance(nb_nodes, distances, timewindows);
        }
    }

    private static boolean isInt(String s) {
        if(s.isEmpty()) return false;
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
     * gives the solution to an instance found in the provided file
     * @param solutionFile file where the solution is written
     * @param instance name of the instance
     * @return null if a solution existed in the file, consecutive visited nodes otherwise
     */
    public static int[] solutionFromFile(String solutionFile, String instance) throws IOException {
        int [] initial = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(solutionFile))) {
            String line;
            while ((line = reader.readLine()) != null && initial == null) {
                if (line.strip().startsWith(instance)) { // line corresponds to the instance
                    boolean ignoreInt = true; // the first int value must be ignored as it corresponds to CV value
                    ArrayList<Integer> initalList = new ArrayList<>();
                    for (String val: line.split("\\s+")) {
                        if (isInt(val)) {
                            if (ignoreInt) { // ignore the first digit
                                ignoreInt = false;
                            } else {
                                initalList.add(Integer.parseInt(val));
                            }
                        }
                    }
                    initial = initalList.stream().mapToInt(i -> i).toArray();
                }
            }
        }
        return initial;
    }
}
