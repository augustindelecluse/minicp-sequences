package minicp.engine.core;

import minicp.util.Procedure;

public interface SequenceEdgeVar {

    Solver getSolver();

    int nScheduled();
    int nPossible();
    int nRequired();
    boolean isBound();
    int nNodes();

    int nextMember(int node);
    int predMember(int node);

    void insert(int pred, int node);
    boolean canInsert(int pred, int node);
    void exclude(int node);
    void excludeAllPossible();

    int fillOrder(int[] dest);      // O(nScheduled)
    int fillPossible(int[] dest);   // O(nPossible)
    int fillExcluded(int[] dest);   // O(nExcluded)

    void removeEdge(int from, int to);  // 2 operations: removePred and removeSucc, O(1)
    void removeAllEdgesFrom(int from);  // O(nPossible)
    void removeAllEdgesTo(int to);      // O(nPossible)
    void hasEdge(int from, int to);     // O(1)

    int nPredecessors(int node);            // O(1)
    int nScheduledPredecessors(int node);   // O(1)
    int nPossiblePredecessors(int node);    // O(1)
    void fillPredecessors(int node, int[] dest);            // O(nPred)
    void fillScheduledPredecessors(int node, int[] dest);   // O(min(nScheduled, nPred))
    void fillPossiblePredecessors(int node, int[] dest);    // O(min(nPossible,  nPred))

    int nSuccessors(int node);              // O(1)
    int nScheduledSuccessors(int node);     // O(1)
    int nPossibleSuccessors(int node);      // O(1)
    void fillSuccessors(int node, int[] dest);              // O(nSucc)
    void fillScheduledSuccessors(int node, int[] dest);     // O(min(nScheduled, nPred))
    void fillPossibleSuccessors(int node, int[] dest);      // O(min(nPossible,  nPred))

    void whenFixed(Procedure f);
    void whenInsert(Procedure f);
    void whenExclude(Procedure f);
    void propagateOnFix(Constraint c);
    void propagateOnInsert(Constraint c);
    void propagateOnExclude(Constraint c);

    InsertionVar getInsertionVar(int node); // same methods for retrieving edges, can propagate on it

    /*
     * - a node scheduled in the sequence might still have a set of successors and predecessors
     * - canInsert(int from, int to) if hasEdge(int from, int to) && isScheduled(from) && hasEdge(int to, int fromSucc)
     *
     * - insert(int pred, int node) is more complex:
     *      predSucc = succ[pred]
     *      succ[pred] = node
     *      succ[node] = predSucc
     *      pred[node] = node
     *      pred[predSucc] = node       // change the order of the sequence
     *      removeEdge(pred, node)      // not anymore a potential edge
     *      removeEdge(node, predSucc)  // not anymore a potential edge
     *
     */

}
