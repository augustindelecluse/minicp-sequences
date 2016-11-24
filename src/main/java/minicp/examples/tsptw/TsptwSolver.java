package minicp.examples.tsptw;


import minicp.engine.constraints.Circuit;
import minicp.engine.constraints.Element1DVar;
import minicp.engine.constraints.LessOrEqual;
import minicp.engine.constraints.sequence.*;
import minicp.engine.core.*;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.util.*;
import java.util.function.BiConsumer;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;


public class TsptwSolver {

    // TODO distance matrix using all pair shortest path for satisfiability

    private int verbosity = 0;

    private final TsptwInstance instance;
    private final int           timeout;
    private final int[]         initial;
    private List<BiConsumer<int[],Integer>> observers = new LinkedList<>();

    private int nNodes;
    private int nNodesWithDepot;
    private int begin;
    private int end;
    private int[][] distances;
    private int[] twStart;
    private int[] twEnd;

    private Solver cp;
    private IntVar[] time; // time window of every node
    private SequenceVar route; // route taken in the TSP
    private IntVar routingCost; // routing cost objective
    private IntVar nVisitedNodes; // TODO enhance number of visited nodes objective

    private int bestNVisited; // best number of visited nodes

    private int[] nodes; // used for fill operations on nodes in the branching
    private int[] insertion; // used for fill operations on insertions in the branching
    private Procedure[] branching; // contain the branching procedures
    private Integer[] heuristicVal; // heuristic values
    private Integer[] branchingRange; // index used to sort using the heuristic values
    private Random random; // used to randomize the branching and the relaxation
    private int seed; // seed used by random

    private int[] visitOrder; // store the visit order within a sequence

    private int[] currentSolOrder; // current solution ordering
    private int[] bestSolOrder; // best solution ordering
    private TsptwResult bestSol; // value for the best solution
    private int[] relaxedNodes; // set of relaxed nodes
    private Set<Integer> relaxed; // set of relaxed nodes
    private final boolean solProvided; // true if an initial solution was provided
    private long init; // time at which the solver has started, in millis

    private ArrayList<int[]> solRegistered;

    public void addObserver(BiConsumer<int[],Integer> observer) {
        observers.add(observer);
    }

    private void notifySolution(int [] solution, int objective) {
        for (BiConsumer<int[],Integer> observer: observers) {
            observer.accept(solution, objective);
        }
    }

