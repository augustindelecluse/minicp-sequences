package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;

public class Schedule extends AbstractConstraint {

    private SequenceVar sequenceVar;
    private int node;
    private int predecessor;

    public Schedule(SequenceVar sequenceVar, int node, int pred) {
        super(sequenceVar.getSolver());
        this.sequenceVar = sequenceVar;
        this.node = node;
        this.predecessor = pred;
    }

    @Override
    public void post() {
        sequenceVar.schedule(node, predecessor);
        setActive(false);
    }
}
