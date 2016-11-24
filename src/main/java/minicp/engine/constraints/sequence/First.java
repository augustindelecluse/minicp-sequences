package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.SequenceVar;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

/**
 * ensure that one node among a set of node is scheduled first in the sequence
 * if the set of nodes is a singleton, schedule it as first immediately
 * otherwise set one of the nodes provided as first and exclude the others
 */
public class First extends AbstractConstraint {

    private final SequenceVar sequenceVar;
    private final Set<Integer> nodes;
    private final int begin;
    private final int[] insertions;
    private final boolean setImmediately;

    /**
     * ensure that the first node visited is amongst a list of specified node
     * @param sequenceVar sequence whose first node needs to be set
     * @param nodes candidates for the first node of the sequence
     */
    public First(SequenceVar sequenceVar, int... nodes) {
        this(sequenceVar, false, nodes);
    }

    /**
     *
     * @param sequenceVar sequence whose first node needs to be set
     * @param setImmediately if true, all possibles nodes in the sequence except the specified nodes will not have
     *                       the beginning node as predecessor
     *                       Otherwise, other nodes can have the beginning node as a valid predecessor (but only one
     *                       node in the list will be set as the final successor of the beginning node)
     * @param nodes candidates for the first node of the sequence
     */
    public First(SequenceVar sequenceVar, boolean setImmediately, int... nodes) {
        super(sequenceVar.getSolver());
        this.sequenceVar = sequenceVar;
        this.nodes = Arrays.stream(nodes).boxed().collect(Collectors.toSet());
        insertions = new int[sequenceVar.nNodes()];
        begin = sequenceVar.begin();
        this.setImmediately = setImmediately;
    }

    @Override
    public void post() {
        if (nodes.size() == 1) {
            sequenceVar.schedule(nodes.iterator().next(), sequenceVar.begin());
            sequenceVar.removeInsertionAfter(sequenceVar.begin());
            setActive(false);
            return;
        }

        // the only valid predecessor for a node in the list is the beginning node
        int nScheduled = 0;
        int scheduledNode = 0;
        for (int node: nodes) {
            if (sequenceVar.isScheduled(node)) {
                if (++nScheduled > 1 || sequenceVar.predMember(node) != begin) {
                    throw INCONSISTENCY; // 2 nodes in the list appear / the node does not have begin as predecessor
                }
                scheduledNode = node;
            } else if (nScheduled == 0) {
                if (sequenceVar.isInsertion(node, begin))
                    sequenceVar.getInsertionVar(node).removeAllInsertBut(begin);
                else
                    sequenceVar.exclude(node);
            }
        }
        if (nScheduled == 1) { // one node has been found as valid
            excludeAllInListBut(scheduledNode);
            excludeAllInsertBegin();
            setActive(false);
        } else {

            if (setImmediately) { // remove all insertions where begin is the predecessor and the node is not in the list
                int first = sequenceVar.nextMember(begin);
                if (first != sequenceVar.end())
                    throw INCONSISTENCY; // the sequence contains one node after its beginning which is not the end node
                int size = sequenceVar.fillPossible(insertions);
                for (int i = 0 ; i < size; ++i) {
                    int node = insertions[i];
                    if (!nodes.contains(node)) {
                        sequenceVar.removeInsertion(node, begin);
                    }
                }
            }

            propagate();
            if (isActive()) {
                sequenceVar.propagateOnInsert(this);
                sequenceVar.propagateOnExclude(this);
            }
        }
    }

    @Override
    public void propagate() {
        // ensure that one node in the list can still be scheduled after the beginning node
        int nValid = 0;
        int validNode = 0;
        int nScheduled = 0;
        int scheduledNode = 0;
        for (int node : nodes) {
            if (sequenceVar.isScheduled(node)) {
                if (++nScheduled > 1 || sequenceVar.predMember(node) != begin) // two scheduled nodes have been found
                    throw INCONSISTENCY;
                scheduledNode = node;
            } else if (sequenceVar.isInsertion(node, begin)) {
                ++nValid;
                validNode = node;
            }
        }

        if (nScheduled == 1) { // one of the nodes has been scheduled
            setActive(false);
            excludeAllInListBut(scheduledNode);
            excludeAllInsertBegin();
        } else if (nValid == 1) { // only one node is valid in the set, schedule it now
            setActive(false);
            sequenceVar.schedule(validNode, begin);
            excludeAllInListBut(validNode);
            excludeAllInsertBegin();
        } else if (nValid == 0) { // no node in the set can have the beginning node as a predecessor, failure
            throw INCONSISTENCY;
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

    // exclude all insertions having the beginning node as a predecessor
    private void excludeAllInsertBegin() {
        int size = sequenceVar.fillPossible(insertions);
        for (int i = 0 ; i < size; ++i) {
            int node = insertions[i];
            sequenceVar.exclude(node);
        }
    }
}
