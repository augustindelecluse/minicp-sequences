package minicp.examples.darp;

import minicp.engine.constraints.sequence.*;
import minicp.engine.core.*;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.state.StateSparseSet;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;

/**
 * versioning:
 * 0: use nScheduled(cv) * nScheduled(ncv)
 * 1: use nScheduled(cv)
 * 2: use blindRequestTreeSearch
 */
public class DARPInsertion extends DARPSolver{

    @Override
    public DARPInstance.DARPSolution solve(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        initCp(instance);
        darpSolveStatistics.init();
        SearchStatistics stats = cp.solve(darpSolveStatistics, null, true, false);
        darpSolveStatistics.finish();
        darpSolveStatistics.setSearchStatistics(stats);
        return cp.bestSolution;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        initCp(instance);
        darpSolveStatistics.solveWithProc(() -> cp.solve(darpSolveStatistics, null, false, false));
        return cp.bestSolution;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPInstance.DARPSolution initSolution, DARPSolveStatistics darpSolveStatistics) {
        initCp(instance);
        darpSolveStatistics.solveWithProc(() -> cp.solve(darpSolveStatistics, initSolution, false, false));
        return cp.bestSolution;
    }

    @Override
    public void solveAll(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        initCp(instance);
        darpSolveStatistics.init();
        SearchStatistics stats = cp.solve(darpSolveStatistics, null, false, true);
        darpSolveStatistics.finish();
        darpSolveStatistics.setSearchStatistics(stats);
    }

    @Override
    public String description() {return "insertion";}

    @Override
    public int getMaxVersion() {
        return 2;
    }

    @Override
    public int defaultVersion() {
        return 0;
    }

    public DARPInsertion(DARPInstance instance) {
        initCp(instance);
    }

    public void initCp(DARPInstance instance) {
        cp = new DarpInsertionCP(instance);
    }

    DarpInsertionCP cp;
    private static final int PRECEDE = 1;
    private static final int SAME_NODE = 2;
    private static final int NOT_PRECEDE = 0;

    public class DarpInsertionCP {

        public record Insertion(int vehicle, int node, int pred, int cost) implements Comparable<Insertion> {

            @Override
            public int compareTo(Insertion insertion) {
                return this.cost - insertion.cost;
            }
        }

        // decision variables
        IntVar [] servingTime;
        IntVar [] routeLength;
        IntVar sumRouteLength;
        SequenceVar[] routes;
        
        public double probUpdateSol = 0.07;

        //final int SCALING = 100;
        final int ALPHA = 80;
        final int BETA = 1;

        int [] vertexLoadChange, serviceTime, twStart, twEnd;

        int [][] travelTimeMatrix;
        int numVars;

        Solver cp = makeSolver();
        DARPInstance.DARPSolution bestSolution;
        int bestSolutionObjective = Integer.MAX_VALUE;
        DARPInstance.DARPSolution currentSolution; // used for diversification in the LNS-FFPA
        int currentSolutionObjective = Integer.MAX_VALUE;
        int upperBoundObjective = Integer.MAX_VALUE;
        ArrayList<DARPInstance.Stop> stops; // used to construct and verify solutions through DARPInstance
        ArrayList<Insertion> insertionsValue = new ArrayList<>();    // used to register and sort insertions in the heuristic

        // used for the heuristic
        Integer[] branchingRange;
        Integer[] heuristicVal;
        Procedure[] branching;

        int rangeRequestMin, rangeRequestMax;
        int rangePickupMin, rangePickupMax;
        int rangeDeliveryMin, rangeDeliveryMax;
        int rangeDepotMin, rangeDepotMax;
        int rangeStartDepotMin, rangeStartDepotMax;
        int rangeEndDepotMin, rangeEndDepotMax;

        StateSparseSet customersLeft; // remaining requests that must be processed
        int [] requests; // used by customersLeft.fillArray in the branching
        int [] insertions; // used by fillArray in the branching
        int [] insertions2; // used by fillArray in the branching
        StateInt lastInsert; // last inserted critical vertex. if >= 0, gives the request id of the critical vertex
        // otherwise, the last insertion was a non critical vertex
        StateInt lastInsertVehicle; // vehicle were the last critical vertex insertion has occurred

        DARPInstance darp;

