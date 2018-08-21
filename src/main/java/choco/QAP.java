package choco;

import minicp.util.InputReader;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.Smallest;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelectorWithTies;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.Arrays;

public class QAP {
    public static void main(String[] args) {

/*
12

    0    90    10    23    43     0     0     0     0     0    0     0
   90     0     0     0     0    88     0     0     0     0    0     0
   10     0     0     0     0     0    26    16     0     0    0     0
   23     0     0     0     0     0     0     0     0     0    0     0
   43     0     0     0     0     0     0     0     0     0    0     0
    0    88     0     0     0     0     0     0     1     0    0     0
    0     0    26     0     0     0     0     0     0     0    0     0
    0     0    16     0     0     0     0     0     0    96    0     0
    0     0     0     0     0     1     0     0     0     0   29     0
    0     0     0     0     0     0     0    96     0     0    0    37
    0     0     0     0     0     0     0     0    29     0    0     0
    0     0     0     0     0     0     0     0     0    37    0     0

    0    36    54    26    59    72     9    34    79    17   46    95
   36     0    73    35    90    58    30    78    35    44   79    36
   54    73     0    21    10    97    58    66    69    61   54    63
   26    35    21     0    93    12    46    40    37    48   68    85
   59    90    10    93     0    64     5    29    76    16    5    76
   72    58    97    12    64     0    96    55    38    54    0    34
    9    30    58    46     5    96     0    83    35    11   56    37
   34    78    66    40    29    55    83     0    44    12   15    80
   79    35    69    37    76    38    35    44     0    64   39    33
   17    44    61    48    16    54    11    12    64     0   70    86
   46    79    54    68     5     0    56    15    39    70    0    18
   95    36    63    85    76    34    37    80    33    86   18     0
 */

        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int[][] w = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int maxDist = 0;
        int[][] d = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
                maxDist = Math.max(maxDist, d[i][j]);
            }
        }


        Model model = new Model(n + "QAP");
        IntVar[] x = new IntVar[n];
        for (int i = 0; i < n; i++) {
            x[i] = model.intVar("X_" + i, 0, n - 1, false);
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                model.arithm(x[i], "!=", x[j]).post();
            }
        }

        IntVar[] dist = new IntVar[n * n];
        Tuples tuples = new Tuples();

        int[] wdist = new int[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tuples.add(i, j, d[i][j]);
            }
        }
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                wdist[k] = w[i][j];
                dist[k] = model.intVar("D_" + i, 0, maxDist, false);
                model.table(new IntVar[]{x[i], x[j], dist[k]}, tuples).post();
                k++;
            }
        }
        IntVar totCost = model.intVar("obj", 0, 100000, false);
        model.scalar(dist, wdist, "=", totCost).post();


        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(
                new VariableSelectorWithTies<>(
                        new FirstFail(model),
                        new Smallest()),
                new IntDomainMin(),
                ArrayUtils.append(x))
        );


        solver.showShortStatistics();
        model.setObjective(false, totCost);
        while (solver.solve()) {
            System.out.println("solution");
            System.out.println(Arrays.toString(x));
            System.out.println(totCost);
        }


    }
}
