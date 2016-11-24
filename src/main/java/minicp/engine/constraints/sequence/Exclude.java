package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;
import minicp.util.exception.InconsistencyException;

/**
 * exclude a node from a sequence
 */
public class Exclude extends AbstractConstraint {

    private final int[] excluded;
    private final SequenceVar sequenceVar;

    /**
     * exclusion constraint. Ensure that a node / set of nodes must belong to the set of excluded nodes
     * @param sequenceVar: sequence var to work on
     * @param nodes : node / set of nodes to exclude
     */
    public Exclude(SequenceVar sequenceVar, int... nodes) {
        super(sequenceVar.getSolver());
        excluded = nodes;
        this.sequenceVar = sequenceVar;
    }

    @Override
    public void post() {
        for (int node: excluded) {
            if (sequenceVar.isScheduled(node))
                throw new InconsistencyException();
            sequenceVar.exclude(node);
        }
        setActive(false);
    }
}
