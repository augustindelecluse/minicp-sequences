package minicp.engine.constraints.sequence;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.SequenceVar;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Cumulative constraint over a set of activities
 * Each activity begins and ends at a given node and consumes a load when executed
 * Ensure that the maximum load is never exceeded
 */
public class Cumulative extends AbstractConstraint {

    private SequenceVar sequenceVar;
    private int[] starts;
    private int[] ends;
    private int maxCapa;
    private int[] capacity;

    private int[] capaBefore;
    private int[] capaAfter;
    private int[] order;
    private int[] inserts1;
    private int[] inserts2;
    private int[] nodeIdx; // contain the mapping node -> node index in the starts and ends array
    private HashSet<Integer> activeActs = new HashSet<>();
    private HashSet<Integer> activeActsToRemove = new HashSet<>();

    HashSet<Integer> startInsertions = new HashSet<>();
    HashSet<Integer> endInsertions = new HashSet<>();
    HashSet<Integer> currentStarts = new HashSet<>(); // valid starts when trying to insert a node

    public Cumulative(SequenceVar sequenceVar, int[] starts, int[] ends, int maxCapa, int[] capacity) {
        super(sequenceVar.getSolver());
        this.starts = starts;
        this.ends = ends;
        this.maxCapa = maxCapa;
        if (capacity.length == sequenceVar.nNodes() + 2)
            this.capacity = capacity;
        else {
            int[] trueCapacity = new int[sequenceVar.nNodes() + 2];
            for (int i = 0; i < starts.length ; ++i) {
                trueCapacity[starts[i]] = capacity[starts[i]];
                trueCapacity[ends[i]] = capacity[ends[i]];
            }
            this.capacity = trueCapacity;
        }
        this.sequenceVar = sequenceVar;

        capaBefore = new int[Math.max(sequenceVar.begin(), sequenceVar.end())+1];
        capaAfter = new int[Math.max(sequenceVar.begin(), sequenceVar.end())+1];
        order = new int[sequenceVar.nNodes() + 2];
        inserts1 = new int[sequenceVar.nNodes()];
        inserts2 = new int[sequenceVar.nNodes()];

        nodeIdx = new int[sequenceVar.nNodes() + 2];
        Arrays.fill(nodeIdx, -1);
        for (int i = 0 ; i < starts.length; ++i) {
            nodeIdx[starts[i]] = i;
            nodeIdx[ends[i]] = i;
        }
    }

    @Override
    public void post() {
        for (int i = 0; i < starts.length; ++i) // the starts must appear before the ends
            getSolver().post(new Precedence(sequenceVar, starts[i], ends[i]));
        // filter the possible insertions
        propagate();
        // register propagators
        int size = sequenceVar.fillPossible(order);
        for (int i = 0; i < size; ++i) {
            int insert = order[i];
            sequenceVar.getInsertionVar(insert).propagateOnInsert(this);
        }
    }

    @Override
    public void propagate() {
        // store the order
        sequenceVar.fillOrder(order);
        buildProfile();
        filterPartiallyInserted();
        filterNonInserted();
    }

    private void buildProfile() {
        // fully inserted activities
        int n = sequenceVar.nScheduledNode() + 2;
        int currentLoad = 0;
        int activity;
        for (int i = 1; i < n-1; ++i) {
            capaBefore[order[i]] = currentLoad;
            activity = indexOf(order[i]);
            if (activity != -1 && isStartInserted(activity) && isEndInserted(activity)) {
                currentLoad += capacity[order[i]];
                if (currentLoad < 0 || currentLoad > maxCapa)
                    throw INCONSISTENCY;
            }
            capaAfter[order[i]] = currentLoad;
        }
        // start fixed and end not fixed
        activeActs.clear();
        activeActsToRemove.clear();
        currentLoad = 0;
        for (int i = 1; i < n-1; ++i) {
            capaBefore[order[i]] += currentLoad;

            if (capaBefore[order[i]] < 0 || capaBefore[order[i]] > maxCapa)
                throw INCONSISTENCY;
            activity = indexOf(order[i]);
            if (activity != -1 && isStartInserted(activity) && !isEndInserted(activity)) { // add the start
                currentLoad += capacity[order[i]];
                activeActs.add(activity);
            }
            capaAfter[order[i]] += currentLoad;
            if (capaAfter[order[i]] < 0 || capaAfter[order[i]] > maxCapa)
                throw INCONSISTENCY;
            // attempt to find a matching end
            //for (Iterator<Integer> it = activeActs.stream().sorted(Comparator.comparingInt(j -> -capacity[j])).iterator(); it.hasNext(); ) {
            for (int active: activeActs) {
                //int active = it.next();
                if (sequenceVar.canSchedule(ends[active], order[i])) {
                    //activeActs.remove(active);
                    activeActsToRemove.add(active);
                    currentLoad += capacity[ends[active]];
                }
            }
            if (!activeActsToRemove.isEmpty()) {
                for (int active: activeActsToRemove)
                    activeActs.remove(active);
                activeActsToRemove.clear();
            }
        }
        if (!activeActs.isEmpty())
            throw INCONSISTENCY;
        // end fixed and start not fixed
        currentLoad = 0;
        for (int i = n-2; i >= 0; --i) {
            // attempt to find a matching start
            //for (Iterator<Integer> it = activeActs.stream().sorted(Comparator.comparingInt(j -> -capacity[j])).iterator(); it.hasNext(); ) {
            for (int active: activeActs) {
                //int active = it.next();
                if (sequenceVar.canSchedule(starts[active], order[i])) {
                    //activeActs.remove(active);
                    activeActsToRemove.add(active);
                    currentLoad -= capacity[starts[active]];
                }
            }
            if (!activeActsToRemove.isEmpty()) {
                for (int active: activeActsToRemove)
                    activeActs.remove(active);
                activeActsToRemove.clear();
            }
            capaAfter[order[i]] += currentLoad;
            if (capaAfter[order[i]] < 0 || capaAfter[order[i]] > maxCapa)
                throw INCONSISTENCY;
            activity = indexOf(order[i]);
            if (activity != -1 && !isStartInserted(activity) && isEndInserted(activity)) { // add the end
                currentLoad -= capacity[order[i]];
                activeActs.add(activity);
            }
            capaBefore[order[i]] += currentLoad;
            if (capaBefore[order[i]] < 0 || capaBefore[order[i]] > maxCapa)
                throw INCONSISTENCY;
        }
        if (!activeActs.isEmpty())
            throw INCONSISTENCY;
    }

