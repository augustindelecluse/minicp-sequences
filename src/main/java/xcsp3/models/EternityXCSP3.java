package xcsp3.models;

import minicp.engine.core.Solver;
import minicp.util.InputReader;
import org.xcsp.common.IVar.Var;
import org.xcsp.common.Types;
import org.xcsp.common.structures.TableInteger;
import org.xcsp.modeler.ProblemAPI;

import java.util.Arrays;

import static minicp.cp.Factory.makeSolver;

class EternityXCSP3 implements ProblemAPI {

    static class Instance {
        public static String instance = "data/eternity/eternity8x8.txt";
    }

    public static Var[] flatten(Var[][] x) {
        return Arrays.stream(x).flatMap(Arrays::stream).toArray(Var[]::new);
    }

    public void model() {

        InputReader reader = new InputReader(Instance.instance);

        int n = reader.getInt();
        int m = reader.getInt();

        int[][] pieces = new int[n * m][4];
        int max_ = 0;

        for (int i = 0; i < n * m; i++) {
            for (int j = 0; j < 4; j++) {
                pieces[i][j] = reader.getInt();
                if (pieces[i][j] > max_)
                    max_ = pieces[i][j];
            }
            System.out.println(Arrays.toString(pieces[i]));
        }
        final int max = max_;

        // ------------------------

        // Table with all pieces and for each their 4 possible rotations

        TableInteger table = new TableInteger(); //[][][4*n*m][5];

        for (int i = 0; i < pieces.length; i++) {
            for (int r = 0; r < 4; r++) {
                table.add(i, pieces[i][(r + 0) % 4], pieces[i][(r + 1) % 4], pieces[i][(r + 2) % 4], pieces[i][(r + 3) % 4], 0);

                table.add(i, max + 1, pieces[i][(r + 1) % 4], pieces[i][(r + 2) % 4], pieces[i][(r + 3) % 4], 1);
                table.add(i, pieces[i][(r + 0) % 4], max + 1, pieces[i][(r + 2) % 4], pieces[i][(r + 3) % 4], 1);
                table.add(i, pieces[i][(r + 0) % 4], pieces[i][(r + 1) % 4], max + 1, pieces[i][(r + 3) % 4], 1);
                table.add(i, pieces[i][(r + 0) % 4], pieces[i][(r + 1) % 4], pieces[i][(r + 2) % 4], max + 1, 1);

                table.add(i, max + 1, max + 1, pieces[i][(r + 2) % 4], pieces[i][(r + 3) % 4], 1);
                table.add(i, max + 1, pieces[i][(r + 1) % 4], max + 1, pieces[i][(r + 3) % 4], 1);
                table.add(i, max + 1, pieces[i][(r + 1) % 4], pieces[i][(r + 2) % 4], max + 1, 1);
                table.add(i, pieces[i][(r + 0) % 4], max + 1, max + 1, pieces[i][(r + 3) % 4], 1);
                table.add(i, pieces[i][(r + 0) % 4], max + 1, pieces[i][(r + 2) % 4], max + 1, 1);
                table.add(i, pieces[i][(r + 0) % 4], pieces[i][(r + 1) % 4], max + 1, max + 1, 1);

                table.add(i, pieces[i][(r + 0) % 4], max + 1, max + 1, max + 1, 1);
                table.add(i, max + 1, pieces[i][(r + 1) % 4], max + 1, max + 1, 1);
                table.add(i, max + 1, max + 1, pieces[i][(r + 2) % 4], max + 1, 1);
                table.add(i, max + 1, max + 1, max + 1, pieces[i][(r + 3) % 4], 1);

                table.add(i, max + 1, max + 1, max + 1, max + 1, 1);
            }
        }

        Solver cp = makeSolver();

        Var[][] id = new Var[n][m]; // id
        Var[][] u = new Var[n][m];  // up
        Var[][] r = new Var[n][m];  // right
        Var[][] d = new Var[n][m];  // down
        Var[][] l = new Var[n][m];  // left
        Var[][] cost = array("cost", size(n, m), dom(range(0, 1)));

        for (int i = 0; i < n; i++) {
            u[i] = array("u" + i, size(m), dom(range(0, max + 1)));
            id[i] = array("id" + i, size(m), dom(range(n * m)));
        }
        for (int k = 0; k < n; k++) {
            final int i = k;
            if (i < n - 1) d[i] = u[i + 1];
            else d[i] = array("d" + (n - 1), size(m), dom(range(0, max + 1)));
        }
        for (int j = 0; j < m; j++) {
            Var[] lj = array("l" + j, size(n), dom(range(0, max + 1)));
            for (int i = 0; i < n; i++) {
                l[i][j] = lj[i];
            }
        }
        for (int j = 0; j < m - 1; j++) {
            for (int i = 0; i < n; i++) {
                r[i][j] = l[i][j + 1];
            }
        }
        Var[] rm = array("r" + (m - 1), size(n), dom(range(0, max + 1)));
        for (int i = 0; i < n; i++) {
            r[i][m - 1] = rm[i];
        }

        allDifferent(flatten(id));

        forall(range(n).range(m), (i, j) -> extension(new Var[]{id[i][j], u[i][j], r[i][j], d[i][j], l[i][j], cost[i][j]}, table));

        forall(range(n), (i) -> equal(l[i][0], 0));
        forall(range(n), (i) -> equal(r[i][m - 1], 0));
        forall(range(m), (j) -> equal(u[0][j], 0));
        forall(range(m), (j) -> equal(d[n - 1][j], 0));


        minimize(Types.TypeObjective.SUM, cost);

        /*decisionVariables(
                Stream.concat(
                    Stream.concat(
                        Stream.concat(Arrays.stream(id).flatMap(Arrays::stream), Arrays.stream(u).flatMap(Arrays::stream)),
                        Stream.concat(Arrays.stream(r).flatMap(Arrays::stream), Arrays.stream(d).flatMap(Arrays::stream))),
                    Arrays.stream(l).flatMap(Arrays::stream)).toArray(Var[]::new));*/
    }

    public static void main(String[] args) {
        Instance.instance = args[0];
        org.xcsp.modeler.Compiler.main(new String[]{"xcsp3.models.EternityXCSP3"});
    }
}
