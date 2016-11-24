package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.SequenceVar;

import java.util.HashMap;

/**
 * ensure that the transitions from node to node respect the cost limit
 */
public class TransitionCost extends AbstractConstraint {

    private final SequenceVar sequenceVar;
    private final IntVar costLimit;
    private int[] insertions;
    private int[] insertions2;

    private boolean useHashMap;
    private int[][] cost;
    private HashMap<Integer, HashMap<Integer, Integer>> costMap;

    /**
     * ensure that the transitions from node to node respect the cost limit
     * @param sequenceVar sequence on which the constraint is applied
     * @param cost cost change from one node to another. Can be negative or positive
     * @param costLimit limit for the cost change
     */
    public TransitionCost(SequenceVar sequenceVar, int[][] cost, IntVar costLimit) {
        super(sequenceVar.getSolver());
        this.sequenceVar = sequenceVar;
        this.cost = cost;
        this.costLimit = costLimit;
        this.insertions = new int[sequenceVar.nNodes() + 2];
        this.insertions2 = new int[sequenceVar.nNodes() + 2];
        useHashMap = false;
    }

    /**
     * ensure that the transitions from node to node respect the cost limit
     * @param sequenceVar sequence on which the constraint is applied
     * @param costMap cost change from one node to another. Can be negative or positive
     * @param costLimit limit for the cost change
     */
    public TransitionCost(SequenceVar sequenceVar, HashMap<Integer, HashMap<Integer, Integer>> costMap, IntVar costLimit) {
        super(sequenceVar.getSolver());
        this.sequenceVar = sequenceVar;
        this.costLimit = costLimit;
        this.costMap = costMap;
        this.insertions = new int[sequenceVar.nNodes() + 2];
        this.insertions2 = new int[sequenceVar.nNodes() + 2];
        useHashMap = true;
    }

    /**
     * give the cost related to a transition from one node to another. If the transition did not exist, the cost is zero
     * @param from origin of the transition
     * @param to destination of the transition
     * @return cost between 2 nodes, 0 if no edge exist between them
     */
    public int getCost(int from, int to) {
        if (useHashMap) {
            if (!hasEdge(from, to))
                return 0;
            return costMap.get(from).get(to);
        }
        else
            return cost[from][to];
    }

    /**
     * tell if an edge exist between 2 nodes
     * @param from origin node of the edge
     * @param to destination node of the edge
     * @return true if an edge exists
     */
    public boolean hasEdge(int from, int to) {
        if (useHashMap) {
            if (costMap.containsKey(from)) {
                return costMap.get(from).containsKey(to);
            }
            return false;
        }
        return true;
    }

    @Override
    public void post() {
        if (useHashMap) {
            // remove the insertions where the cost is not specified
            int[] insertions2 = new int[sequenceVar.nNodes() + 2];
            int size = sequenceVar.fillPossible(insertions);
            for (int i = 0 ; i < size; ++i) {
                int node = insertions[i];
                int nPred = sequenceVar.fillInsertions(node, insertions2);
                for (int j = 0; j < nPred; ++j) {
                    int pred = insertions2[j];
                    if (!hasEdge(pred, node)) {
                        sequenceVar.removeInsertion(node, pred);
                    }
                }
            }
        }
        propagate();
        if (isActive()) {
            sequenceVar.propagateOnInsert(this);
            sequenceVar.propagateOnExclude(this);
        }
    }

    @Override
    public void propagate() {
        int size = sequenceVar.fillOrder(insertions, true);
        // compute the current cost of the sequence
        int currentCost = 0;
        int pred = insertions[0];
        for (int i = 1; i < size; ++i) {
            int current = insertions[i];
            currentCost += getCost(pred, current);
            pred = current;
        }

        if (sequenceVar.isBound()) {
            costLimit.assign(currentCost);
            setActive(false);
        } else {
            int changeCost = computeLowerBound();
            costLimit.removeBelow(changeCost + currentCost);
        }
    }

    private int computeLowerBound() {
        int size = sequenceVar.fillPossible(insertions);
        int minCostTot = 0;
        for (int i = 0 ; i < size ; ++i) {
            int node = insertions[i];
            int nPred = sequenceVar.fillInsertions(node, insertions2);
            if (nPred > 0) {
                int minCost = Integer.MAX_VALUE;
                for (int j = 0 ; j < nPred;++j) {
                    int pred = insertions2[j];
                    minCost = Math.min(minCost, getCost(pred, node));
                }
                minCostTot += minCost;
            }
        }
        return minCostTot;
    }
}
