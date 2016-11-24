package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.SequenceVar;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class TSPTW extends AbstractConstraint {

    private final SequenceVar seq; // sequence
    private final IntVar[] time; // time window
    private final IntVar distance; // distance of the sequence
    private final int[][] transition; // transition cost matrix

    private final int[] nodes; // used by fill operations on the sequence to retrieve nodes
    private final int[] insertions; // used by fill operations on the sequence to retrieve insertions

    private int nPossible; // number of possible nodes. Set at each propagation and reused in the differents methods

    /**
     * create a TSPTW from a sequence variable
     *
     * same effect as using a {@link TransitionTimes} constraint, compute the bounds on the distance and giving a failure
     * when a node is excluded. This constraint is more performant as it reuses the same array without changing them, and
     * is called once per propagation step when it is used alone
     *
     * @param sequenceVar sequence that will perform a TSPTW
     * @param time time of visit for each node
     * @param distance distance of the sequence
     * @param transition transition from one node to another
     */
    public TSPTW(SequenceVar sequenceVar, IntVar[] time, IntVar distance, int[][] transition) {
        super(sequenceVar.getSolver());
        seq = sequenceVar;
        this.time = time;
        this.distance = distance;
        this.transition = transition;

        nodes = new int[sequenceVar.nNodes() + 2];
        insertions = new int[sequenceVar.nNodes()];
    }

    @Override
    public void post() {
        if (seq.nExcludedNode() > 0)
            throw INCONSISTENCY; // no node can be excluded in a TSPTW
        seq.whenExclude(() -> {throw INCONSISTENCY;});
        updatePossibleInsertions();
        propagate();
        seq.propagateOnInsert(this);
    }

    @Override
    public void propagate() {
        setActive(false); // prevent trigger of itself by the fixpoint
        // update time window and compute cost
        int currentDistance = updateTimeWindow();
        // rest of the pruning will occur based on the possible nodes
        nPossible = seq.fillPossible(nodes);
        // prune insertions that would visit a node outside its tw
        updateScheduledInsertionsFromTW();
        // set the new bounds for the cost
        setBoundDistance(currentDistance);
        // prune insertions that would exceed the distance
        updateScheduledInsertionsFromDist(currentDistance);
        if (!seq.isBound())
            setActive(true);
    }

    /**
     * update the bounds for the time window and compute the current distance of the sequence
     * @return current distance of the sequence
     */
    public int updateTimeWindow() {
        // update min window and compute cost
        int n = seq.fillOrder(nodes, true);
        int distance = 0;
        int current = nodes[1];
        int pred = nodes[0];
        int predTime = time[pred].min();

        for (int i = 1 ; i < n - 1 ; ++i) {
            int dist = transition[pred][current];
            predTime += dist;
            distance += dist;

            time[current].removeBelow(predTime);
            predTime = Math.max(predTime, time[current].min()); // waiting at a node is allowed

            pred = current;
            if (i < n-1)
                current = nodes[i+1];
        }
        distance += transition[pred][nodes[n-1]];

        // update max window
        int succ = nodes[n-1];
        current = nodes[n-2];
        int succTime = time[succ].max();
        for (int i = n - 2 ; i >= 0 ; --i) {
            succTime -= transition[current][succ];
            time[current].removeAbove(succTime);
            succTime = Math.min(succTime, time[current].max()); // waiting at a node is allowed

            succ = current;
            if (i > 0)
                current = nodes[i-1];
        }
        // return current distance of the sequence
        return distance;
    }

    /**
     * update insertions from a possible node up to a possible node
     * {@link TSPTW#nodes} must be filled with the possible nodes of the sequence
     * and {@link TSPTW#nPossible} is set to the number of possible nodes
     */
    public void updatePossibleInsertions() {
        nPossible = seq.fillPossible(nodes);
        for (int i = 0; i < nPossible; ++i) {
            int current = nodes[i];
            int nbInsert = seq.fillPossibleInsertions(current, insertions); // only use the possible insertions
            for (int j = 0; j < nbInsert; ++j) {
                int pred = insertions[j];
                // if the min time of the insert does not allow to reach the node within its own max time, remove it
                if (time[pred].min() + transition[pred][current] > time[current].max())
                    seq.removeInsertion(current, pred);
            }
        }
    }

    /**
     * remove insertions for nodes based on the values of their time window
     * {@link TSPTW#nodes} must be filled with the possible nodes of the sequence
     * and {@link TSPTW#nPossible} is set to the number of possible nodes
     */
    public void updateScheduledInsertionsFromTW() {
        for (int i = 0; i < nPossible; ++i) { // for all possible insertion ...
            int current = nodes[i];
            int nInsert = seq.fillScheduledInsertions(current, insertions);
            boolean foundInsert = false;
            for (int j = 0; j < nInsert; ++j) { // for all of its scheduled insertion point candidate ...
                int pred = insertions[j];  // check that .. -> pred -> current -> succ -> .. is feasible
                int succ = seq.nextMember(pred); // successor of the insertion
                int timeReachingNode = time[pred].min() + transition[pred][current];
                if (timeReachingNode > time[current].max()) // check that pred -> current is feasible
                    seq.removeInsertion(current, pred);
                else { // check that current -> succ is feasible
                    int timeDeparture = Math.max(timeReachingNode, time[current].min());
                    if (timeDeparture + transition[current][succ] > time[succ].max())
                        seq.removeInsertion(current, pred);
                    else
                        foundInsert = true;
                }
            }
            if (!foundInsert) { // no scheduled insertion point exists for this node, inconsistency
                throw INCONSISTENCY;
            }
        }
    }

    /**
     * set the new min and max for the distance
     * {@link TSPTW#nodes} must be filled with the possible nodes of the sequence
     * and {@link TSPTW#nPossible} is set to the number of possible nodes
     * @param currentDistance current distance of the sequence (exact value)
     */
    public void setBoundDistance(int currentDistance) {
        if (seq.nScheduledNode() == 0)
            return;
        int minDetourSum = 0;
        int maxDetourSum = 0;
        for (int i = 0; i < nPossible ; ++i) {
            int current = nodes[i];
            int nInsert = seq.fillInsertions(current, insertions);
            int minDetour = Integer.MAX_VALUE;
            int maxDetour = Integer.MIN_VALUE;
            boolean validInsert = false;
            for (int j = 0 ; j < nInsert ; ++j) {
                int pred = insertions[j];
                int succ = seq.nextMember(pred);
                int detour;
                if (seq.isScheduled(pred)) {
                    // increase the current distance by inserting the node into the route
                    detour = transition[pred][current] + transition[current][succ] - transition[pred][succ];
                } else {
                    detour = transition[pred][current] + transition[current][pred]; // multiply by 2 as we must go back to origin
                }
                if (detour + currentDistance > distance.max()) {
                    seq.removeInsertion(current, pred); // cannot reach the node using this predecessor
                } else {
                    validInsert = true;
                    minDetour = Math.min(minDetour, detour);
                    maxDetour = Math.max(maxDetour, detour);
                }
            }
            if (!validInsert) {
                // no insertion for this node has been found as valid. Should be detected by the SequenceVar invariant
                throw INCONSISTENCY;
            }
            if (minDetour != Integer.MAX_VALUE) {
                minDetourSum += minDetour;
                maxDetourSum += maxDetour;
            }
        }
        if (minDetourSum != 0) {
            distance.removeAbove(currentDistance + maxDetourSum);
            distance.removeBelow(currentDistance + minDetourSum);
        } else if (seq.isBound()) {
            distance.assign(currentDistance);
        }
    }

    /**
     * remove insertions using the maximum detour allowed
     * {@link TSPTW#nodes} must be filled with the possible nodes of the sequence
     * and {@link TSPTW#nPossible} is set to the number of possible nodes
     * @param currentDistance current distance of the sequence (exact value)
     */
    public void updateScheduledInsertionsFromDist(int currentDistance) {
        int maxDetour = distance.max() - currentDistance;
        for (int i = 0; i < nPossible; ++i) { // for all possible insertion ...
            int current = nodes[i];
            int nInsert = seq.fillScheduledInsertions(current, insertions);
            boolean foundInsert = false;
            for (int j = 0; j < nInsert; ++j) { // for all of its scheduled insertion point candidate ...
                int pred = insertions[j];  // check that the detour is not too costly
                int succ = seq.nextMember(pred); // successor of the insertion
                int detour = transition[pred][current] + transition[current][succ] - transition[pred][succ];
                if (detour > maxDetour) { // maximum detour allowed
                    seq.removeInsertion(current, pred);
                } else {
                    foundInsert = true;
                }
            }
            if (!foundInsert) { // no scheduled insertion point exists for this node, inconsistency
                throw INCONSISTENCY;
            }
        }
    }
}