    private void filterPartiallyInserted() {
        int n = sequenceVar.nScheduledNode() + 2;
        for (int i = 1; i < n-1 ; ++i) {
            int activity = indexOf(order[i]); // index in the starts and ends array
            if (activity == -1)
                continue;
            boolean startInserted = isStartInserted(activity);
            boolean endInserted = isEndInserted(activity);
            if (startInserted == endInserted) // the start and end are both inserted / not inserted, no need to filter
                continue;
            int start = starts[activity];
            int end = ends[activity];
            if (startInserted) { // filter possible ends
                boolean inProfile = true; // contribution is currently in the profile
                int currentLoad;
                for (int j = i ; j < n ; ++j) {
                    currentLoad = inProfile ? Math.max(capaAfter[order[j]], capaBefore[order[j]]) : Math.max(capaAfter[order[j]], capaBefore[order[j]]) + capacity[start];
                    if (currentLoad > maxCapa) {
                        for (; j < n ; ++j)
                            sequenceVar.removeInsertion(end, order[j]);
                    } else {
                        if (inProfile && sequenceVar.isInsertion(end, order[j])) // first valid insertion point reached, not in the profile anymore
                            inProfile = false;
                    }
                }
            } else { // filter possible starts for the partially inserted activity

                boolean inProfile = true;
                boolean invalid = false;
                int current = -1;
                int currentLoad;
                for (int j = i ; j > 0; --j) {
                    if (!invalid) {  // the node can still be inserted
                        current = order[j];
                        if (inProfile) {
                            currentLoad = capaBefore[current];
                            int pred = order[j - 1];
                            if (sequenceVar.isInsertion(start, pred))
                                inProfile = false;  // not anymore in the load profile
                        } else {
                            currentLoad = capaBefore[current] + capacity[start];
                        }
                        invalid = currentLoad > maxCapa; // the node is now invalid
                        if (invalid)
                            --j;
                    }
                    if (invalid) {
                        for (; j >= 0; --j) {// remove the starts that would exceed the maximum capacity
                            current = order[j];
                            sequenceVar.removeInsertion(start, current);
                        }
                        break;
                    }
                    if (inProfile) {
                        if (capaAfter[current] > maxCapa)
                            invalid = true;
                    } else {
                        if (capaAfter[current] + capacity[start] > maxCapa)
                            invalid = true;
                    }
                }

            }
        }
    }

    private void filterNonInserted() {
        int n = sequenceVar.nScheduledNode() + 2;
        for (int activity = 0 ; activity < starts.length ; ++activity) {
            if ((!isEndInserted(activity) && !isStartInserted(activity))) {
                int start = starts[activity];
                int end = ends[activity];
                boolean canClose = false;
                startInsertions.clear();
                endInsertions.clear();
                currentStarts.clear();
                int size1 = sequenceVar.fillScheduledInsertions(start, inserts1);
                for (int i = 0; i < size1; ++i)
                    startInsertions.add(inserts1[i]);
                int size2 = sequenceVar.fillScheduledInsertions(start, inserts2);
                for (int i = 0; i < size2; ++i)
                    endInsertions.add(inserts2[i]);
                // iterate over the sequence
                for (int i = 0; i < n - 1 && !startInsertions.isEmpty() && !endInsertions.isEmpty(); ++i) {
                    // as long as we have not met the end and that there are still starts ad ends that can be considered
                    if (sequenceVar.isInsertion(start, order[i])) { // if the start can be inserted, consider it as active
                        currentStarts.add(order[i]);
                        canClose = true;
                    }
                    int idx = indexOf(order[i]);
                    // is the current node that we are looking at partially inserted?
                    boolean partiallyInserted = idx != -1 && isStartInserted(idx) != isEndInserted(idx);
                    // compute the capacity when reaching the node
                    int capaAtNode = (partiallyInserted ? Math.min(capaAfter[order[i]], capaBefore[order[i]]): capaAfter[order[i]]) + capacity[start];
                    // if some starts needs to be considered and that the capacity when reaching the current node is too high
                    if (!currentStarts.isEmpty() && (capaAtNode > maxCapa)) {
                        currentStarts.clear();
                        canClose = false;
                    }
                    // if the starts can be closed, remove them
                    if (canClose && sequenceVar.isInsertion(end, order[i])) {
                        endInsertions.remove(order[i]);
                        for (int si : currentStarts)
                            startInsertions.remove(si);
                        currentStarts.clear();
                    }
                }
                for (int si : startInsertions)
                    sequenceVar.removeInsertion(start, si);
                for (int si : endInsertions)
                    sequenceVar.removeInsertion(end, si);
            }
        }
    }

    private int indexOf(int node) {
        return node < nodeIdx.length ? nodeIdx[node] : -1;
    }

    private boolean isEndInserted(int activity) {
        return sequenceVar.isScheduled(ends[activity]);
    }

    private boolean isStartInserted(int activity) {
        return sequenceVar.isScheduled(starts[activity]);
    }
}
