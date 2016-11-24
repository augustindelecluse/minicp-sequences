package minicp.examples.tsptw;

import java.util.Arrays;
import java.util.Comparator;

// The formulation is based on this example model from the choco documentation
// https://github.com/chocoteam/choco-solver/blob/master/examples/src/main/java/org/chocosolver/examples/integer/TSP.java

public class TsptwInstance {
    int          nbNodes;
    int[][]      distances;
    TimeWindow[] timeWindows;

    public static final double PRECISION = 1000.0;

    public TsptwInstance(final int nbNodes, final int[][] distances, final TimeWindow[] tw) {
        this.nbNodes    = nbNodes;
        this.distances  = distances;
        this.timeWindows= tw;
    }

    public TsptwInstance sort() {
        Integer [] perm = new Integer[nbNodes];
        for (int i = 0; i < nbNodes; i++) {
            perm[i] = i;
        }
        Arrays.sort(perm, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return timeWindows[o1].latest-timeWindows[o2].latest;
            }
        });

        int [][] distMatrix_ = new int[nbNodes][nbNodes];
        TimeWindow [] tw_ = new TimeWindow[nbNodes];

        for (int i = 0; i < nbNodes; i++) {
            tw_[i] = new TimeWindow(timeWindows[perm[i]].earliest,timeWindows[perm[i]].latest);
        }
        for (int i = 0; i < nbNodes; i++) {
            for (int j = 0; j < nbNodes; j++) {
                distMatrix_[i][j] = distances[perm[i]][perm[j]];
            }
        }
        return new TsptwInstance(nbNodes,distMatrix_,tw_);
    }

    /**
     * gives the cost associated to a visit ordering
     * @param ordering order of visit for the nodes. First node == 0 == begin depot
     * @return routing cost associated with the visit of the nodes
     */
    public int cost(int[] ordering) {
        int cost = 0;
        int pred = 0;
        int current = -1;
        for (int i = 1; i < nbNodes ; ++i) {
            current = ordering[i];
            cost += distances[pred][current];
            pred = current;
        }
        cost += distances[current][0]; // closes the route
        return cost;
    }

    /**
     * tell if the matrix respect the triangular inequality
     * @return true if the triangular inequality is respected
     */
    public boolean respectTriangularInequality() {
        for (int i = 0; i < distances.length ; ++i) {
            for (int j = 0; j < distances.length ; ++j) { //transition[i,j] <= transition[i,k] + transition[k,j]
                for (int k = 0 ; k < distances.length; ++k) {
                    if (distances[i][j] > distances[i][k] + distances[k][j]) {
                        //System.err.println(String.format("transition[%d][%d] > transition[%d][%d] + transition[%d][%d]" +
                        //                "(%d > %d + %d)", i, j, i, k, k, j, distances[i][j], distances[i][k], distances[k][j]));
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * give a matrix respecting the triangular inequality using the current distance matrix
     * @return matrix respecting the triangular inequality
     */
    public int[][] allPairsShortestPath() {
        int[][] new_dist = new int[nbNodes][nbNodes];
        for (int i = 0 ; i < nbNodes ; ++i) {
            System.arraycopy(distances[i], 0, new_dist[i], 0, nbNodes);
        }
        for (int i = 0; i < nbNodes ; ++i) {
            for (int j = 0; j < nbNodes ; ++j) {
                for (int k = 0 ; k < nbNodes; ++k) {
                    //transition[i,j] <= transition[i,k] + transition[k,j]
                    if (new_dist[i][j] > new_dist[i][k] + new_dist[k][j]) {
                        new_dist[i][j] = new_dist[i][k] + new_dist[k][j];
                    }
                }
            }
        }
        return  new_dist;
    }

    /**
     * tell if a path is correct or not
     * @param ordering ordering for the nodes
     * @return true if the ordering is suitable for this instance
     */
    public boolean isPathCorrect(int[] ordering) {
        int initTime = 0;
        for (int current: ordering) {
            int time = timeWindows[current].getEarliest(); // arrival time at the node
            //if
        }
        return true;
    }

}
