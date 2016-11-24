package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;

/**
 * exclude all possible nodes of the sequence, binding it
 */
public class ExcludeAllPossible extends AbstractConstraint {

    SequenceVar[] sequenceVars;

    /**
     * exclude all possible nodes from the sequence
     * @param sequenceVar sequence var whose possible nodes will be removed
     */
    public ExcludeAllPossible(SequenceVar... sequenceVar) {
        super(sequenceVar[0].getSolver());
        this.sequenceVars = sequenceVar;
    }

    @Override
    public void post() {
        for (SequenceVar seq: sequenceVars)
            seq.excludeAllPossible();
        setActive(false);
    }
}