    /**
     * initialize a TSP with time window solver
     * @param instance instance to solve
     * @param timeout timeout for the solving [s]
     * @param initial optional initial solution to start from. null if no initial solution is provided
     */
    public TsptwSolver(final TsptwInstance instance, final int timeout, final int[] initial) {
        this.instance = instance;
        this.timeout  = timeout * 1000; // convert to millis
        this.initial  = initial;
        boolean respect = instance.respectTriangularInequality();
        nNodes = instance.nbNodes;
        nNodesWithDepot = nNodes + 1; // node 0: begin node, last node: end depot
        nodes = new int[nNodesWithDepot];
        begin = 0;
        end = nNodes;
        visitOrder = new int[nNodes];
        heuristicVal = new Integer[nNodes];
        branchingRange = new Integer[nNodes];
        branching = new Procedure[nNodes];
        // transition from node to node
        distances = new int[nNodesWithDepot][nNodesWithDepot];
        twStart = new int[nNodesWithDepot];
        twEnd = new int[nNodesWithDepot];
        for (int i = 0 ; i < nNodes ; ++i) {
            System.arraycopy(instance.distances[i], 0, distances[i], 0, nNodes);
            distances[i][end] = distances[i][begin]; // getting to the end node is the same as getting to the beginning node
            twStart[i] = instance.timeWindows[i].getEarliest();
            twEnd[i] = instance.timeWindows[i].getLatest();
        }
        // time window for end node
        twStart[end] = instance.timeWindows[begin].getEarliest();
        twEnd[end] = instance.timeWindows[begin].getLatest();
        seed = 42;
        random = new Random(seed);
        relaxed = new HashSet<>();
        relaxedNodes = new int[nNodes];
        bestSolOrder = new int[nNodesWithDepot];
        currentSolOrder = new int[nNodesWithDepot];
        // solution
        if (initial == null) { // no initial solution given
            bestSol = new TsptwResult(Integer.MAX_VALUE);
            solProvided = false;
            bestNVisited = 0;
        } else { // initial solution provided
            System.arraycopy(initial, 1, bestSolOrder, 1, nNodes - 1);
            bestSolOrder[end] = end;
            System.arraycopy(bestSolOrder, 0, currentSolOrder, 0, nNodesWithDepot);
            bestSol = new TsptwResult(instance.cost(initial));
            solProvided = true;
            bestNVisited = nNodesWithDepot;
        }
        insertion = new int[nNodes];
        init = System.currentTimeMillis();
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    public int getSeed() {
        return seed;
    }

    /**
     * give the best result found in the available time
     * @return best result in the available time
     */
    public TsptwResult optimize() {
        solveSatisfy();
        solveOptimize();
        return bestSol;
    }

    /**
     * find a first feasible solution
     */
    private void solveSatisfy() {
        // TODO could be enhanced: cannot always find a feasible solution using this methodology
        if (solProvided) { // an initial solution is already given
            return;
        }

        cp = makeSolver();
        initCpVars();
        postSatisfactionConstraint();

        DFSearch search = makeDfs(cp, this::branchOnOneInsertionVar);
        search.onSolution(() -> {
            int nVisit = this.nVisitedNodes.max();
            if (nVisit > bestNVisited) {
                int current = begin;
                for (int i = 1 ; i < nVisit - 1; ++i) {
                    currentSolOrder[i] = route.nextMember(current);
                    current = currentSolOrder[i];
                }
                //currentSolOrder[nNodes] = nNodes;
                if (verbosity > 0) {
                    System.out.println("#visit: " + (nVisit - 1) + "/" + nNodes +
                            ". ordering: " + route.ordering(false, " "));
                }
                updateSatisfiabilitySolution(visitOrder, nVisit);
            }
        });

        SearchStatistics stats;

        search.solve(searchStatistics -> System.currentTimeMillis() - init >= Math.min(timeout, 2_000)); // find an initial number of possible nodes
        boolean foundFirstSol = bestNVisited == nNodesWithDepot;
        if (!foundFirstSol) {
            if (verbosity > 1)
                System.out.println("switching branching");
            search = makeDfs(cp, this::branchForSatisfiability);
            search.onSolution(() -> {
                int nVisit = this.nVisitedNodes.max();
                if (nVisit > bestNVisited) {
                    int current = begin;
                    for (int i = 1 ; i < nVisit - 1; ++i) {
                        currentSolOrder[i] = route.nextMember(current);
                        current = currentSolOrder[i];
                    }
                    if (verbosity > 0) {
                        System.out.println("#visit: " + (nVisit - 1) + "/" + nNodes +
                                ". ordering: " + route.ordering(false, " "));
                    }
                    updateSatisfiabilitySolution(visitOrder, nVisit);
                }
            });
            Objective objective = cp.maximize(nVisitedNodes);

            boolean running = System.currentTimeMillis() - init < timeout;
            int minNeighborhoodStart = 10;
            int range = 5; // VLNS parameters
            int numIters = 300;
            int failureLimitFinal = 1000;
            int maxRange = nNodes + range;

            for (int minNeighborhood = minNeighborhoodStart; minNeighborhood <= maxRange && running; ++minNeighborhood) {
                if (minNeighborhood == maxRange)
                    minNeighborhood = minNeighborhoodStart; // reset of the neighborhood
                for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                    int nRelax = minNeighborhood + offsetNeighborhood;
                    if (verbosity > 1)
                        System.out.println("relaxing " + nRelax + " nodes");
                    for (int i = 0; i < numIters && running; ++i) {
                        stats = search.optimizeSubjectTo(objective,
                                searchStatistics -> (
                                        System.currentTimeMillis() - init >= timeout
                                        || searchStatistics.numberOfNodes() > 10_000),
                                () -> relaxSuccessiveNodes(nRelax, bestNVisited));
                        foundFirstSol = bestNVisited == nNodesWithDepot;
                        running = !foundFirstSol && System.currentTimeMillis() - init < timeout;
                        if (running) {
                            stats = search.solveSubjectTo(
                                    searchStatistics -> System.currentTimeMillis() - init >= timeout
                                            || searchStatistics.numberOfNodes() > 10_000,
                                    () -> relaxRandomly(nRelax, bestNVisited));
                            //System.out.println(stats);
                        }
                    }
                }
            }
        }
        if (foundFirstSol) {
            if (verbosity > 0)
                System.out.println("found first solution");
        }
    }

