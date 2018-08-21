package xcsp3.models;

import org.xcsp.common.IVar.Var;
import org.xcsp.modeler.ProblemAPI;

class GolombRuler implements ProblemAPI {
    public void model() {
        int rulerSize = 14;

        Var[] x = array("x", size(rulerSize), dom(range(0, 2 * rulerSize * rulerSize)));
        Var[] distsO = array("dists", size(rulerSize * (rulerSize + 1) / 2 - rulerSize), dom(range(0, 2 * rulerSize * rulerSize)));

        imp().manageLoop(() -> {
            int idx = 0;
            for (int i = 0; i < rulerSize; i++) {
                for (int j = 0; j < rulerSize; j++) {
                    if (j > i) {
                        intension(eq(distsO[idx], dist(x[i], x[j])));
                        idx += 1;
                    }
                }
            }
        });
        allDifferent(distsO);
        intension(eq(x[0], 0));

        forall(range(rulerSize), (i) -> {
            if (i != rulerSize - 1)
                intension(lt(x[i], x[i + 1]));

        });
        minimize(x[rulerSize - 1]);
        decisionVariables(x);
    }

    public static void main(String[] args) {
        org.xcsp.modeler.Compiler.main(new String[]{"xcsp3.models.GolombRuler"});
    }
}
