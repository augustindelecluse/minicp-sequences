package minicp.engine.core;

import minicp.util.Procedure;

public interface SequenceVarNoBound {

    Solver getSolver();

    int nScheduled();
    int nPossible();
    int nRequired();
    boolean isBound();
    int nNodes();

    int nextMember(int node);
    int predMember(int node);

    void schedule(int node, int pred);
    boolean canSchedule(int node, int pred);
    void exclude(int node);
    void excludeAllPossible();

    boolean isScheduled(int node);
    boolean isPossible(int node);
    boolean isExcluded(int node);

    int fillOrder(int[] dest);
    int fillScheduled(int[] dest);
    int fillPossible(int[] dest);
    int fillExcluded(int[] dest);

    int nPossibleInsertions(int node);
    int nScheduledInsertions(int node);
    int nInsertions(int node);

    /**
     * fill operations to get predecessors can give bottom as a result
     *
     * possible ways:
     *  - ok?
     *          -> fill operations always yield bottom if it is present
     *  - not ok?
     *          -> fill operations can yield bottom or not (boolean to ask to give it or not)
     *
     * why it can break:
     *  - some constraints use array access from the predecessors (time[pred] for instance)
     *  - with bottom this would give an OutOfBoundException
     *
     * Additional question:
     *  - let a sequence be    bottom -> a -> b
     *  - should succ[b] == a or succ[b] == bottom?
     *      - same for pred[a]
     *
     */

    int fillInsertions(int node, int[] dest);
    int fillScheduledInsertions(int node, int[] dest);
    int fillPossibleInsertions(int node, int[] dest);
    boolean isInsertion(int node, int pred);
    void removeInsertion(int node, int pred);

    void whenBind(Procedure f);
    void whenInsert(Procedure f);
    void whenExclude(Procedure f);
    void propagateOnBind(Constraint c);
    void propagateOnInsert(Constraint c);
    void propagateOnExclude(Constraint c);

}
