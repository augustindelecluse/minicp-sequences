package minicp.engine.core;

import minicp.state.StateInt;
import minicp.state.StateSequenceSet;
import minicp.state.StateStack;
import minicp.util.Procedure;

import java.util.Set;

public class SequenceVarNoBoundImpl implements SequenceVarNoBound{

    private Solver cp;
    private int nNodes;
    private InsertionVarInSequence[] insertionVars;
    private StateInt[] succ;                    // successors of the nodes
    private StateInt[] pred;                    // predecessors of the nodes
    private StateSequenceSet domain;
    private static final int bottom = -1;       // empty sequence

    // constraints registered for this sequence
    private StateStack<Constraint> onInsert;    // a node has been inserted into the sequence
    private StateStack<Constraint> onBind;      // all nodes are scheduled or excluded: no possible node remain
    private StateStack<Constraint> onExclude;   // a node has been excluded from the sequence

    private final int[] values;

    public SequenceVarNoBoundImpl(Solver cp, Set<Integer> nodes) {
        this(cp, nodes.stream().max(Integer::compareTo).get());
        for (int i = 0; i < nNodes ; ++i) {
            if (!nodes.contains(i))
                exclude(i);
        }
    }

    public SequenceVarNoBoundImpl(Solver cp, int nNodes) {
        this.cp = cp;
        this.nNodes = nNodes;
        values = new int[nNodes];
        insertionVars = new InsertionVarInSequence[nNodes];
        succ = new StateInt[nNodes];
        pred = new StateInt[nNodes];
        for (int i = 0; i < nNodes; ++i) {
            insertionVars[i] = new InsertionVarInSequence(i);
            succ[i] = cp.getStateManager().makeStateInt(i);
            pred[i] = cp.getStateManager().makeStateInt(i);
        }
        domain = new StateSequenceSet(cp.getStateManager(), nNodes);
        onInsert = new StateStack<>(cp.getStateManager());
        onBind = new StateStack<>(cp.getStateManager());
        onExclude = new StateStack<>(cp.getStateManager());
    }

    @Override
    public Solver getSolver() {
        return null;
    }

    @Override
    public int nScheduled() {
        return 0;
    }

    @Override
    public int nPossible() {
        return 0;
    }

    @Override
    public int nRequired() {
        return 0;
    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public int nextMember(int node) {
        return 0;
    }

    @Override
    public int predMember(int node) {
        return 0;
    }

    @Override
    public int nNodes() {
        return 0;
    }

    @Override
    public void schedule(int node, int pred) {

    }

    @Override
    public boolean canSchedule(int node, int pred) {
        return false;
    }

    @Override
    public void exclude(int node) {

    }

    @Override
    public void excludeAllPossible() {

    }

    @Override
    public boolean isScheduled(int node) {
        return false;
    }

    @Override
    public boolean isPossible(int node) {
        return false;
    }

    @Override
    public boolean isExcluded(int node) {
        return false;
    }

    @Override
    public int fillOrder(int[] dest) {
        return 0;
    }

    @Override
    public int fillScheduled(int[] dest) {
        return 0;
    }

    @Override
    public int fillPossible(int[] dest) {
        return 0;
    }

    @Override
    public int fillExcluded(int[] dest) {
        return 0;
    }

    @Override
    public int fillScheduledInsertions(int node, int[] dest) {
        return 0;
    }

    @Override
    public int fillPossibleInsertions(int node, int[] dest) {
        return 0;
    }

    @Override
    public int nPossibleInsertions(int node) {
        return 0;
    }

    @Override
    public int nScheduledInsertions(int node) {
        return 0;
    }

    @Override
    public int nInsertions(int node) {
        return 0;
    }

    @Override
    public int fillInsertions(int node, int[] dest) {
        return 0;
    }

    @Override
    public boolean isInsertion(int node, int predecessor) {
        return false;
    }

    @Override
    public void removeInsertion(int node, int predecessor) {

    }

    @Override
    public void whenBind(Procedure f) {

    }

    @Override
    public void whenInsert(Procedure f) {

    }

    @Override
    public void whenExclude(Procedure f) {

    }

    @Override
    public void propagateOnBind(Constraint c) {

    }

    @Override
    public void propagateOnInsert(Constraint c) {

    }

    @Override
    public void propagateOnExclude(Constraint c) {

    }

    private class InsertionVarInSequence implements InsertionVar {

        private StateInt nbPossible;  // number of possible insertions. Each value is included within the possible set of the sequence
        private StateInt nbScheduled; // number of scheduled insertions. Each value is included within the scheduled set of the sequence
        private int[] values;
        private int[] indexes;
        private int n; // max number of elements
        private int id;

        // constraints registered for this insertion
        private StateStack<Constraint> onInsert;
        private StateStack<Constraint> onDomain;
        private StateStack<Constraint> onExclude;

        private InsertionListener listener = new InsertionListener() {
            @Override
            public void insert() {
                scheduleAll(onInsert);
            }

            @Override
            public void exclude() {
                scheduleAll(onExclude);
            }

            @Override
            public void change() { scheduleAll(onDomain); }

        };

        public InsertionVarInSequence(int id) {
            n = nNodes + 1; // number of elements is nNodes + bottom
            nbPossible = cp.getStateManager().makeStateInt(n-1); // consider all nodes except itself as possible
            nbScheduled = cp.getStateManager().makeStateInt(1); // bottom is always scheduled at first
            onDomain = new StateStack<>(cp.getStateManager());
            onInsert = new StateStack<>(cp.getStateManager());
            onExclude = new StateStack<>(cp.getStateManager());

            // TODO fill correctly the values, use bottom symbol as well
            values = new int[n];
            indexes = new int[n];
            for (int i = 0; i < n; i++) {
                values[i] = i;
                indexes[i] = i;
            }

            nbScheduled.setValue(1);
        }

        @Override
        public Solver getSolver() {
            return cp;
        }

        @Override
        public boolean isBound() {
            return !SequenceVarNoBoundImpl.this.isPossible(node());
        }

        @Override
        public void removeInsert(int i) {

        }

        @Override
        public void removeAllInsert() {

        }

        @Override
        public void removeAllInsertBut(int i) {

        }

        @Override
        public boolean contains(int i) {
            return false;
        }

        @Override
        public int node() {
            return 0;
        }

        @Override
        public int fillInsertions(int[] dest) {
            return 0;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void whenInsert(Procedure f) {

        }

        @Override
        public void propagateOnInsert(Constraint c) {

        }

        @Override
        public void whenDomainChange(Procedure f) {

        }

        @Override
        public void propagateOnDomainChange(Constraint c) {

        }

        @Override
        public void whenExclude(Procedure f) {

        }

        @Override
        public void propagateOnExclude(Constraint c) {

        }

        @Override
        public void whenBind(Procedure f) {

        }

        @Override
        public void propagateOnBind(Constraint c) {

        }
    }

    protected void scheduleAll(StateStack<Constraint> constraints) {
        for (int i = 0; i < constraints.size(); i++)
            cp.schedule(constraints.get(i));
    }
}