        public DarpInsertionCP(DARPInstance darpInstance) {
            this.darp = darpInstance;
            numVars = darpInstance.nRequests * 2 + 2 * darpInstance.nVehicles;

            rangeRequestMin = 0;
            rangeRequestMax = darp.nRequests;

            rangePickupMin = 0;
            rangePickupMax = darp.nRequests;
            rangeDeliveryMin = rangePickupMax;
            rangeDeliveryMax = 2 * darp.nRequests;
            rangeDepotMin = rangeDeliveryMax;
            rangeDepotMax = 2 * darp.nRequests + darp.nVehicles * 2;

            rangeStartDepotMin = rangeDeliveryMax;
            rangeStartDepotMax = rangeDeliveryMax + darp.nVehicles;
            rangeEndDepotMin = rangeStartDepotMax;
            rangeEndDepotMax = rangeEndDepotMin + darp.nVehicles;

            // travelTimeMatrix and vertexLoadChange

            travelTimeMatrix = new int[numVars][numVars];
            vertexLoadChange = new int[numVars];
            serviceTime = new int[numVars];
            requests = new int[darp.nRequests*2];
            insertions = new int[darp.nRequests*2];
            insertions2 = new int[darp.nRequests*2];
            twEnd = new int[numVars];
            twStart = new int[numVars];


            heuristicVal = new Integer[numVars*numVars]; // upperbound on the number of values stored in the heuristic array
            branchingRange = new Integer[numVars*numVars];
            branching = new Procedure[numVars*numVars];

            DARPInstance.Coordinate[] coords = new DARPInstance.Coordinate[numVars];
            for (int i = 0; i < 2*darp.nRequests; i++) {
                coords[i] = darp.stops[i].coord();
                vertexLoadChange[i] = darp.stops[i].loadChange();
                serviceTime[i] = darp.stops[i].servingDuration();
                twStart[i] = darp.stops[i].twStart();
                twEnd[i] = darp.stops[i].twEnd();
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                coords[i] = darp.startDepot.coord();
                vertexLoadChange[i] = 0;
                serviceTime[i] = darp.startDepot.servingDuration();
                twStart[i] = darp.startDepot.twStart();
                twEnd[i] = darp.startDepot.twEnd();
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                coords[i] = darp.endDepot.coord();
                vertexLoadChange[i] = 0;
                serviceTime[i] = darp.endDepot.servingDuration();
                twStart[i] = darp.endDepot.twStart();
                twEnd[i] = darp.endDepot.twEnd();
            }

            // travel time
            // TODO investigate rounding of matrix dist: and floor can preserve inequality?
            for (int i = 0; i < numVars; i++) {
                for (int j = i+1; j < numVars; j++) {
                    int distance = DARPInstance.distance(coords[i], coords[j]);
                    travelTimeMatrix[i][j] = distance;
                    travelTimeMatrix[j][i] = distance;
                }
            }

            // arraylist of stops used in the solutions. Indexing corresponds to the indexing used in this representation
            this.stops = new ArrayList<>(); // no need to specify end nodes, only pickup and drops
            stops.addAll(Arrays.asList(darp.stops).subList(0, 2 * darp.nRequests));
            initCpVars();
            postConstraints();
            currentSolution = constructSolution();
        }

        private void initCpVars() {
            // create cp variables
            customersLeft = new StateSparseSet(cp.getStateManager(), darp.nRequests, 0);
            servingTime = new IntVar[numVars];
            lastInsert = cp.getStateManager().makeStateInt(-1); // no node inserted at the moment
            lastInsertVehicle = cp.getStateManager().makeStateInt(-1); // no node inserted at the moment
            for (int i = 0; i < 2*darp.nRequests; ++i) {
                servingTime[i] = makeIntVar(cp,darp.stops[i].twStart(), darp.stops[i].twEnd(), true);
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; ++i) {
                servingTime[i] = makeIntVar(cp,darp.startDepot.twStart(), darp.startDepot.twEnd(), true);
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; ++i) {
                servingTime[i] = makeIntVar(cp,darp.endDepot.twStart(), darp.endDepot.twEnd(), true);
            }

            routeLength = new IntVar[darp.nVehicles];
            for (int i = 0; i < darp.nVehicles; ++i) {
                routeLength[i] = makeIntVar(cp, 0, darp.timeHorizon, true);
            }
            sumRouteLength = makeIntVar(cp, 0, darp.timeHorizon * darp.nVehicles, true);

            routes = new SequenceVar[darp.nVehicles];
            for (int i = 0; i < darp.nVehicles; ++i) {
                routes[i] = new SequenceVarImpl(cp, darp.nRequests * 2, rangeDepotMin + i, rangeEndDepotMin + i);
            }
        }

