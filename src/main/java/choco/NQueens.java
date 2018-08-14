package choco;


import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.search.strategy.selectors.variables.FirstFail;
import org.chocosolver.solver.search.strategy.selectors.variables.Smallest;
import org.chocosolver.solver.search.strategy.selectors.variables.VariableSelectorWithTies;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

public class NQueens {

    public static void main(String[] args) {
        int n = 88;
        Model model = new Model(n + "n-queens problem");
        IntVar[] q = new IntVar[n];

        for(int i = 0; i < n; i++){
            q[i] = model.intVar("Q_"+i, 1, n);
        }

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                model.arithm(q[i],"!=",q[j]).post();
                model.arithm(q[i], "!=", q[j], "-", j - i).post();
                model.arithm(q[i], "!=", q[j], "+", j - i).post();
            }
        }

        Solver solver = model.getSolver();
        solver.setSearch(Search.intVarSearch(
                new VariableSelectorWithTies<>(
                        new FirstFail(model),
                        new Smallest()),
                new IntDomainMin(),
                ArrayUtils.append(q))
        );

        solver.showShortStatistics();

        solver.solve();


    }

    /*
    int n = 88;
    Solver cp = makeSolver();
    IntVar[] q = makeIntVarArray(cp, n, n);

        for (int i = 0; i < n; i++)
            for (int j = i + 1; j < n; j++) {
        cp.post(notEqual(q[i], q[j]));
        cp.post(notEqual(plus(q[i], j - i), q[j]));
        cp.post(notEqual(minus(q[i], j - i), q[j]));
    }
    */
}
