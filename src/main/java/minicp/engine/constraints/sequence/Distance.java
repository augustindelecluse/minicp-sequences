package minicp.engine.constraints.sequence;

import minicp.engine.core.*;
import minicp.util.exception.InconsistencyException;

/**
 * compute the distance of the sequence, assuming that all nodes must be visited
 */
public class Distance extends AbstractConstraint {

    private final SequenceVar seq;
    private final IntVar distance;
    private final int[][] transition;
    private int[] possible;   // store the possible InsertionsVar
    private int[] insertions; // store the insertions points for an insertion var

    /**
     * compute the lower bound on the distance of a sequence, assuming that all nodes must be visited in the sequence
     * @param sequenceVar sequence whose nodes need to be visited
     * @param transition transition from one node to another
     * @param distance distance of the sequence
     */
    public Distance(SequenceVar sequenceVar, int[][] transition, IntVar distance) {
        super(sequenceVar.getSolver());
        this.transition = transition;
        this.distance = distance;
        this.seq = sequenceVar;
        possible = new int[sequenceVar.nNodes()];
        insertions = new int[sequenceVar.nNodes() + 2];
    }

    @Override
    public void post() {
        if (seq.nExcludedNode() > 0)
            throw InconsistencyException.INCONSISTENCY;
        propagate();
        if (isActive()) {
            seq.whenExclude(() -> {
                throw InconsistencyException.INCONSISTENCY;
            });
            seq.propagateOnInsert(this);
            distance.propagateOnBoundChange(this);
        }
    }

    @Override
    public void propagate() {
        setActive(false);
        int dist = computeCurrentDistance();
        distance.removeBelow(dist);
        setDistanceToCompleteRoute(dist);
        if (!seq.isBound())
            setActive(true);
    }

    // current distance of the sequence
    private int computeCurrentDistance() {
        int n = seq.fillOrder(insertions, true);
        int distance = 0;
        for (int i = 1 ; i < n ; ++i)
            distance += transition[insertions[i]][insertions[i-1]]; // increase the distance
        return distance;
    }

    // minimal distance that must be added to complete the route
    private void setDistanceToCompleteRoute(int currentDist) {
        // TODO check: currently incorrect, need to also account for possible insertions
        int nPossible = seq.fillPossible(possible);
        int minDetourSum = 0;
        int maxDetourSum = 0;
        for (int i = 0; i < nPossible ; ++i) {
            int current = possible[i];
            int nInsert = seq.fillScheduledInsertions(current, insertions);
            int minDetour = Integer.MAX_VALUE;
            int maxDetour = Integer.MIN_VALUE;
            for (int j = 0 ; j < nInsert ; ++j) {
                int pred = insertions[j];
                int succ = seq.nextMember(pred);
                int detour = transition[pred][current] + transition[current][succ] - transition[pred][succ];
                if (detour + currentDist > distance.max()) {
                    seq.removeInsertion(current, pred); // cannot reach the node using this predecessor
                } else {
                    minDetour = Math.min(minDetour, detour);
                    maxDetour = Math.max(maxDetour, detour);
                }
            }
            if (minDetour == Integer.MAX_VALUE) {
                // no scheduled insertion could be found for the node and no insertion will be found in the future
                // the node will never be scheduled, inconsistency detected
                throw InconsistencyException.INCONSISTENCY;
            }
            minDetourSum += minDetour;
            maxDetourSum += maxDetour;
        }
        distance.removeAbove(currentDist + maxDetourSum);
        distance.removeBelow(currentDist + minDetourSum);
    }
}