        private void postConstraints() {
            // post the constraints
            // serving time pickup + serving + travel time to delivery <= serving time delivery
            for (int i = rangePickupMin; i < rangePickupMax; ++i) {
                cp.post(lessOrEqual(plus(servingTime[i], serviceTime[i] + travelTimeMatrix[i][i+darp.nRequests]),
                        servingTime[i+darp.nRequests]), false);
            }
            // max ride time constraint
            for (int i = 0; i < darp.nRequests; i++) {
                int servingDuration = darp.stops[i].servingDuration();
                final int idx = i;
                IntVar departurePickup = plus(servingTime[i], servingDuration);
                cp.post(new AbstractConstraint(cp) {
                    @Override
                    public void post() {
                        propagate();
                        if (isActive()) {
                            servingTime[idx + darp.nRequests].propagateOnBoundChange(this);
                            departurePickup.propagateOnBoundChange(this);
                        }
                    }
                    @Override
                    public void propagate() {
                        servingTime[idx+darp.nRequests].removeAbove( departurePickup.max()+darp.maxRideTime);
                        departurePickup.removeBelow(servingTime[idx+darp.nRequests].min()- darp.maxRideTime);
                        if (departurePickup.min() + darp.maxRideTime > servingTime[idx+darp.nRequests].max())
                            setActive(false); // maxRideTime is always respected
                    }
                }, false);

            }

            int[] starts = IntStream.range(rangePickupMin, rangePickupMax).toArray();
            int[] ends = IntStream.range(rangeDeliveryMin, rangeDeliveryMax).toArray();
            for (int i = 0; i < darp.nVehicles; ++i) {
                // maximum capacity per vehicle
                cp.post(new Cumulative(routes[i], starts, ends, darp.vehicleCapacity, vertexLoadChange), false);
                // transition time between nodes
                //cp.post(new TransitionTimes(routes[i], servingTime, routeLength[i], travelTimeMatrix, serviceTime, false), false);
                cp.post(new TransitionTimes(routes[i], servingTime, travelTimeMatrix, serviceTime), false);
            }
            // objective value is the sum of all routes
            // cp.post(new Sum(routeDuration, sumRouteDuration), false);
            // the nodes can be visited once
            cp.post(new Disjoint(routes), false);
            cp.fixPoint();
        }

        /**
         * relax the current solution, by removing nRelax customers from it
         * @param nRelax number of customers to remove from the current solution
         */
        public void relax(int nRelax) {
            int[] possibleCustomers = IntStream.range(0, darp.nRequests).toArray();
            int relaxEnd = 0;
            int toRelax;
            int cRelaxed;
            while (relaxEnd < nRelax && relaxEnd < darp.nRequests) { // relax as many requests as asked
                toRelax = relaxEnd + nextInt(darp.nRequests - relaxEnd);
                cRelaxed = possibleCustomers[toRelax];
                possibleCustomers[toRelax] = possibleCustomers[relaxEnd];
                possibleCustomers[relaxEnd] = cRelaxed;
                ++relaxEnd;
            }
            // possibleCustomers[0..relaxEnd-1] contains the relaxed customers
            for (int i = relaxEnd; i < darp.nRequests ; ++i) { // possibleCustomers[relaxEnd..] are set to their previous value
                customersLeft.remove(possibleCustomers[i]);
            }
            for (int v = 0; v < darp.nVehicles; ++v) {
                int prev = routes[v].begin();
                for (int current: currentSolution.succ[v]) {
                    if (!customersLeft.contains(getCorrespondingRequest(current))) {
                        cp.post(new Schedule(routes[v], current, prev), false); // the vehicle goes through this node
                        prev = current; // only updated when a non-relaxed node is met, to complete the partial route
                    }
                }
            }
        }

        public void postSequence(String sequence) {
            int i = 0;
            for (String route: sequence.split("\n")) {
                int pred = -1;
                for (String node: route.split(" -> ")) {
                    int current = Integer.parseInt(node.strip());
                    if (pred == -1) {
                        pred = current;
                    } else {
                        cp.post(new Schedule(routes[i], current, pred));
                        pred = current;
                    }
                }
                ++i;
            }
        }

        private DFSearch getInitialSearch() {
            if (version == 2)
                return makeDfs(cp, this::blindRequestTreeSearch);
            return makeDfs(this.cp, this::boundImpactRequestTreeSearch);
            /*
            if (version == 0 || version == 1) {
                return makeDfs(this.cp, this::treeSearch);
            } else if (version == 2 || version == 3) {
                return makeDfs(this.cp, this::boundImpactRequestTreeSearch);
            } else if (version == 4) {
                return makeDfs(this.cp, this::boundImpactRequestTreeSearch);
            }
            return null;

             */
        }

        private DFSearch getLNSSearch() {
            if (version == 2)
                return makeDfs(cp, this::blindRequestTreeSearch);
            return makeDfs(this.cp, this::boundImpactRequestTreeSearch);
            /*
            if (version == 0 || version == 2) {
                return makeDfs(this.cp, this::treeSearch);
            } else if (version == 1 || version == 3) {
                return makeDfs(this.cp, this::boundImpactRequestTreeSearch);
            } else if (version == 4) {
                return makeDfs(this.cp, this::sequenceFirstFail);
            }
            return null;

             */
        }

