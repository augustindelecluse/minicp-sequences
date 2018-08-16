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
            q[i] = model.intVar("Q_"+i, 1, n,false);
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
}