    /**
     * find a solution minimizing the traveled distance
     * set the solution in bestSol through the use of updateSolution
     * a first solution must be provided!
     */
    private void solveOptimize() {
        cp = makeSolver();
        initCpVars();
        postOptimisationConstraint();

        DFSearch search = makeDfs(cp, this::branchOnOneInsertionVar);
        search.onSolution(() -> {
            if (this.routingCost.max() < bestSol.cost) {
                int current = begin;
                for (int i = 1 ; i < nNodesWithDepot; ++i) {
                    currentSolOrder[i] = route.nextMember(current);
                    current = currentSolOrder[i];
                }
                if (verbosity > 0) {
                    System.out.println("improving solution found " + route.ordering(false, " "));
                    System.out.println("found cost " + routingCost.max());
                }
                //System.out.print("solution found with successors ");
                //System.out.print(Arrays.toString(currentSolOrder));
                notifySolution(currentSolOrder, this.routingCost.max());
                updateOptimisationSolution(visitOrder, new TsptwResult(this.routingCost.max()));
            } else {
                //System.out.println("non improving solution found " + route.toString());
            }
        });

        SearchStatistics stats;

        int range = 5; // VLNS parameters
        int numIters = 300;
        int failureLimitFinal = 1000;
        int maxRange = nNodes - range;

        boolean running = System.currentTimeMillis() - init < timeout;

        Objective objective = cp.minimize(routingCost);
        for (int minNeighborhood = 2; minNeighborhood <= maxRange && running; ++minNeighborhood) {
            if (minNeighborhood == maxRange)
                minNeighborhood = 2; // reset of the neighborhood
            for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                for (int i = 0; i < numIters && running; ++i) {
                    int nRelax = minNeighborhood + offsetNeighborhood;
                    stats = search.optimizeSubjectTo(objective,
                            searchStatistics -> (
                                    System.currentTimeMillis() - init >= timeout
                                    //|| searchStatistics.numberOfFailures() > failureLimitFinal
                                    ),
                            () -> relaxSuccessiveNodes(nRelax));
                    running = System.currentTimeMillis() - init < timeout;
                }
            }
        }
    }

    /* ================================ relaxation operators =======================================================  */

    /**
     * relax nRelax nodes randomly from the current solution
     * @param nRelax number of nodes to relax
     * @param nVisit number of visited nodes in the solution (including end depot)
     */
    private void relaxRandomly(int nRelax, int nVisit) {
        Arrays.setAll(relaxedNodes, i-> i);
        int relaxEnd = 0;
        int toRelax;
        int cRelaxed;
        while (relaxEnd < nRelax && relaxEnd < nVisit) { // relax as many nodes as asked
            toRelax = relaxEnd + random.nextInt(nVisit - relaxEnd);
            cRelaxed = relaxedNodes[toRelax];
            relaxedNodes[toRelax] = relaxedNodes[relaxEnd];
            relaxedNodes[relaxEnd] = cRelaxed;
            ++relaxEnd;
        }
        // relaxedNodes[0..relaxEnd-1] contains the relaxed nodes
        relaxed.clear();
        for (int i = 0 ; i < relaxEnd; ++i)
            relaxed.add(relaxedNodes[i]);
        // relaxedNodes[relaxEnd..] are set to the previous value
        int prev = begin;
        //for (int current: bestSolOrder) {
        for (int i = 1 ; i < nVisit - 1 ; ++i) {
            int current = currentSolOrder[i];
            if (!relaxed.contains(current)) {
                try {
                cp.post(new Schedule(route, current, prev), false); // the vehicle goes through this node
                } catch (InconsistencyException e) {
                    int a = 0 ;
                    throw e;
                }
                prev = current; // only updated when a non-relaxed node is met, to complete the partial route
            }
        }
        //System.out.println(route.ordering(false, " "));
    }

    /**
     * relax nRelax nodes randomly from the current solution
     * @param nRelax number of nodes to relax
     */
    private void relaxRandomly(int nRelax) {
        Arrays.setAll(relaxedNodes, i-> i);
        int relaxEnd = 0;
        int toRelax;
        int cRelaxed;
        while (relaxEnd < nRelax && relaxEnd < nNodes) { // relax as many nodes as asked
            toRelax = relaxEnd + random.nextInt(nNodes - relaxEnd);
            cRelaxed = relaxedNodes[toRelax];
            relaxedNodes[toRelax] = relaxedNodes[relaxEnd];
            relaxedNodes[relaxEnd] = cRelaxed;
            ++relaxEnd;
        }
        // relaxedNodes[0..relaxEnd-1] contains the relaxed nodes
        relaxed.clear();
        for (int i = 0 ; i < relaxEnd; ++i)
            relaxed.add(relaxedNodes[i]);
        // relaxedNodes[relaxEnd..] are set to the previous value
        int prev = begin;
        for (int current: bestSolOrder) {
            if (!relaxed.contains(current)) {
                cp.post(new Schedule(route, current, prev), false); // the vehicle goes through this node
                prev = current; // only updated when a non-relaxed node is met, to complete the partial route
            }
        }
        // the objective that must be respected
        // cost.removeBelow(bestSol.cost);
    }


    /**
     * relax consecutive nodes in the sequence while
     * @param nRelax number of nodes to relax
     * @param nVisited number of visited nodes in the current solution
     */
    private void relaxSuccessiveNodes(int nRelax, int nVisited) {
        // look in the current solution array and select randomly the first node to relax
        if (nRelax >= nVisited - 1) // all nodes must be relaxed
            return;
        int firstNodeIdx = 1 + random.nextInt(nVisited - 1 - nRelax);

        boolean verbose = verbosity > 3;
        if (verbose) {
            System.out.println("given ");
            for (int i = 0 ; i < nVisited ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.println("\nI plan to relax to ");
            for (int i = 0 ; i < firstNodeIdx ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.print("----- ");
            for (int i = firstNodeIdx + nRelax ; i < nVisited ; ++i) {
                System.out.print(currentSolOrder[i] + " ");
            }
            System.out.println("\n");
        }

        int pred = begin;
        int current;
        for (int i = 1 ; i < nVisited - 1 ; ++i) {
            current = currentSolOrder[i];
            if (i == firstNodeIdx) { // in the relaxed nodes set
                for (; i < firstNodeIdx + nRelax ; ++i) { // for all relaxed nodes
                    current = currentSolOrder[i];
                    for (int j = 0; j < firstNodeIdx - 1; ++j) { // for all nodes before the set of relaxed node except the one before
                        int invalidPred = currentSolOrder[j];
                        route.removeInsertion(current, invalidPred); // remove the insertion
                    }
                    for (int j = firstNodeIdx + nRelax; j < nVisited; ++j) { // for all nodes after the set of relaxed node
                        int invalidPred = currentSolOrder[j];
                        route.removeInsertion(current, invalidPred); // remove the insertion
                    }
                    if (route.nInsertions(current) != nRelax) {
                        int a = 0;
                    }
                }
                // end of loop, i = firstNodeIdx + nRelax
                i = firstNodeIdx + nRelax;
                current = currentSolOrder[i];
            }
            try {
                cp.post(new Schedule(route, current, pred), false);
            } catch (Exception e) {
                int a = 0;
                throw e;
            }
            pred = current;
        }
        // close the sequence
        //System.out.println(route.ordering(false, " "));
    }

    /**
     * relax a number of consecutive nodes in the sequence
     *
     * the sequence will be split in three parts:
     *  1. before the set of relaxed nodes (A)
     *  2. set of relaxed nodes (B)
     *  3. after the set of relaxed nodes (C)
     * the relaxed nodes in (B) can only be inserted between (A) and (C)
     *
     * @param nRelax number of nodes to relax
     */
    private void relaxSuccessiveNodes(int nRelax) {
        relaxSuccessiveNodes(nRelax, nNodesWithDepot);
    }

    /* ================================ branching ================================================================  */

    /**
     * branching procedure to solve the TSPTW instance
     *
     * variable selection: select the node with the least successors
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] branchOnOnePredecessor() {
        return null; // TODO
    }

    /**
     * branching procedure to find a first feasible solution
     *
     * each ordering is a solution
     *
     * variable selection: select the node with the least scheduled insertions
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] branchForSatisfiability() {
        if (route.isBound()) // all nodes have been sequenced
            return EMPTY;

        // select the node with the least insertions points
        int size = route.fillPossible(nodes);
        int minInsert = Integer.MAX_VALUE;
        int nFound = 0;
        for (int i = 0 ; i < size; ++i) {
            int nInsert = route.nScheduledInsertions(nodes[i]);
            if (nInsert < minInsert && nInsert > 0) {
                minInsert = nInsert;
                insertion[0] = nodes[i];
                nFound = 1;
            } else if (nInsert == minInsert) {
                insertion[nFound++] = nodes[i];
            }
        }
        if (nFound == 0) {
            return EMPTY;
        }

        int branchingNode = insertion[random.nextInt(nFound)]; // randomly select the node amongst the nodes that have been selected
        // branch on every scheduled insertion
        route.fillScheduledInsertions(branchingNode, nodes);
        for (int i = 0 ; i < minInsert; ++i) {
            int pred = nodes[i];
            branchingRange[i] = i;
            heuristicVal[i] =  satisfiabilityHeuristic(branchingNode, pred);
            branching[i] = () -> cp.post(new Schedule(route, branchingNode, pred));
        }
        // sort according to the heuristic
        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        Procedure[] branchingSorted = new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];
        // exclude all possible nodes, binding the sequence and giving a solution
        //branchingSorted[minInsert] = () -> {cp.post(new ExcludeAllPossible(route));};
        return branchingSorted;
    }

    /**
     * branching procedure to solve the TSPTW instance
     *
     * variable selection: select the node with the least scheduled insertions
     * value selection: branch on every scheduled insertion in increasing heuristic value
     *
     * @return branching to solve the TSPTW instance
     */
    public Procedure[] branchOnOneInsertionVar() {
        if (route.isBound()) // all nodes have been sequenced
            return EMPTY;

        // select the node with the least insertions points
        int size = route.fillPossible(nodes);
        int minInsert = Integer.MAX_VALUE;
        int nFound = 0;
        for (int i = 0 ; i < size; ++i) {
            int nInsert = route.nScheduledInsertions(nodes[i]);
            if (nInsert < minInsert && nInsert > 0) {
                minInsert = nInsert;
                insertion[0] = nodes[i];
                nFound = 1;
            } else if (nInsert == minInsert) {
                insertion[nFound++] = nodes[i];
            }
        }
        if (nFound == 0)
            throw INCONSISTENCY;

        int branchingNode = insertion[random.nextInt(nFound)]; // randomly select the node amongst the nodes that have been selected

        // branch on every scheduled insertion
        route.fillScheduledInsertions(branchingNode, nodes);
        for (int i = 0 ; i < minInsert; ++i) {
            int pred = nodes[i];
            branchingRange[i] = i;
            heuristicVal[i] =  heuristic(branchingNode, pred);
            branching[i] = () -> cp.post(new Schedule(route, branchingNode, pred));
        }
        // sort according to the heuristic
        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        Procedure[] branchingSorted = new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];
        return branchingSorted;
    }

    /* ================================ heuristic ================================================================  */

    /**
     * heuristic value when inserting a node at a given position
     * @param node node that will be inserted
     * @param pred predecessor after which the node will be inserted
     * @return heuristic value for the insertion: the lower the better
     */
    public int heuristic(int node, int pred) {
        int succ = route.nextMember(pred);
        int slack = time[succ].max() - (time[pred].min() + distances[pred][node] + distances[node][succ]);
        int objChange = distances[node][succ] + distances[pred][node] - distances[pred][succ];
        return objChange - slack;
    }

    /**
     * minus slack when inserting a node at a given position
     * @param node node that will be inserted
     * @param pred predecessor after which the node will be inserted
     * @return heuristic value for the insertion: the lower the better
     */
    public int satisfiabilityHeuristic(int node, int pred) {
        int succ = route.nextMember(pred);
        return - (time[succ].max() - (time[pred].min() + distances[pred][node] + distances[node][succ]));
    }

    /* ================================ model ======================================================================  */

    private void initCpVars() {
        // sequence
        route = new SequenceVarImpl(cp, nNodes, begin, end);

        // time window
        time = new IntVar[nNodesWithDepot];
        for (int i = 0 ; i < nNodes ; ++i) {
            time[i] = makeIntVar(cp, twStart[i], twEnd[i], true);
        }
        time[end] = makeIntVar(cp, twStart[begin], twEnd[begin], true);

        // routing cost
        int lowerBoundTot = 0;
        int upperBoundTot = twEnd[end];
        routingCost = makeIntVar(cp, lowerBoundTot, upperBoundTot, true);

        // visited nodes cost
        nVisitedNodes = makeIntVar(cp, 2, nNodesWithDepot);
    }

    /**
     * post constraints to visit as much node as possible
     * nodes can be excluded from the problem
     */
    private void postSatisfactionConstraint() {
        int[] servingDuration = new int[nNodesWithDepot];
        // respect the transitions between nodes
        cp.post(new TransitionTimes(route, time, distances, servingDuration));
        // cost is the number of visited nodes
        cp.post(new NScheduled(route, nVisitedNodes));
    }

    /**
     * post constraints to visit all nodes in the problem
     */
    private void postOptimisationConstraint() {
        cp.post(new TSPTW(route, time, routingCost, distances));
    }

    /* ================================ solution update ============================================================  */


    /**
     * update the best solution based on a newly found solution
     * @param solFound successor array of the newly found solution
     * @param result result of the newly found solution
     */
    private void updateOptimisationSolution(int[] solFound, TsptwResult result) {
        if (result.cost < bestSol.cost) {
            bestSolOrder = solFound;
            bestSol = result;
        }
    }

    /**
     * update the most promising satisfiable solution found with nNodes in the sequences
     * @param solFound best solution found. Include the beginning and ending nodes
     * @param nNodes number of nodes (beginning and ending nodes included) in the solFound
     */
    private void updateSatisfiabilitySolution(int[] solFound, int nNodes) {
        if (nNodes > bestNVisited) {
            bestSolOrder = solFound;
            bestNVisited = nNodes;
        }
    }

    /* ========================= circuit approach to compare found solutions ======================================== */

    private static IntVar elementVar(IntVar[] array, IntVar y) {
        Solver cp = y.getSolver();
        int min = Arrays.stream(array).mapToInt(IntVar::min).min().getAsInt();
        int max = Arrays.stream(array).mapToInt(IntVar::max).max().getAsInt();
        IntVar z = makeIntVar(cp, min,max);
        cp.post(new Element1DVar(array, y, z));
        return z;
    }

    /**
     * test if the solutions found with the circuit approach are contained within the sequence approach
     */
    private void compareWithCircuitSol() {
        // solve with the circuit approach
        solRegistered = new ArrayList<>();
        if (verbosity > 0)
            System.out.println("solving with circuit");
        solveWithCircuitConstraint();
        if (verbosity > 0)
            System.out.println("end of circuit");

        // solve with the sequence approach

        if (verbosity > 0)
            System.out.println("\nposting solution found on sequence model");
        cp = makeSolver();
        initCpVars();
        postOptimisationConstraint();
        DFSearch search = makeDfs(cp, this::branchOnOneInsertionVar);
        search.onSolution(() -> {
            int current = begin;
            for (int i = 1 ; i < nNodes; ++i) {
                visitOrder[i] = route.nextMember(current);
                current = visitOrder[i];
            }
            //System.arraycopy(solutionInternal, 1, visitOrder, 1, nNodes - 1);

            if (this.routingCost.max() < bestSol.cost)
                notifySolution(visitOrder, this.routingCost.max());

            if (verbosity > 0) {
                System.out.print("solution found with successors ");
                System.out.print(Arrays.toString(visitOrder));
            }
            int cost = 0;
            current = begin;
            while (current != end) {
                int next = route.nextMember(current);
                cost += distances[current][next];
                current = next;
            }
            if (verbosity > 0) {
                System.out.println(" and cost " + cost + "  (from constraint: [" + this.routingCost.min() + " , " + this.routingCost.max() + "])");
            }
            updateOptimisationSolution(visitOrder, new TsptwResult(this.routingCost.max()));
        });
        //SearchStatistics stats = search.solve();
        //System.out.println(stats);
        // test the first solution encountered using the circuit approach
        for (int[] ordering: solRegistered) {
            search.solveSubjectTo(searchStatistics -> {return false;}, () -> {
                int pred = begin;
                for (int i = 1; i < nNodes ; ++i) {
                    int current = ordering[i];
                    cp.post(new Schedule(route, current, pred), false);
                    pred = current;
                }
                try {
                    cp.fixPoint();
                } catch (InconsistencyException e) {
                    if (verbosity > 0)
                        System.out.println("failed for " + Arrays.toString(ordering));
                    throw e;
                }
            });
        }
        if (verbosity > 0) {
            System.out.println("end of posting\n");

            System.out.println("trying to find solutions from scratch with sequence");
        }
        SearchStatistics stats = search.solve();
        if (verbosity > 0) {
            System.out.println(stats);
            System.out.println("end of try\n");
        }
    }

    /**
     * solve the instance using a circuit approach
     * set the found solutions in the solRegistered list
     */
    private void solveWithCircuitConstraint() {
        cp = makeSolver();

        IntVar[] succ = makeIntVarArray(cp, nNodesWithDepot, nNodesWithDepot);

        time = new IntVar[nNodesWithDepot];
        int maxTwEnd = Integer.MIN_VALUE;
        int minTwStart = Integer.MAX_VALUE;
        for (int i = 0 ; i < nNodes ; ++i) {
            int twStart = instance.timeWindows[i].getEarliest();
            int twEnd = instance.timeWindows[i].getLatest();
            time[i] = makeIntVar(cp, twStart, twEnd, true);
        }
        time[begin] = makeIntVar(cp, instance.timeWindows[begin].getEarliest(), instance.timeWindows[begin].getLatest(), true);
        time[end] = makeIntVar(cp, instance.timeWindows[begin].getEarliest(), instance.timeWindows[begin].getLatest(), true);

        try {
            cp.post(equal(succ[end], begin));
            cp.post(new Circuit(succ));

            // time transition
            for (int i = 0; i < nNodes; ++i) {
                // time[i] + dist[i][succ[i]] <= time[succ[i]]
                IntVar timeSucc = elementVar(time, succ[i]);
                IntVar distance = element(distances[i], succ[i]);
                //System.out.println("posting time for " + i);
                cp.post(new LessOrEqual(sum(time[i], distance), timeSucc));
            }

        } catch (InconsistencyException e) {
            if (verbosity > 0)
                System.err.println("inconsistency when initializing the problem");
            return;
        }

        DFSearch search = makeDfs(cp, firstFail(succ));

        int[] ordering = new int[nNodes];
        search.onSolution(() -> {
            int current = begin;
            for (int i = 1 ; i < nNodes; ++i) {
                ordering[i] = succ[current].min();
                current = succ[current].min();
            }
            if (verbosity > 0) {
                System.out.print("solution found with ordering ");
                System.out.print(Arrays.toString(ordering));
            }
            int cost = 0;
            current = begin;
            while (current != end) {
                int next = succ[current].min();
                cost += distances[current][next];
                current = next;
            }
            if (verbosity > 0) {
                System.out.println(" and cost " + cost);
            }
            solRegistered.add(ordering.clone());
        });
        SearchStatistics stats = null;
        if (initial == null) {
            stats = search.solve();
        } else { // post the initial solution provided
            stats = search.solveSubjectTo(searchStatistics -> false,
                    () -> {
                        int pred = begin;
                        for (int current : initial) {
                            if (current == begin)
                                continue;
                            cp.post(equal(succ[pred], current), false);
                            pred = current;
                        }
                        try {
                            cp.fixPoint();
                        } catch (InconsistencyException e) {
                            if (verbosity > 0) {
                                System.err.println("could not initialize problem from initial solution");
                            }
                            throw e;
                        }
                    }
            );
        }
        System.out.println(stats);
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }


}

class TsptwResult {
    boolean isOptimum = false;
    int cost = Integer.MAX_VALUE;

    public TsptwResult(int cost) {
        this.cost = cost;
    }
}