        public SearchStatistics solve(DARPSolveStatistics solveStatistics, DARPInstance.DARPSolution initSolution, boolean firstSolOnly, boolean allSolution) {
            assert (!firstSolOnly || initSolution == null); // cannot ask for a first sol if an init sol is provided
            SearchStatistics stats = null;
            DFSearch search = getInitialSearch();

            search.onSolution( () -> {
                DARPInstance.DARPSolution solution = constructSolution();
                solution.computeNoExcept();
                updateSolutionFfpa(solution, solveStatistics);
                //updateSolutionFfpa(solveStatistics);
            });
            if (allSolution) {
                stats = search.solve(searchStatistics -> solveStatistics.isFinished());
            } else { // find a first solution
                boolean feasibleSolution;
                int failureLimit = 500;
                if (initSolution == null) {  // find first a feasible solution
                    boolean solFound = false;
                    while (!solFound && !solveStatistics.isFinished()) {
                        stats = search.solve(searchStatistics -> (
                                searchStatistics.numberOfSolutions() > 0 ||
                                solveStatistics.isFinished() ||
                                searchStatistics.numberOfFailures() > failureLimit));
                        solFound = stats.numberOfSolutions() > 0;
                    }
                    feasibleSolution = solFound;
                } else {
                    if (getVerbosity() > 0)
                        System.out.println("initial solution provided");
                    initSolution.computeNoExcept();
                    updateSolutionFfpa(initSolution, solveStatistics);
                    feasibleSolution = true;
                }
                if (!firstSolOnly && feasibleSolution) { // continue using LNS
                    search = getLNSSearch();
                    search.onSolution( () -> {
                        DARPInstance.DARPSolution solution = constructSolution();
                        solution.computeNoExcept();
                        updateSolutionFfpa(solution, solveStatistics);
                        //updateSolutionFfpa(solveStatistics);
                    });
                    int range = 5;
                    int numIters = 300;
                    int failureLimitFinal = 1000;
                    int maxRange = darp.nRequests / 2 - range;
                    boolean running = !solveStatistics.isFinished();
                    for (int minNeighborhood = 2; minNeighborhood <= maxRange && running; ++minNeighborhood) {
                        if (minNeighborhood == maxRange)
                            minNeighborhood = 2; // reset of the neighborhood
                        for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                            for (int i = 0; i < numIters && running; ++i) {
                                int nRelax = minNeighborhood + offsetNeighborhood;
                                stats = search.solveSubjectTo(searchStatistics -> (
                                            searchStatistics.numberOfSolutions() == 1 ||
                                            solveStatistics.isFinished() ||
                                            searchStatistics.numberOfFailures() > failureLimitFinal),
                                        () -> relax(nRelax));
                                running = !solveStatistics.isFinished();
                            }
                        }
                    }
                } else if (!feasibleSolution) {
                    if (getVerbosity() > 0)
                        System.out.println("no solution found");
                }
            }
            return stats;
        }

        /**
         * construct a solution based on the current values for the successors array
         * @return
         */
        public DARPInstance.DARPSolution constructSolution() {
            DARPInstance.DARPSolution solution = darp.constructSolution(stops);
            return addSolutionRoute(solution);
        }

        /**
         * modify in place the current solution found
         */
        public void updateCurrentSolution() {
            currentSolution.reset();
            addSolutionRoute(currentSolution);
        }

        private DARPInstance.DARPSolution addSolutionRoute(DARPInstance.DARPSolution initSolution) {
            for (int vehicle = 0; vehicle < darp.nVehicles; ++vehicle) {
                SequenceVar route = routes[vehicle];
                int current = route.nextMember(route.begin());
                int end = route.end();
                while (current != end) {
                    initSolution.addStop(vehicle, current);
                    current = route.nextMember(current);
                }
            }
            return initSolution;
        }

        private void printTimeVars() {
            for (int i = 0 ; i < numVars; ++i) {
                System.out.printf("node %d : [%d, %d]\n", i, servingTime[i].min(), servingTime[i].max());
            }
        }

        private void printCurrentRoutes() {
            int nodesVisited = 0;
            for (SequenceVar route: routes) {
                ++nodesVisited;
                int current = route.nextMember(route.begin());
                int end = route.end();
                int begin = route.begin();
                StringBuilder road = new StringBuilder(String.format("%2d", begin));
                while (current != begin) {
                    ++nodesVisited;
                    road.append(" -> ").append(String.format("%2d", current));
                    current = route.nextMember(current);
                }
                System.out.println(road);
            }
            System.out.println(nodesVisited + " nodes visited / " + this.numVars);
            System.out.println("----------");
        }

