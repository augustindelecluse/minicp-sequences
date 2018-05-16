package minicp.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.function.Consumer;

public class GraphUtil {

    public static interface Graph {
        /**
         * @return the number of nodes in this graph. They are indexed from 0 to n-1.
         */
        int n();

        /**
         * @param idx the node to consider
         * @return the nodes ids that have an edge going from then to node idx
         */
        Iterable<Integer> in(int idx);

        /**
         * @param idx the node to consider
         * @return the nodes ids that have an edge going from node idx to them.
         */
        Iterable<Integer> out(int idx);
    }

    /**
     * Transpose the graph
     *
     * @param graph
     * @return
     */
    public static Graph transpose(Graph graph) {
        return new Graph() {
            @Override
            public int n() {
                return graph.n();
            }

            @Override
            public Iterable<Integer> in(int idx) {
                return graph.out(idx);
            }

            @Override
            public Iterable<Integer> out(int idx) {
                return graph.in(idx);
            }
        };
    }

    /**
     * Returns the SCC of the graph
     * For at each index, an integer representing the scc id of the node
     */
    public static int[] stronglyConnectedComponents(Graph graph) {
        Stack<Integer> firstOrder = new Stack<>();
        dfs(graph, firstOrder::push, (x) -> { });
        int [] scc = new int[graph.n()];
        Counter cpt = new Counter();
        dfs(transpose(graph),
                x -> scc[x] = cpt.getValue(),
                x -> cpt.incr());
        return scc;
    }

    /**
     * Do a DFS
     *
     * @param graph      the graph on which the DFS is run
     * @param action     the action to be made on each node. It is a function that will be called with the id of the node.
     * @param onNewStart each time the DFS has to restart from a new node, this function is called with the id of the node.
     */
    public static void dfs(Graph graph, Consumer<Integer> action, Consumer<Integer> onNewStart) {
        boolean[] visited = new boolean[graph.n()];
        Arrays.fill(visited, false);
        for (int i = 0; i < graph.n(); i++) {
            if (!visited[i]) {
                onNewStart.accept(i);
                dfsNode(graph, action, visited, i);
            }

        }
    }

    private static void dfsNode(Graph graph, Consumer<Integer> action, boolean[] visited, int start) {
        Stack<Integer> todo = new Stack<>();
        todo.add(start);
        visited[start] = true;
        while (!todo.isEmpty()) {
            int cur = todo.pop();
            action.accept(cur);
            for (int next : graph.out(cur)) {
                if (!visited[next]) {
                    todo.add(next);
                    visited[next] = true;
                }
            }
        }
    }
}
