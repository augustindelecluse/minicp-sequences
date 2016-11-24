package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class Dependence1D extends AbstractConstraint {

    private SequenceVar[] s;
    private int[] nodes;

    // TODO optimize using insertionVar propagation

    /**
     * ensure that all nodes in the array are present in some sequence or all absent from all sequences
     * the same node cannot be scheduled in different sequences
     * @param s sequences
     * @param nodes nodes that must all be present or all absent
     */
    public Dependence1D(SequenceVar[] s, int... nodes) {
        super(s[0].getSolver());
        this.s = s;
        this.nodes = nodes;
    }

    @Override
    public void post() {
        propagate();
        if (isActive()) {
            for (int node: nodes) {
                for (SequenceVar seq : s) {
                    seq.getInsertionVar(node).propagateOnBind(this);
                }
            }
        }
    }

    @Override
    public void propagate() {
        // look if some node has been scheduled
        boolean oneNodeScheduled = false;
        for (int node: nodes) {
            boolean foundScheduled = false;
            for (SequenceVar seq: s) {
                if (seq.isScheduled(node)) {
                    if (foundScheduled)// a given node has been scheduled in 2 sequences
                        throw INCONSISTENCY;
                    oneNodeScheduled = true; // some node is scheduled
                    foundScheduled = true;
                }
            }
        }
        // if some node is scheduled, the other nodes in the array cannot be excluded from all sequences
        for (int node: nodes) {
            boolean allExcluded = true;
            for (SequenceVar seq: s) {
                if (seq.isPossible(node) || seq.isScheduled(node)) {
                    allExcluded = false;
                    break;
                }
            }
            if (allExcluded) { // a node is excluded from all sequences
                if (oneNodeScheduled) { // but another node was scheduled
                    throw INCONSISTENCY; // inconsistent
                } else { // no scheduled node was found
                    // exclude all other nodes in the array from all the other sequences
                    for (SequenceVar seq: s) {
                        for (int n: nodes) {
                            seq.exclude(n);
                        }
                    }
                    setActive(false); // end of constraint
                    return;
                }
            }
        }
    }
}
