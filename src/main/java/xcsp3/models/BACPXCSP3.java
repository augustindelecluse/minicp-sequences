package xcsp3.models;

import minicp.engine.constraints.TableDecomp;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.SearchStatistics;
import minicp.util.InputReader;
import org.xcsp.common.IVar;
import org.xcsp.common.IVar.Var;
import org.xcsp.common.Size;
import org.xcsp.common.predicates.XNodeParent;
import org.xcsp.common.structures.TableInteger;
import org.xcsp.modeler.ProblemAPI;

import java.util.Arrays;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.cp.Factory.equal;
import static minicp.cp.Factory.makeDfs;
import static minicp.cp.Heuristics.and;
import static minicp.cp.Heuristics.firstFail;

class BACPXCSP3 implements ProblemAPI {

    static class Instance {
        public static String instance = "data/bacp/instances12/inst12.txt";
    }



    public static Var[] flatten(Var [][] x) {
        return Arrays.stream(x).flatMap(Arrays::stream).toArray(Var[]::new);
    }

    public void model() {

        InputReader reader = new InputReader(Instance.instance);

        int nbCourses = reader.getInt();
        int nbPeriods = reader.getInt();
        int minCredits = reader.getInt();
        int maxCredits = reader.getInt();
        int nbPre = reader.getInt();

        int [] credits = new int[nbCourses];
        for (int i = 0; i < credits.length; i++) {
            credits[i] = reader.getInt();
        }
        int [][] prerequisites = new int[nbPre][2];
        for (int i = 0; i < nbPre; i++) {
            prerequisites[i][0] = reader.getInt();
            prerequisites[i][1] = reader.getInt();
        }

        Var[] x = array("x",size(nbCourses),dom(range(0,nbPeriods-1)));
        Var[] l = array("l",size(nbCourses),dom(range(0,IntStream.of(credits).sum())));
        Var[][] y = array("y",Size.Size2D.build(nbPeriods,nbCourses), dom(0,1));
        forall(range(nbPeriods).range(nbCourses), (b,i) -> intension(iff(y[b][i],eq(x[i],b))));
        forall(range(nbPeriods), j -> sum(y[j],credits,EQ,l[j]));
        sum(l,EQ, IntStream.of(credits).sum());

        forall(range(nbPre), j -> lessThan(x[prerequisites[j][0]],x[prerequisites[j][1]]));

        minimize(MAXIMUM,l);




    }

    public static void main(String[] args) {
        if (args.length > 0) Instance.instance = args[0];
        org.xcsp.modeler.Compiler.main(new String[]{"xcsp3.models.BACPXCSP3"});
    }
}
