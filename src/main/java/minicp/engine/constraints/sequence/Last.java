package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * ensure that one node among a set of node is scheduled last in the sequence
 * if the set of nodes is a singleton, schedule it as last immediately
 * otherwise set one of the nodes provided as last and exclude the others
 */
public class Last extends AbstractConstraint {

    private final SequenceVar sequenceVar;
    //private int[] nodes;
    private Set<Integer> nodes; // TODO use sparse set instead of set
    private final int[] insertions;
    private final int end;

    public Last(SequenceVar sequenceVar, int... nodes) {
        super(sequenceVar.getSolver());
        this.sequenceVar = sequenceVar;
        this.nodes = Arrays.stream(nodes).boxed().collect(Collectors.toSet());
        end = sequenceVar.end();
        insertions = new int[sequenceVar.nNodes()];
    }

    @Override
    public void post() {
        if (nodes.size() == 1) {
            int node = nodes.iterator().next();
            sequenceVar.schedule(node, sequenceVar.predMember(sequenceVar.end()));
            sequenceVar.removeInsertionAfter(node);
            setActive(false);
            return;
        }

        // exclude all insertions where the nodes in the list are predecessors
        int size = sequenceVar.fillPossible(insertions);
        for (int lastNode: nodes) {
            for (int i = 0 ; i < size; ++i) {
                int node = insertions[i];
                sequenceVar.removeInsertion(node, lastNode);  // no node can have a node in the list as predecessor
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
        // ensure that one node in the list can still be scheduled as the last node
        int lastNode = sequenceVar.predMember(end);
        int nValid = 0;
        int validNode = 0;
        int nScheduled = 0;
        int scheduledNode = 0;
        // remove the insertions for the first nodes in the sequence as the nodes must be scheduled last
        int size = sequenceVar.fillOrder(insertions, true);
        if (size != 2) { // size == 2 if the sequence contains only the beginning and end node
            for (int node: nodes) {
                for (int i = 0; i < size - 2; ++i) {
                    int pred = insertions[i];
                    sequenceVar.removeInsertion(node, pred); // the node must be scheduled last in the sequence
                }
            }
        }

        // look at the current appearance of nodes in the sequence
        for (int node : nodes) {
            if (sequenceVar.isScheduled(node)) {
                if (++nScheduled > 1 || sequenceVar.nextMember(node) != end)
                    throw INCONSISTENCY; // two scheduled nodes have been found or the next member is not the last one
                scheduledNode = node;
            } else if (sequenceVar.isInsertion(node, lastNode)) {
                ++nValid;
                validNode = node;
            }
        }

        if (nScheduled == 1) { // one of the nodes has been scheduled
            excludeAllInListBut(scheduledNode);
            setActive(false);
        } else if (nValid == 1) { // only one node is valid in the set
            // if no node can be scheduled after the current last node, schedule the candidate now
            size = sequenceVar.fillPossible(insertions);
            boolean isTrulyLast = true;
            for (int i = 0; i < size && isTrulyLast; ++i) {
                int node = insertions[i];
                // if a node different from the supposed last can be scheduled, it might not be the final last node of the sequence
                if (sequenceVar.isInsertion(node, lastNode) && node != validNode) {
                    isTrulyLast = false;
                }
            }
            if (isTrulyLast) { // the last node currently in the sequence is truly the last node
                sequenceVar.schedule(validNode, lastNode);
                excludeAllInListBut(validNode);
                setActive(false);
            }

        }
    }

    // exclude all nodes in the nodes array except the specified node
    private void excludeAllInListBut(int nodeToKeep) {
        for (int node: nodes) {
            if (node != nodeToKeep) { // exclude the node from the sequence
                sequenceVar.exclude(node);
            }
        }
    }
}
