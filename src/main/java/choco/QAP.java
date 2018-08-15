package choco;

import minicp.util.InputReader;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
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

        InputReader reader = new InputReader("data/qap.txt");

        int n = reader.getInt();
        // Weights
        int [][] w = new int[n][n];
        for (int i = 0; i < n ; i++) {
            for (int j = 0; j < n; j++) {
                w[i][j] = reader.getInt();
            }
        }
        // Distance
        int maxDist = 0;
        int [][] d = new int[n][n];
        for (int i = 0; i < n ; i++) {
            for (int j = 0; j < n; j++) {
                d[i][j] = reader.getInt();
                maxDist = Math.max(maxDist,d[i][j]);
            }
        }


        Model model = new Model(n + "QAP");
        IntVar[] x = new IntVar[n];
        for (int i = 0; i < n; i++) {
            x[i] = model.intVar("X_" + i, 0, n - 1);
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                model.arithm(x[i], "!=", x[j]).post();
            }
        }

        IntVar[] dist = new IntVar[n*n];
        Tuples tuples = new Tuples();

        int[] wdist = new int[n*n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tuples.add(i,j,d[i][j]);
            }
        }
        int k = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                wdist[k] = w[i][j];
                dist[k] = model.intVar("D_" + i, 0, maxDist);
                model.table(new IntVar[]{x[i],x[j],dist[k]},tuples).post();
                k++;
            }
        }
        IntVar totCost = model.intVar("obj",0,100000,true);
        model.scalar(dist,wdist,"=",totCost).post();


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
        while(solver.solve()) {
            System.out.println("solution");
            System.out.println(Arrays.toString(x));
            System.out.println(totCost);
        }


    }
}
