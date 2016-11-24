package minicp.engine.core;

import minicp.util.Procedure;

/**
 * decision variable used to represent a sequence of nodes
 * the nodes are split into 3 categories:
 *  - scheduled nodes, which are part of the sequence
 *  - possible nodes, which could be part of the sequence
 *  - excluded nodes, which cannot be part of the sequence
 */
public interface SequenceVar {

    /**
     * Returns the solver in which this variable was created.
     *
     * @return the solver in which this variable was created
     */
    Solver getSolver();

    /**
     * return the first node of the sequence
     * @return first node of the sequence
     */
    int begin();

    /**
     * return the last node of the sequence
     * @return last node of the sequence
     */
    int end();

    /**
     * @return number of scheduled nodes in the sequence
     */
    int nScheduledNode();

    /**
     * @param includeBounds whether to count the beginning and ending node or not
     * @return number of schduled nodes
     */
    int nScheduledNode(boolean includeBounds);

    /**
     * @return number of possible nodes in the sequence
     */
    int nPossibleNode();

    /**
     * @return number of excluded nodes in the sequence
     */
    int nExcludedNode();

    /**
     * @return number of nodes in the sequence, omitting the beginning and ending node
     */
    int nNodes();

    /**
     * @param includeBounds whether to count the beginning and ending node or not
     * @return number of nodes in the sequence
     */
    int nNodes(boolean includeBounds);

    /**
     * set the node into the sequence, right after a predecessor. Fails if the node is in the excluded set, no effect
     * if it is already in the scheduled set
     * @param node node to schedule
     * @param pred predecessor for the node
     */
    void schedule(int node, int pred);

    /**
     * tell if a node can be scheduled with a given predecessor
     * @param node node trying to be scheduled
     * @param pred predecessor for the node
     * @return true if the node can be scheduled
     */
    boolean canSchedule(int node, int pred);

    /**
     * exclude the node from the set of possible nodes. Fails if the node is in the scheduled set, no effect if it is
     * already in the excluded set
     * @param node node to exclude
     */
    void exclude(int node);

    /**
     * exclude all possible nodes from the sequence, binding the variable
     */
    void excludeAllPossible();

    boolean isScheduled(int node);

    boolean isPossible(int node);

    boolean isExcluded(int node);

    int fillScheduled(int[] dest);

    int fillPossible(int[] dest);

    int fillExcluded(int[] dest);

    int fillScheduledInsertions(int node, int[] dest);

    int fillPossibleInsertions(int node, int[] dest);

    int nPossibleInsertions(int node);

    int nScheduledInsertions(int node);

    int nInsertions(int node);

    int fillInsertions(int node, int[] dest);

    boolean isInsertion(int node, int predecessor);

    void removeInsertion(int node, int predecessor);

    /**
     * remove all insertion having the specified node as predecessor
     * @param node
     */
    void removeInsertionAfter(int node);

    /**
     * gives the InsertionVar related to node i in the sequence
     * @param i id of the InsertionVar
     * @return InsertionVar with id i
     */
    InsertionVar getInsertionVar(int i);

    /**
     * Asks that the closure is called whenever the domain
     * of this variable is reduced to a single setValue
     *
     * @param f the closure
     */
    void whenBind(Procedure f);

    /**
     * Asks that the closure is called whenever
     * a new node is scheduled into the sequence
     *
     * @param f the closure
     */
    void whenInsert(Procedure f);

    /**
     * Asks that the closure is called whenever
     * a new node is excluded from the sequence
     *
     * @param f the closure
     */
    void whenExclude(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable is reduced to a singleton.
     * In such a state the variable is bind and we say that a <i>bind</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bind events of this variable.
     */
    void propagateOnBind(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever
     * a new node is scheduled into the sequence
     * We say that a <i>bound change</i> event occurs in this case.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bound change events of this variable.
     */
    void propagateOnInsert(Constraint c);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever
     * a new node is excluded from the sequence
     * We say that a <i>bound change</i> event occurs in this case.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bound change events of this variable.
     */
    void propagateOnExclude(Constraint c);

    /**
     * @return true when no more node belongs to the set of possible nodes
     */
    boolean isBound();

    /**
     * @param node node in the scheduled sequence
     * @return index of the successor of the node. Irrelevant if the node is not in the sequence
     */
    int nextMember(int node);

    /**
     * @param node node in the scheduled sequence
     * @return index of the predecessor of the node. Irrelevant if the node is not in the sequence
     */
    int predMember(int node);

    /**
     * fill the current order of the sequence into an array large enough, including beginning and ending node
     * @param dest array where to store the order of the sequence
     * @return number of elements in the sequence, including beginning and ending node
     */
    int fillOrder(int[] dest);

    /**
     * fill the current order of the sequence into an array large enough
     * @param dest array where to store the order of the sequence
     * @param includeBounds if True, includes the beginning and ending node into the order array
     * @return number of elements in the sequence
     */
    int fillOrder(int[] dest, boolean includeBounds);

    /**
     * give the ordering of nodes without the beginning and end nodes
     * @return
     */
    String ordering();

    /**
     * give the ordering of nodes with the beginning and end nodes
     * @param includeBounds if the bounds must be included or not
     * @return
     */
    String ordering(boolean includeBounds);

    /**
     * give the ordering of nodes with the beginning and end nodes
     * @param includeBounds if the bounds must be included or not
     * @param join string that must be used to join two consecutive nodes
     * @return
     */
    String ordering(boolean includeBounds, String join);

}
