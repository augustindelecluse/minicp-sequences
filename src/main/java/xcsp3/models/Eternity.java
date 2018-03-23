package xcsp3.models;

import minicp.engine.constraints.TableDecomp;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.SearchStatistics;
import minicp.util.InputReader;
import org.xcsp.common.IVar.Var;
import org.xcsp.common.structures.TableInteger;
import org.xcsp.modeler.ProblemAPI;

import java.util.Arrays;

import static minicp.cp.Factory.*;
import static minicp.cp.Factory.equal;
import static minicp.cp.Factory.makeDfs;
import static minicp.cp.Heuristics.and;
import static minicp.cp.Heuristics.firstFail;

class Eternity implements ProblemAPI {

    public static Var[] flatten(Var [][] x) {
        return Arrays.stream(x).flatMap(Arrays::stream).toArray(Var[]::new);
    }

    public void model() {

        InputReader reader = new InputReader("data/eternity7x7.txt");

        int n = reader.getInt();
        int m = reader.getInt();

        int [][] pieces = new int[n*m][4];
        int max_ = 0;

        for (int i = 0; i < n*m; i++) {
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
                table.add(i,(r+0)%4,(r+1)%4,(r+2)%4,(r+3)%4);
            }
        }


        Solver cp = makeSolver();

        Var[][] id = new Var[n][m]; // id
        Var[][] u = new Var[n][m];  // up
        Var[][] r = new Var[n][m];  // right
        Var[][] d = new Var[n][m];  // down
        Var[][] l = new Var[n][m];  // left

        for (int i = 0; i < n; i++) {
             u[i] = array("u"+i,size(m),dom(range(0,max)));
             id[i] = array("id"+i,size(m),dom(range(n*m)));
        }

        for (int k = 0; k < n; k++) {
            final int i = k;
            if (i < n-1) d[i] = u[i+1];
            else d[i] = array("d",size(m),dom(range(0,max)));
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                l[i][j] = var("l"+i+","+j, dom(range(0,max)));
            }
        }
        for (int j = 0; j < m; j++) {
            for (int i = 0; i < n; i++) {
                if (j < m-1) r[i][j] = l[i][j+1];
                else r[i][j] = var("r"+i+","+j, dom(range(0,max)));
            }
        }


        // The constraints of the problem

        // all the pieces placed are different
        allDifferent(flatten(id));

        // all the pieces placed are valid one (one of the given mxn piece possibly rotated)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                extension(new Var[]{id[i][j],u[i][j],r[i][j],d[i][j],l[i][j]},table);
            }
        }

        // 0 on the border
        for (int i = 0; i < n; i++) {
            equal(l[i][0],0);
            equal(r[i][m-1],0);
        }
        for (int j = 0; j < m; j++) {
            equal(u[0][j],0);
            equal(d[n-1][j],0);
        }



    }

    public static void main(String[] args) {
        org.xcsp.modeler.Compiler.main(new String[]{"xcsp3.models.Eternity"});
        
    }
}
