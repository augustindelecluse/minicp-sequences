package xcsp3.models;

import minicp.engine.core.Solver;
import minicp.util.InputReader;
import org.xcsp.common.IVar.Var;
import org.xcsp.common.Size;
import org.xcsp.modeler.ProblemAPI;

import java.util.stream.IntStream;

import static minicp.cp.Factory.makeSolver;

class SteelXCSP3 implements ProblemAPI {

    static class Instance {
        public static String instance = "data/steel/bench_19_10";
    }


    public void model() {

        // Reading the data

        InputReader reader = new InputReader(Instance.instance);
        int nCapa = reader.getInt();
        int[] capa = new int[nCapa];
        for (int i = 0; i < nCapa; i++) {
            capa[i] = reader.getInt();
        }
        int maxCapa = capa[capa.length - 1];
        int[] lossTab = new int[maxCapa + 1];
        int capaIdx = 0;
        for (int i = 0; i < maxCapa; i++) {
            lossTab[i] = capa[capaIdx] - i;
            if (lossTab[i] == 0) capaIdx++;
        }


        int nCol = reader.getInt();
        int nSlab = reader.getInt();
        int nOrder = nSlab;
        int[] w = new int[nOrder];
        int[] c = new int[nOrder];
        for (int o = 0; o < nOrder; o++) {
            w[o] = reader.getInt();
            c[o] = reader.getInt() - 1;
        }


        // ---------------------------

        Solver cp = makeSolver();


        Var[] x = array("x", size(nOrder), dom(range(0, nSlab - 1)));
        Var[] load = array("load", size(nSlab), dom(range(0, maxCapa + 1)));
        Var[] loss = array("loss", size(nSlab), dom(range(0, maxCapa + 1)));
        Var[][] y = array("y", Size.Size2D.build(nSlab, nOrder), dom(0, 1));


        // max two color in each slab
/*
        Var[][] nOrdersOfColor = array("nOrders",Size.Size2D.build(nSlab,nCol), dom(0,nOrder+1));


        forall(range(nSlab).range(nCol), (s,col) -> {
            ArrayList<Var> scope = new ArrayList<>();
            for (int o = 0; o < nOrder; o++) {
                if (c[o] == col) {
                    scope.add(y[s][o]);
                }
            }
            sum(scope.toArray(new Var[0]),EQ,nOrdersOfColor[s][col]);
        });

        Var[][] colPresent = array("colPresent",Size.Size2D.build(nSlab,nCol), dom(0,1));
        forall(range(nSlab).range(nCol), (s,col) -> intension(iff(colPresent[s][col],ge(nOrdersOfColor[s][col],1))));
        forall(range(nSlab), s -> sum(colPresent[s],LE,2));
*/


        forall(range(nSlab).range(nOrder), (s, o) -> intension(iff(y[s][o], eq(x[o], s))));
        forall(range(nSlab), s -> sum(y[s], w, EQ, load[s]));
        sum(load, EQ, IntStream.of(w).sum());
        forall(range(nSlab), s -> element(lossTab, load[s], loss[s]));

        minimize(SUM, loss);


    }

    public static void main(String[] args) {
        //if (args.length > 0) Instance.instance = args[0];
        org.xcsp.modeler.Compiler.main(new String[]{"xcsp3.models.SteelXCSP3"});
    }
}
