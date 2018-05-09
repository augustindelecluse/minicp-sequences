package xcsp3.models;

import org.xcsp.common.IVar.Var;
import org.xcsp.common.Range;
import org.xcsp.common.Types;
import org.xcsp.modeler.ProblemAPI;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MaxRetentionMagicSquare implements ProblemAPI {
    int N;

    @Override
    public void model() {
        Var[][] square = array("square", size(N,N), dom(new Range(1, N*N)), "value of the square");
        Var[][] waterLevel = array("waterLevel", size(N, N), dom(new Range(1, N*N)), "Water Level in the square. Amount of water is waterLevel[i][j] - square[i][j]");
        Var[][] localMinimum = array("localMinimum", size(N, N), dom(new Range(1, N*N)), "Minimum water level in the four squares around i, j");

        int magic = N*(N*N+1)/2;

        allDifferent(Arrays.stream(square).flatMap(Arrays::stream).toArray(Var[]::new));
        forall(range(N), i -> {
            sum(square[i], EQ, magic);
            sum(getColumn(square, i), EQ, magic);
        });

        forall(range(N), i -> {
            equal(waterLevel[0][i], square[0][i]);
            equal(waterLevel[N-1][i], square[N-1][i]);
            if (i != 0 && i != N-1) {
                equal(waterLevel[i][0], square[i][0]);
                equal(waterLevel[i][N-1], square[i][N-1]);
            }
        });

        forall(range(1, N-2).range(1, N-2), (i,j) -> {
            minimum(new Var[]{waterLevel[i][j-1], waterLevel[i][j+1], waterLevel[i-1][j], waterLevel[i+1][j]}, localMinimum[i][j]);
        });

        forall(range(1, N-2).range(1, N-2), (i,j) -> {
            maximum(new Var[]{square[i][j], localMinimum[i][j]}, waterLevel[i][j]);
        });

        decisionVariables(concatenate(
                Arrays.stream(square).flatMap(Arrays::stream).toArray(Var[]::new),
                Arrays.stream(waterLevel).flatMap(Arrays::stream).toArray(Var[]::new)));
        maximize(Types.TypeObjective.SUM, waterLevel);
    }

    public Var[] getColumn(Var[][] array, int column) {
        Var[] out = new Var[array.length];
        for(int i = 0; i < array.length; i++)
            out[i] = array[i][column];
        return out;
    }

    public static void main(String[] args) {
        org.xcsp.modeler.Compiler.main(new String[]{"org.xcsp.modeler.models.MaxRetentionMagicSquare", "-data=6", "-ev"});
    }

    public <T> T[] concatenate(T[] a, T[] b) {
        int aLen = a.length;
        int bLen = b.length;

        @SuppressWarnings("unchecked")
        T[] c = (T[]) Array.newInstance(a.getClass().getComponentType(), aLen + bLen);
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
