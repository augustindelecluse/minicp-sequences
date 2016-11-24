package minicp.engine.constraints.sequence;

import minicp.engine.core.*;
import minicp.util.exception.InconsistencyException;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class Disjoint extends AbstractConstraint {

    private final SequenceVar[] s;
    private final boolean mustAppear;

    /**
     * disjoint constraint. Ensures that the same node id must be scheduled once and only once across all sequences
     * a node can be excluded from all sequences
     * @param s array of SequenceVar to post the constraint on
     */
    public Disjoint(SequenceVar... s) {
        this(true, s);
    }

    /**
     * disjoint constraint. Ensures that the same node id must be scheduled once and only once across all sequences
     * @param s array of SequenceVar to post the constraint on
     * @param mustAppear if true, each unique node must be scheduled in one sequence
     */
    public Disjoint(boolean mustAppear, SequenceVar... s) {
        super(s[0].getSolver());
        this.s = s;
        this.mustAppear = mustAppear;
    }

    @Override
    public void post() {
        // TODO create subclass for each node, use setActive on it
        // use hashset to set the nodes and so on



        if (mustAppear && s.length == 1) { // all nodes must be visited and only one sequence is in the set
            if (s[0].nExcludedNode() > 0)
                throw INCONSISTENCY;
            s[0].whenExclude(() -> {throw INCONSISTENCY;});
            //setActive(false); // not needed in this case
            return;
        }

        // find the maximum number of nodes for the sequences
        int maxRequests = Integer.MIN_VALUE;
        int nbRequests;
        for (SequenceVar seq: s) {
            nbRequests = seq.nNodes() + 2;
            if (nbRequests > maxRequests)
                maxRequests = nbRequests;
        }
        int[] requests = new int[maxRequests];
        int size;
        for (int i=0; i < s.length ; ++i) { // look at the scheduled nodes of each sequence s[i]
            size = s[i].fillScheduled(requests);
            if (size != 0) { // some nodes are scheduled in s[i]
                for (int j = i + 1; j < s.length; ++j) { // for all others sequences s[j]
                    for (int k = 0; k < size; ++k) { // for all scheduled nodes in s[i]
                        if (s[j].isScheduled(requests[k])) // scheduled node in s[i] appears twice, inconsistency
                            throw INCONSISTENCY;
                        else if (s[j].isPossible(requests[k])) // a node can be scheduled once, exclude it from the other sequences
                            s[j].exclude(requests[k]);
                    }
                }
            }
            if (mustAppear) {
                // no node can be excluded from all sequences
                size = s[i].fillExcluded(requests);
                if (size != 0) { // some nodes are scheduled in s[i]
                    for (int k = 0; k < size; ++k) { // for all excluded nodes in s[i]
                        int node = requests[k];
                        boolean canBeScheduled = false;
                        for (int j = 0; j < s.length && !canBeScheduled; ++j) { // for all other sequences s[j]
                            if (i == j)
                                continue;
                            if (!s[j].isExcluded(node))
                                canBeScheduled = true;
                        }
                        if (!canBeScheduled)
                            throw INCONSISTENCY;
                    }
                }
            }
        }


        // propagate on insert for all remaining nodes in the sequences
        int[] insertions = new int[maxRequests];
        for (SequenceVar seq: s) {
            size = seq.fillPossible(insertions);
            for (int i=0; i < size; ++i) {
                InsertionVar insertionVar = seq.getInsertionVar(insertions[i]);
                final int id = insertionVar.node();
                insertionVar.whenInsert(() -> excludeOnInsert(id));
                if (mustAppear)
                    insertionVar.whenExclude(() -> mustBeScheduledOnce(id));
            }
        }
    }

    /**
     * when a node i is scheduled in a sequence, exclude it from the other sequences
     * ensures that the node is scheduled once in all sequences
     * @param i
     */
    public void excludeOnInsert(int i) {
        boolean foundScheduled = false;
        for (SequenceVar seq: s) {
            if (seq.isPossible(i))
                seq.exclude(i);
            else if (seq.isScheduled(i)) {
                if (foundScheduled) // the node needs to be found once
                    throw INCONSISTENCY;
                foundScheduled = true;
            }
        }
    }

    /**
     * when a node i is scheduled in a sequence, exclude it from the other sequences
     * ensures that the node is scheduled once in all sequences
     * @param i
     */
    public void mustBeScheduledOnce(int i) {
        boolean foundScheduled = false;
        for (SequenceVar seq: s) {
            if (!seq.isExcluded(i)) {
                foundScheduled = true;
                break;
            }
        }
        if (!foundScheduled)
            throw INCONSISTENCY; // the node cannot be scheduled anymore
    }
}