        /**
         * tell if a node lies before another one
         * @param order ordering of the node
         * @param first node that is supposed to be first
         * @param last node that is supposed to be last
         * @return PRECEDE if first <= last in the sequence,
         *  SAME_NODE if first == last and
         *  NOT_PRECEDE if first > last
         */
        private int precede(int[] order, int first, int last, int nNodes) {
            if (first == last)
                return SAME_NODE;
            for (int i = 0 ; i < nNodes ; ++i) {
                if (order[i] == first)
                    return PRECEDE;
                else if (order[i] == last)
                    return NOT_PRECEDE;
            }
            return NOT_PRECEDE;
        }

        /**
         * tree search inserting request per request instead of node per node
         * insert the critical vertex and the non-critical-vertex into the vehicle if the pickup is before the delivery
         * @return branching consisting of inserting a critical vertex and a non-critical vertex into the sequence
         *  the values are ordered by heuristic
         */
        private Procedure[] blindRequestTreeSearch() {
            int size = customersLeft.fillArray(requests);
            if (size == 0) {
                return EMPTY;
            }
            int minInsert = Integer.MAX_VALUE; // min number of insertions found
            int nInsert;
            int cnt = 0; // number of requests with a min number of insertions
            for (int i = 0; i < size; ++i) { // find the request with the min number of insertions
                nInsert = 0;
                int cv = getCriticalVertex(requests[i]);
                int ncv = getNonCriticalVertex(requests[i]);
                for (SequenceVar route : routes) {
                    nInsert += route.nScheduledInsertions(cv) * route.nScheduledInsertions(ncv);
                }
                if (nInsert < minInsert) {
                    minInsert = nInsert;
                    insertions[0] = requests[i];
                    cnt = 1;
                } else if (nInsert == minInsert) {
                    insertions[cnt++] = requests[i];
                }
            }
            if (minInsert == 0)
                throw INCONSISTENCY;
            int id = insertions[nextInt(cnt)];

            // create the branching for the pair of cvv - ncv
            int vehicle = 0;
            int cv = getCriticalVertex(id);
            int ncv = getNonCriticalVertex(id);
            boolean cvPickup = cv < ncv;
            int i = 0;
            for (SequenceVar route : routes) {
                int sizeCv = route.fillScheduledInsertions(cv, insertions);
                int sizeNcv = route.fillScheduledInsertions(ncv, insertions2);
                int orderSize = route.fillOrder(requests, true);

                for (int j = 0 ; j < sizeCv ; ++j) {
                    int insertCv = insertions[j];
                    for (int k = 0 ; k < sizeNcv ; ++k) {
                        int insertNcv = insertions2[k];
                        int prec = cvPickup ? precede(requests, insertCv, insertNcv, orderSize) : precede(requests, insertNcv, insertCv, orderSize);
                        if (prec != NOT_PRECEDE) {
                            if (i >= 144) {
                                int a = 0 ;
                            }
                            branchingRange[i] = i;
                            if (cvPickup) {
                                int finalInsertNcv;
                                if (prec == SAME_NODE) { // ncv will come right after cv
                                    heuristicVal[i] = getHeuristicVal(cv, insertCv, ncv) + getHeuristicVal(ncv, cv, route.nextMember(insertCv));
                                    finalInsertNcv = cv;
                                } else { // some node lies between cv and ncv
                                    heuristicVal[i] = getHeuristicVal(cv, insertCv, route.nextMember(insertCv)) + getHeuristicVal(ncv, insertNcv, route.nextMember(insertNcv));
                                    finalInsertNcv = insertNcv;
                                }
                                branching[i++] = () -> {
                                    cp.post(new Schedule(route, cv, insertCv),false);  // insert the critical vertex
                                    cp.post(new Schedule(route, ncv, finalInsertNcv));  // insert the non-critical vertex
                                    customersLeft.remove(id);  // the customer has been served
                                };
                            } else {
                                int finalInsertCv;
                                if (prec == SAME_NODE) { // cv will come right after ncv
                                    heuristicVal[i] = getHeuristicVal(ncv, insertNcv, cv) + getHeuristicVal(cv, ncv, route.nextMember(insertNcv));
                                    finalInsertCv = ncv;
                                } else { // some node lies between ncv and cv
                                    heuristicVal[i] = getHeuristicVal(ncv, insertNcv, route.nextMember(insertNcv)) + getHeuristicVal(cv, insertCv, route.nextMember(insertCv));
                                    finalInsertCv = insertCv;
                                }
                                branching[i++] = () -> {
                                    cp.post(new Schedule(route, ncv, insertNcv), false);  // insert the non-critical vertex
                                    cp.post(new Schedule(route, cv, finalInsertCv));  // insert the critical vertex
                                    customersLeft.remove(id);  // the customer has been served
                                };
                            }
                        }
                    }
                }

            }
            if (i == 0) // no effective insertion point was found after using precede
                throw INCONSISTENCY;
            minInsert = i;
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            // map the branching before to the branching after ordering
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (i = 0 ; i < minInsert ; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;

        }

        /**
         * tree search inserting request per request instead of node per node
         * use bound impact to insert the critical vertex and deduce the remaining insertions for the non-critical vertex
         * afterwards
         * @return branching consisting of inserting a critical vertex and a non-critical vertex into the sequence
         *  the values are ordered by heuristic
         */
        private Procedure[] boundImpactRequestTreeSearch() {
            int size = customersLeft.fillArray(requests);
            if (size == 0) {
                return EMPTY;
            }
            // consider that min insert for a request = min insert for its critical vertex
            int minInsert = Integer.MAX_VALUE; // min number of insertions found
            int nInsert;
            int cnt = 0; // number of requests with a min number of insertions
            if (getVersion() == 0) {
                for (int i = 0; i < size; ++i) { // find the request with the min number of insertions
                    nInsert = 0;
                    int cv = getCriticalVertex(requests[i]);
                    int ncv = getNonCriticalVertex(requests[i]);
                    for (SequenceVar route : routes) {
                        nInsert += route.nScheduledInsertions(cv) * route.nScheduledInsertions(ncv);
                    }
                    if (nInsert < minInsert) {
                        minInsert = nInsert;
                        insertions[0] = requests[i];
                        cnt = 1;
                    } else if (nInsert == minInsert) {
                        insertions[cnt++] = requests[i];
                    }
                }
            } else {
                for (int i = 0; i < size; ++i) { // find the request with the min number of insertions
                    nInsert = 0;
                    int cv = getCriticalVertex(requests[i]);
                    for (SequenceVar route : routes) {
                        nInsert += route.nScheduledInsertions(cv);
                    }
                    if (nInsert < minInsert) {
                        minInsert = nInsert;
                        insertions[0] = requests[i];
                        cnt = 1;
                    } else if (nInsert == minInsert) {
                        insertions[cnt++] = requests[i];
                    }
                }
            }
            if (minInsert == 0)
                throw INCONSISTENCY;
            int id = insertions[nextInt(cnt)];

            // create the branching for all pair of requests
            int vehicle = 0;
            int cv = getCriticalVertex(id);
            int ncv = getNonCriticalVertex(id);
            int i = 0;
            int hCvv;
            for (SequenceVar route : routes) {
                size = route.fillScheduledInsertions(cv, insertions);
                int finalVehicle = vehicle;
                for (int j = 0; j < size; ++j) {
                    int insertCv = insertions[j];
                    hCvv = getHeuristicVal(route, cv, insertCv);
                    cp.getStateManager().saveState();
                    try {
                        cp.post(new Schedule(route, cv, insertCv));  // insert the critical vertex
                        int sizeNcv = route.fillScheduledInsertions(ncv, insertions2);
                        for (int k = 0 ; k < sizeNcv; ++k) {
                            int insertNcv = insertions2[k];
                            branchingRange[i] = i;
                            heuristicVal[i] = hCvv + getHeuristicVal(route, ncv, insertNcv);
                            branching[i++] = () -> {
                                cp.post(new Schedule(route, cv, insertCv),false);  // insert the critical vertex
                                cp.post(new Schedule(route, ncv, insertNcv));  // insert the non-critical vertex
                                customersLeft.remove(id);  // the customer has been served
                            };
                        }
                    } catch (InconsistencyException ignored) {

                    }
                    cp.getStateManager().restoreState();
                }
                ++vehicle;
            }
            if (i == 0) // no effective insertion point was found after trypost
                throw INCONSISTENCY;
            minInsert = i;
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            // map the branching before to the branching after ordering
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (i = 0 ; i < minInsert ; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        }

        /**
         * provide the branching when searching a solution
         * if the last insert is a critical vertex, insert the corresponding non-critical vertex
         * otherwise, the last insert was a non-critical vertex (or no inserted vertex) and the critical vertex with the least
         * possible insertions is chosen
         * @return branching procedure
         */
        private Procedure[] treeSearch() {
            //System.out.println("------------------");
            int minInsert = Integer.MAX_VALUE; // min number of insertions found
            if (lastInsert.value() < 0) { // insert a critical vertex
                int nInsert;
                //int id = 0; // id of the request with the min number of insertions
                int cnt = 0; // number of requests with a min number of insertions
                int size = customersLeft.fillArray(requests);
                if (size == 0)
                    return EMPTY;
                for (int i = 0; i < size; ++i) { // find the request with the min number of insertions
                    nInsert = 0;
                    int cv = getCriticalVertex(requests[i]);
                    for (SequenceVar route : routes)
                        nInsert += route.nScheduledInsertions(cv);
                    if (nInsert < minInsert) {
                        minInsert = nInsert;
                        //id = requests[i];
                        insertions[0] = requests[i];
                        cnt = 1;
                    } else if (nInsert == minInsert) {
                        insertions[cnt++] = requests[i];
                    }
                }
                if (minInsert == 0)
                    throw INCONSISTENCY;
                int id = insertions[nextInt(cnt)];
                // insert the insertion var at each possible branching point
                int vehicle = 0;
                int cv = getCriticalVertex(id);
                int i = 0;
                int finalId = id;

                for (SequenceVar route : routes) {
                    size = route.fillScheduledInsertions(cv, insertions);
                    int finalVehicle = vehicle;
                    for (int j = 0; j < size; ++j) {
                        int insert = insertions[j];
                        branchingRange[i] = i;
                        heuristicVal[i] = getHeuristicVal(route, cv, insert);
                        branching[i++] = () -> {
                            cp.post(new Schedule(route, cv, insert));  // insert the critical vertex
                            lastInsert.setValue(finalId);  // tell the next step in the branching what was the last insertion
                            lastInsertVehicle.setValue(finalVehicle);
                        };
                    }
                    ++vehicle;
                }
            } else { // insert a non critical vertex
                int vehicle = lastInsertVehicle.value();
                SequenceVar route = routes[vehicle];
                int id = lastInsert.value();
                int ncv = getNonCriticalVertex(id);
                minInsert = route.fillScheduledInsertions(ncv, insertions);
                if (minInsert == 0) // no insertion point exist for the node in this vehicle
                    throw INCONSISTENCY;
                //branching = new Procedure[minInsert];
                for (int i = 0; i < minInsert; ++i) {
                    int insert = insertions[i];
                    branchingRange[i] = i;
                    heuristicVal[i] = getHeuristicVal(route, ncv, insert);
                    branching[i] = () -> {
                        cp.post(new Schedule(route, ncv, insert));  // insert the non-critical vertex
                        lastInsert.setValue(-1);  // tell the next step in the branching that a non-critical node was inserted
                        customersLeft.remove(id);  // the customer has been served
                    };
                }
            }
            Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
            // map the branching before to the branching after ordering
            Procedure[] branchingSorted = new Procedure[minInsert];
            for (int i = 0 ; i < minInsert ; ++i)
                branchingSorted[i] = branching[branchingRange[i]];
            return branchingSorted;
        }

        /**
         * Variable selection: SequenceVar with the least possible nodes
         * Value selection: node with the least scheduled insertions
         * order the selection according to the heuristic
         * @return branching procedure
         */
        private Procedure[] sequenceFirstFail() {
            // select the route with the least number of possible nodes
            SequenceVar route = selectMin(routes,
                    s -> !s.isBound(),
                    SequenceVar::nPossibleNode);
            if (route == null)
                return EMPTY;
            else {
                // select the node
                int node = -1;
                int minInsert = Integer.MAX_VALUE;
                int insert;
                int size = route.fillPossible(insertions);
                // take the node with the minimum number of scheduled insertions
                for (int i = 0; i < size ; ++i) {
                    insert = route.nScheduledInsertions(insertions[i]);
                    if (insert < minInsert && insert != 0) {
                        minInsert = insert;
                        node = insertions[i];
                    }
                }
                if (minInsert == Integer.MAX_VALUE) { // no schedule insertion has been found
                    minInsert = 0;
                }
                minInsert = route.fillScheduledInsertions(node, insertions); // branch on every scheduled insertion and removal for this route
                int finalNode = node;
                for (int i = 0; i < minInsert; ++i) {
                    int pred = insertions[i];
                    branchingRange[i] = i;
                    heuristicVal[i] = getHeuristicVal(route, node, pred);
                    branching[i] = () -> {
                        cp.post(new Schedule(route, finalNode, pred));
                    };
                }
                // sort the nodes according to the heuristic value
                Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
                // map the branching before to the branching after ordering
                Procedure[] branchingSorted = new Procedure[minInsert + 1];
                for (int i = 0 ; i < minInsert ; ++i)
                    branchingSorted[i] = branching[branchingRange[i]];
                branchingSorted[minInsert] = () -> {
                    cp.post(new Exclude(route, finalNode));
                };
                return branchingSorted;
            }
        }

        /**
         * gives the heuristic value for a considered insertion, taking the service time and travel time into account
         * @param route Sequence Var for the insertion
         * @param node node that is going to be inserted
         * @param pred predecessor for the inserted node
         * @return heuristic value. The lower the value, the better the insertion
         */
        int getHeuristicVal(SequenceVar route, int node, int pred) {
            int succ = route.nextMember(pred);
            int slack = servingTime[succ].max() - (servingTime[pred].min() + serviceTime[pred] + travelTimeMatrix[pred][node] + serviceTime[node] + travelTimeMatrix[node][succ]);
            int objChange = travelTimeMatrix[node][succ] + travelTimeMatrix[pred][node] - travelTimeMatrix[pred][succ];
            return ALPHA * objChange - BETA * slack;
        }

        int getHeuristicVal(int node, int pred, int succ) {
            int slack = servingTime[succ].max() - (servingTime[pred].min() + serviceTime[pred] + travelTimeMatrix[pred][node] + serviceTime[node] + travelTimeMatrix[node][succ]);
            int objChange = travelTimeMatrix[node][succ] + travelTimeMatrix[pred][node] - travelTimeMatrix[pred][succ];
            return ALPHA * objChange - BETA * slack;
        }


        int getCriticalVertex(int request) {
            if (twStart[request] > 0 || twEnd[request] < darp.timeHorizon) return request;
            else return  request + darp.nRequests;
        }

        int getNonCriticalVertex(int request) {
            if (twStart[request] > 0 || twEnd[request] < darp.timeHorizon) return request + darp.nRequests;
            else return  request;
        }

        int getCorrespondingRequest(int i) {
            if (i < darp.nRequests) return i;
            else return  i - darp.nRequests;
        }

        private void updateSolutionFfpa(DARPInstance.DARPSolution solution, DARPSolveStatistics solveStatistics) {
            float prob = nextFloat();
            int foundObjective = solution.getObjectiveScaled();
            //int foundObjective = sumRouteDuration.min();
            if (foundObjective < currentSolutionObjective || prob < probUpdateSol) {
                currentSolution = solution;
                currentSolutionObjective = foundObjective;
                if (foundObjective < bestSolutionObjective) {
                    bestSolution = currentSolution;
                    bestSolutionObjective = foundObjective;
                    if (getVerbosity() > 0) {
                        System.out.printf("best solution %d\n", bestSolutionObjective);
                        if (getVerbosity() > 1) {
                            printCurrentRoutes();
                        }
                    }
                    if (solveStatistics != null) {
                        solveStatistics.addSolution(bestSolution);
                    }
                }
            }
        }

        private void updateSolutionFfpa(DARPSolveStatistics solveStatistics) {
            float prob = nextFloat();
            int foundObjective = 0;
            for (IntVar dur: routeLength)
                foundObjective += dur.max();

            if (foundObjective < currentSolutionObjective || prob < probUpdateSol) {
                currentSolution = constructSolution();
                //updateCurrentSolution();
                currentSolutionObjective = foundObjective;
                if (foundObjective < bestSolutionObjective) {
                    bestSolution = currentSolution.copy();
                    bestSolutionObjective = foundObjective;
                    if (getVerbosity() > 0) {
                        System.out.printf("best solution %d\n", bestSolutionObjective);
                        if (getVerbosity() > 1) {
                            printCurrentRoutes();
                        }
                    }
                    if (solveStatistics != null) {
                        bestSolution.computeNoExcept();
                        solveStatistics.addSolution(bestSolution);
                    }
                }
            }
        }

        private void updateSolutionFfpa(DARPInstance.DARPSolution solution) {
            updateSolutionFfpa(solution, null);
        }

    }

    /**
     * used for constraints checking: from a given solution file, parse the solution and post the given order for all sequence variables
     * @param instance instance related to the provided solution
     * @param filename file where the solution is written.
     *                 The solution should be written as series of nodes ids (int), separated by "->"
     * @throws IOException in case of error when opening / reading / closing the file
     */
    public static void postSequenceInFile(DARPInstance instance, String filename) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename)));
        String line = reader.readLine();
        String route = "";
        while (line != null) {
            if (line.contains("->")) {
                if (route.equals(""))
                    route = line;
                else
                    route += '\n' + line;
            }
            else if (!route.equals("")){
                DARPInsertion solver = new DARPInsertion(instance);
                try {
                    solver.cp.postSequence(route);
                } catch (InconsistencyException e) {
                    System.err.println(route + "threw inconsistency");
                    reader.close();
                    return;
                }
                route = "";
            }
            line = reader.readLine();
        }
        reader.close();
    }


    public static void main(String[] args) {
        //DARPInstance instance = new DARPInstance("data/darp/Cordeau2003/a13-144.txt");
        DARPInstance instance = new DARPInstance(args[0]);
        DARPInsertion solver = new DARPInsertion(instance);
        solver.setVersion(2);
        solver.setSeed( -241020543);
        int maxRunTime = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        int verbosity = args.length > 2 ? Integer.parseInt(args[2]) : 1;
        solver.setVerbosity(verbosity);
        DARPSolveStatistics solveStats = new DARPSolveStatistics(maxRunTime);

        solver.solveLns(instance, solveStats);
        if (verbosity > 1) {
            System.out.println(solveStats);
            System.out.println("\n" + solveStats.bestSolution());
        }
    }


}
