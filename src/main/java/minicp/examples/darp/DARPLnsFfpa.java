package minicp.examples.darp;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.state.StateSparseSet;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.EMPTY;
import static minicp.cp.BranchingScheme.branch;
import static minicp.cp.Factory.*;

public class DARPLnsFfpa extends DARPSolver{

    @Override
    public DARPInstance.DARPSolution solve(DARPInstance instance, DARPSolveStatistics solveStatistics) {
        initCp(instance);
        solveStatistics.init();
        SearchStatistics stats = cp.solve(solveStatistics, null, true, false);
        solveStatistics.finish();
        solveStatistics.setSearchStatistics(stats);
        return cp.bestSolution;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPSolveStatistics solveStatistics) {
        initCp(instance);
        solveStatistics.solveWithProc(() -> cp.solve(solveStatistics, null, false, false));
        return cp.bestSolution;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPInstance.DARPSolution initSolution, DARPSolveStatistics solveStatistics) {
        initCp(instance);
        solveStatistics.solveWithProc(() -> cp.solve(solveStatistics, initSolution, false, false));
        return cp.bestSolution;
    }

    @Override
    public void solveAll(DARPInstance instance, DARPSolveStatistics solveStatistics) {
        initCp(instance);
        solveStatistics.init();
        SearchStatistics stats = cp.solve(solveStatistics, null, false, true);
        solveStatistics.finish();
        solveStatistics.setSearchStatistics(stats);
    }

    @Override
    public String description() {
        return "lns_ffpa";
    }

    public DARPLnsFfpa(DARPInstance instance) {
        initCp(instance);
    }
    
    public void initCp(DARPInstance instance) {
        cp = new DARPCp(instance);
    }

    DARPCp cp;
    boolean printAllDetails = false;

    /**
     * class related to the modelling and solving of a DARP instance
     */
    public class DARPCp {

        public record BranchingChoice(int request, int vehicle, int cvi, int ncvi, int change) implements Comparable<BranchingChoice> {
            public int compareTo(BranchingChoice other) {
                if (this.change == other.change) {
                    return this.request - other.request;
                }
                return - (this.change - other.change);
            }

            public String completeDesc() {
                return String.format("(%d,%d,%d,%d)", vehicle, cvi, ncvi, change);
            }

            public String toString() {
                return String.format("BranchingChoice(%d,%d,%d,%d,%d)", request, cvi, ncvi, change, vehicle);
            }
        }

        // Variables related to modelling
        IntVar[] servingTime;
        IntVar [] servingVehicle;

        StateInt[] succ;
        StateInt[] pred;
        StateInt[] capaLeftInRoute;

        // parameters for the search
        final int ALPHA = 80;
        private int tau = 1000;
        private int gamma = 200;
        private int maxSize;
        private int range = 4;
        private int numIter = 300;
        private double probUpdateSol = 0.07; // probability to accept a solution even if its cost is greater than the previous best found solution

        int [] twStart, twEnd, vertexLoadChange, servingDuration;

        int [][] travelTimeMatrix;
        int numVars;
        int nVehicles;

        Solver cp = makeSolver();
        DARPInstance.DARPSolution bestSolution;
        int bestSolutionObjective = Integer.MAX_VALUE;
        DARPInstance.DARPSolution currentSolution; // used for diversification in the LNS-FFPA
        int currentSolutionObjective = Integer.MAX_VALUE;
        ArrayList<DARPInstance.Stop> stops; // used to construct and verify solutions through DARPInstance

        // array of [requests] -> {vehicle -> {critical node -> {non critical node -> objective value change }}}
        HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>[] insertionObjChange;

        int rangeRequestMin, rangeRequestMax;
        int rangePickupMin, rangePickupMax;
        int rangeDeliveryMin, rangeDeliveryMax;
        int rangeDepotMin, rangeDepotMax;
        int rangeStartDepotMin, rangeStartDepotMax;
        int rangeEndDepotMin, rangeEndDepotMax;

        StateSparseSet customersLeft; // remaining requests that must be processed
        int[] remainingRequest;

        DARPInstance darp;

        public DARPCp(DARPInstance darpInstance) {
            this.darp = darpInstance;
            numVars = darpInstance.nRequests * 2 + 2 * darpInstance.nVehicles;
            maxSize = darpInstance.nRequests / 2;
            nVehicles = darpInstance.nVehicles;

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

            // vertexLoadChange, twStart, twEnd, servingDuration
            travelTimeMatrix = new int[numVars][numVars];
            DARPInstance.Coordinate[] coords = new DARPInstance.Coordinate[numVars];
            vertexLoadChange = new int[numVars];
            servingDuration = new int[numVars];
            twEnd = new int[numVars];
            twStart = new int[numVars];
            remainingRequest = new int[rangeRequestMax];
            for (int i = 0; i < 2* darp.nRequests; i++) {
                coords[i] = darp.stops[i].coord();
                vertexLoadChange[i] = darp.stops[i].loadChange();
                twStart[i] = darp.stops[i].twStart();
                twEnd[i] = darp.stops[i].twEnd();
                servingDuration[i] = darp.stops[i].servingDuration();
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                coords[i] = darp.startDepot.coord();
                vertexLoadChange[i] = 0;
                twStart[i] = darp.startDepot.twStart();
                twEnd[i] = darp.startDepot.twEnd();
                servingDuration[i] = darp.startDepot.servingDuration();
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                coords[i] = darp.endDepot.coord();
                vertexLoadChange[i] = 0;
                twStart[i] = darp.endDepot.twStart();
                twEnd[i] = darp.endDepot.twEnd();
                servingDuration[i] = darp.endDepot.servingDuration();
            }

            // travel time
            for (int i = 0; i < numVars; i++) {
                for (int j = i+1; j < numVars; j++) {
                    travelTimeMatrix[i][j] = (int) Math.round(coords[i].euclideanDistance(coords[j]));
                    travelTimeMatrix[j][i] = (int) Math.round(coords[j].euclideanDistance(coords[i]));
                }
            }

            insertionObjChange = new HashMap[darp.nRequests]; // used for the heuristic
            for (int i=0; i < darp.nRequests ; ++i)
                insertionObjChange[i] = new HashMap<>();

            // arraylist of stops used in the solutions. Indexing corresponds to the indexing used in this representation
            this.stops = new ArrayList<>(); // no need to specify end nodes, only pickup and drops
            for (int i = 0; i < 2* darp.nRequests; i++)
                stops.add(darp.stops[i]);

            initVars();
            postConstraints();
        }

        private void initVars() {
            // predecessor successor and capaLeftInRoute state initialization
            succ = new StateInt[numVars];
            pred = new StateInt[numVars];
            capaLeftInRoute = new StateInt[numVars];
            for (int i = 0; i < numVars; i++) {
                succ[i] = cp.getStateManager().makeStateInt(0);
                pred[i] = cp.getStateManager().makeStateInt(0);
                capaLeftInRoute[i] = cp.getStateManager().makeStateInt(darp.vehicleCapacity);
            }

            for (int i = 0; i < 2 * darp.nRequests; i++) {
                succ[i].setValue(i);
                pred[i].setValue(i);
            }

            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                succ[i].setValue(i+darp.nVehicles);
                pred[i].setValue(succ[i].value());
            }

            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                succ[i].setValue(i-darp.nVehicles);
                pred[i].setValue(succ[i].value());
            }

            servingTime = new IntVar[numVars];
            for (int i = 0; i < 2*darp.nRequests; i++) {
                servingTime[i] = makeIntVar(cp,darp.stops[i].twStart(), darp.stops[i].twEnd(), true);
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                servingTime[i] = makeIntVar(cp,darp.startDepot.twStart(), darp.startDepot.twEnd(), true);
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                servingTime[i] = makeIntVar(cp,darp.endDepot.twStart(), darp.endDepot.twEnd(), true);
            }

            servingVehicle = new IntVar[numVars];
            for (int i = 0; i < 2*darp.nRequests; i++) {
                servingVehicle[i] = makeIntVar(cp,darp.nVehicles);
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                servingVehicle[i] = makeIntVar(cp,i-rangeDepotMin, i-rangeDepotMin);
                servingVehicle[i + darp.nVehicles] = makeIntVar(cp, i -rangeDepotMin, i -rangeDepotMin);
            }

            customersLeft = new StateSparseSet(cp.getStateManager(),darp.nRequests,0);
        }

        private void postConstraints() {
            // serving time pickup + serving + travel time to delivery <= serving time delivery
            for (int i = 0; i < darp.nRequests; i++) {
                int servingDuration = darp.stops[i].servingDuration();
                int timeToDelivery = travelTimeMatrix[i][i+darp.nRequests];
                cp.post(lessOrEqual(plus(servingTime[i],servingDuration + timeToDelivery),
                        servingTime[i+darp.nRequests]));
            }
            // max ride time constraint
            for (int i = 0; i < darp.nRequests; i++) {
                int servingDuration = darp.stops[i].servingDuration();
                //IntVar val = sum(servingTime[i+darp.nRequests], minus(plus(servingTime[i], servingDuration)));
                //cp.post(lessOrEqual(val, darp.maxRideTime));
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

            // pickup and drop are served by the same vehicle
            for (int i = 0; i < darp.nRequests; i++) {
                cp.post(equal(servingVehicle[i], servingVehicle[i+darp.nRequests]));
            }

            // serving time end depot i - serving time start depot i <= timeHorizon
            //for (int v = 0; v < nVehicles; ++v) {
            //    cp.post(lessOrEqual(sum(servingTime[getEndDepot(v)], minus(servingTime[getBeginDepot(v)])),
            //            darp.timeHorizon));
            //}
        }

        /**
         * initialize the hashmap of insertions costs
         */
        private void initDict() {
            for (int v = 0; v < this.darp.nVehicles; ++v)
                for (int i = 0; i < this.darp.nRequests; ++i)
                    setInsertionCost(i, v);
        }

        /** solving functions */

        public SearchStatistics solve(DARPSolveStatistics solveStatistics, DARPInstance.DARPSolution initSolution, boolean firstSolOnly, boolean allSolution) {
            // construct the dictionary
            initDict();
            SearchStatistics stats = null;
            DFSearch search = makeDfs(this.cp, this::treeSearch);
            search.onSolution(() -> {
                DARPInstance.DARPSolution solution = constructSolution();
                solution.computeNoExcept();
                updateSolutionFfpa(solution, solveStatistics);
            });
            if (allSolution) {
                stats = search.solve(searchStatistics -> solveStatistics.isFinished());
            } else {
                boolean feasibleSolution = false;
                if (initSolution == null) {
                    int maxFailure = Math.max(gamma * nVehicles, tau);
                    while (!feasibleSolution && !solveStatistics.isFinished()) {
                        stats = search.solveSubjectTo(searchStatistics -> {
                            boolean criterion = searchStatistics.numberOfSolutions() > 0;
                            return criterion || solveStatistics.isFinished() || searchStatistics.numberOfFailures() > maxFailure;
                        }, () -> {
                            ;
                        });
                        feasibleSolution = stats.numberOfSolutions() > 0;
                    }
                } else {
                    if (getVerbosity() > 0)
                    System.out.println("initial solution provided");
                    initSolution.computeNoExcept();
                    updateSolutionFfpa(initSolution, solveStatistics);
                    feasibleSolution = true;
                }
                if (!firstSolOnly && feasibleSolution) { // continue using LNS
                    int range = 5;
                    int numIters = 300;
                    int maxRange = darp.nRequests / 2 - range;
                    boolean running = !solveStatistics.isFinished();
                    for (int minNeighborhood = 2; minNeighborhood <= maxRange && running; ++minNeighborhood) {
                        if (minNeighborhood == maxRange)
                            minNeighborhood = 2; // reset of the neighborhood
                        for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                            for (int i = 0; i < numIters && running; ++i) {
                                int nRelax = minNeighborhood + offsetNeighborhood;
                                stats = search.solveSubjectTo(searchStatistics -> (searchStatistics.numberOfSolutions() == 1 ||
                                                solveStatistics.isFinished()),
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
         * relax the current instance, by setting the successor to the value of the previous solution found
         * and setting nRelax requests unbound
         */
        public void relax(int nRelax) {
            // select the customers that will be relaxed
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
            //System.out.print("relaxed customers = Set(");
            //for (int i = 0; i < relaxEnd-1 ; ++i) {
            //    System.out.print(possibleCustomers[i] + ", ");
            //}
            //System.out.println(possibleCustomers[relaxEnd-1] + ")");
            // relax the values of the successors
            for (int i = 0; i < relaxEnd ; ++i) { // relaxed customers
                succ[possibleCustomers[i]].setValue(possibleCustomers[i]); // pickup points toward itself
                pred[possibleCustomers[i]].setValue(possibleCustomers[i]);
                succ[possibleCustomers[i] + darp.nRequests].setValue(possibleCustomers[i] + darp.nRequests); // delivery points towards itself
                pred[possibleCustomers[i] + darp.nRequests].setValue(possibleCustomers[i] + darp.nRequests);
            }
            for (int i = relaxEnd; i < darp.nRequests ; ++i) { //
                customersLeft.remove(possibleCustomers[i]);
            }

            for (int v = 0; v < darp.nVehicles; ++v) {
                int begin = getBeginDepot(v);
                int end = getEndDepot(v);
                int prev = begin;
                for (int current: currentSolution.succ[v]) {
                    if (!customersLeft.contains(getCorrespondingRequest(current))) {
                        pred[current].setValue(prev);
                        succ[prev].setValue(current);
                        cp.post(lessOrEqual(getArrivalTime(prev, current), servingTime[current])); // setup of the serving time
                        cp.post(equal(servingVehicle[current], v)); // the vehicle goes through this node
                        prev = current; // only updated when a non-relaxed node is met, to complete the partial route
                    }
                }
                if (prev != end) { // the route goes through at least one request
                    pred[end].setValue(prev);
                    succ[prev].setValue(end);
                    cp.post(lessOrEqual(getArrivalTime(prev, end), servingTime[end]));
                }
                updateCapaLeftInRoute(v, -1); // compute the current capacity in the route
            }
        }

        /**
         * construct a solution based on the current values for the successors array
         * @return
         */
        public DARPInstance.DARPSolution constructSolution() {
            DARPInstance.DARPSolution solution = darp.constructSolution(stops);
            //printCurrentRoutes();
            return addSolutionRoute(solution);
        }

        public DARPInstance.DARPSolution constructSolution(DARPInstance.DARPSolution initSolution) {
            initSolution.reset();
            return addSolutionRoute(initSolution);
        }

        private DARPInstance.DARPSolution addSolutionRoute(DARPInstance.DARPSolution initSolution) {
            for (int vehicle = 0; vehicle < darp.nVehicles; ++vehicle) {
                int current = succ[rangeStartDepotMin + vehicle].value(); // depot start
                int end = rangeEndDepotMin + vehicle; // corresponding depot end
                while (current != end) {
                    initSolution.addStop(vehicle, current);
                    current = succ[current].value();
                }
            }
            return initSolution;
        }

        public void updateSolution(DARPInstance.DARPSolution solution) {
            if (solution.getObjective() < currentSolutionObjective) { // only current solution is used here
                currentSolution = solution;
                currentSolutionObjective = solution.getObjectiveScaled();
                if (getVerbosity() > 0)
                    System.out.println("solution found with value " + solution.getObjective());
                //printCurrentRoutes();
            }
        }

        private void updateSolutionFfpa(DARPInstance.DARPSolution solution, DARPSolveStatistics solveStatistics) {
            float prob = nextFloat();
            if (printAllDetails) {
                System.out.println("proba = " + prob);
            }
            int foundObjective = solution.getObjectiveScaled();
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

        public void updateSolutionFfpa(DARPInstance.DARPSolution solution) {
            updateSolutionFfpa(solution, null);
        }

        public Procedure[] treeSearch() {
            if (customersLeft.isEmpty())
                return EMPTY;
            int request = getUnassignedRequest();
            //System.out.println("inserting request " + request);
            ArrayList<BranchingChoice> insertions = getInsertionPoints(request, true);
            //insertions.forEach(i -> System.out.println("\t" + i));
            if (printAllDetails) {
                System.out.println("search: choosing request " + request + " with insertions points (" + insertions.size() + "):");
                System.out.print("\tArray(");
                int size = insertions.size();
                for (int i = 0 ; i < size; ++i) {
                    BranchingChoice bc = insertions.get(i);
                    System.out.print(bc.completeDesc());
                    if (i != size - 1)
                        System.out.print(", ");
                }
                System.out.println(")");
            }
            //System.out.println("-------------");
            //ArrayList<BranchingChoice> insertions = getUnassignedRequest();
            if (insertions.size() == 0) { // no remaining request can be inserted in the current configuration
                throw new InconsistencyException();
            }
            Procedure[] branching = new Procedure[insertions.size()];

            int i = 0;
            for (BranchingChoice a : insertions) { // list of insertion points, sorted by the heuristic value
                final BranchingChoice insert = a;
                branching[i] = () -> {
                    // the vehicle goes through the request
                    int cvv = getCriticalVertex(insert.request);
                    int ncv = getCorrespondingVertex(cvv);
                    if (!tryPost(equal(servingVehicle[insert.request], servingVehicle[insert.cvi]))) {
                        //System.out.println("failed to assign a serving vehicle");
                        throw new InconsistencyException(); // vehicle assignment failed
                    }
                    // constraint for the critical vertex
                    if (!insertVertexIntoRoute(cvv, insert.cvi)) {
                        //System.out.println("failed to insert critical node");
                        throw new InconsistencyException(); // insertion failed
                    }
                    // constraint for the non critical vertex
                    if (!insertVertexIntoRoute(ncv, insert.ncvi)) {
                        //System.out.println("failed to insert non critical node");
                        throw new InconsistencyException(); // insertion failed
                    }
                    // update the capacity
                    //updateCapaLeftInRoute(insert.vehicle, insert.request);
                    updateCapaLeftInRoute(insert.vehicle, -1);
                    int begin = getBeginDepot(insert.vehicle);
                    int end = getEndDepot(insert.vehicle);
                    while (begin != end) {
                        if (capaLeftInRoute[begin].value() < 0) {
                            //System.out.println("negative capacity found");
                            throw new InconsistencyException(); // capacity did not permit to insert request
                        }
                        begin = succ[begin].value();
                    }
                    // request has been proceed
                    customersLeft.remove(insert.request);
                };
                ++i;
            }
            return branch(branching);
        }

        /**
         * get a sorted list of possible insertion points for a request, sorted by heuristic values
         * also update the dictionary of possible insertions points (insertionObjChange)
         */
        public int getUnassignedRequest() {
            int size = customersLeft.fillArray(remainingRequest);
            for (int i = 0 ; i < size ; ++i) { // update insertionObjChange
                int request = remainingRequest[i];
                insertionObjChange[request].clear();
                for (int v = 0; v < darp.nVehicles ; ++v) {
                    setInsertionCost(request, v);
                }
            }
            return getUnassignedMinVehicleMinInsertionPointsRequest();
        }

        public int getUnassignedMinVehicleMinInsertionPointsRequest() {
            int minVehicles   = darp.nVehicles;  // minimum number of vehicles found
            int minChoices = Integer.MAX_VALUE;  // minimum number of insertion points found
            int maxObjChange  = Integer.MIN_VALUE;  // maximum increase in objective value found
            ArrayList<BranchingChoice> insertions = new ArrayList<>(); // best insertions found

            // the request can be inserted in the fewest vehicles
            int size = customersLeft.fillArray(remainingRequest);
            //Arrays.sort(remainingRequest, 0, size);
            for (int i = 0; i < size ; ++i) {
                int request = remainingRequest[i];
                if (request == 90) {
                    int a = 0;
                }
                int nVehicle = servingVehicle[request].size();
                int numChoices = getNumInsertionPoints(request);
                int objChange = Integer.MIN_VALUE;
                if (nVehicle > minVehicles)
                    continue;
                ArrayList<BranchingChoice> branchingQueue = getInsertionCost(request, false);
                if (nVehicle < minVehicles) {
                    // new min number of vehicles has been found
                    minVehicles = nVehicle;
                    // minInsertions = nInsertions;
                    // add insertions option
                    insertions.clear();
                    //insertions.addAll(getInsertionPoints(request, false));
                    insertions.addAll(branchingQueue);
                }
                else if (nVehicle == minVehicles && numChoices < minChoices) {
                    // min number of vehicles and less insertions possibles
                    minChoices = numChoices;
                    // add insertions option
                    insertions.clear();
                    //insertions.addAll(getInsertionPoints(request, false));
                    insertions.addAll(branchingQueue);
                }
                else if (nVehicle == minVehicles && numChoices == minChoices && objChange >= maxObjChange) {
                    // TODO best insertion point produces smallest amount in objective value? should reduce diversity, maybe not a good idea. Was absent from the comet code anyway
                    //maxObjChange = objChange;
                    // add insertions option
                    //insertions.addAll(getInsertionPoints(request, false));
                    insertions.addAll(branchingQueue);
                }
            }
            int insertSize = insertions.size();
            if (insertSize == 0)
                throw new InconsistencyException();
            int nextInt = nextInt(insertSize);
            int currentRequest = insertions.get(nextInt).request;
            if (printAllDetails) {
                System.out.println("getUnassignedMinVehicleMinInsertionPointsRequest: length = " + insertSize + " choosing number " +
                        nextInt + " giving request " + currentRequest);
            }
            //System.out.println("choice of min request length: " + insertions.size() + " choosing rand = " + nextRand + " giving request " + currentRequest);
            BranchingChoice bc = getBestBranchingDecision(currentRequest);
            return bc.request;
        }

        public BranchingChoice getBestBranchingDecision(int request) {
            int cvv = getCriticalVertex(request);
            int ncv = getCorrespondingVertex(cvv);
            int bestCvi = -1;
            int bestNcvi = -1;
            int bestChange = Integer.MIN_VALUE;
            int change;
            ArrayList<BranchingChoice> branchingQueue = new ArrayList<>();
            for (int v = 0; v < darp.nVehicles ; ++ v) {
                if (insertionObjChange[request].containsKey(v) && servingVehicle[request].contains(v)) {
                    for (int cvi : insertionObjChange[request].get(v).keySet()) {
                        for (int ncvi : insertionObjChange[request].get(v).get(cvi).keySet()) {
                            change = insertionObjChange[request].get(v).get(cvi).get(ncvi);
                            if (change > bestChange) {
                                bestChange = change;
                                bestCvi = cvi;
                                bestNcvi = ncvi;
                                branchingQueue.clear();
                                branchingQueue.add(new BranchingChoice(request, v, cvi, ncvi, change));
                            }
                            else if (change == bestChange) {
                                branchingQueue.add(new BranchingChoice(request, v, cvi, ncvi, change));
                            }
                        }
                    }
                }
            }
            int nextInt = nextInt(branchingQueue.size());
            BranchingChoice ret = branchingQueue.get(nextInt);
            //System.out.println("number of insertions points for request: " + branchingQueue.size() + " choosing rand = " + nextRand + " giving " + branchingQueue.get(nextRand));
            if (printAllDetails) {
                System.out.println("getBestBranchingDecision: : length = " + branchingQueue.size() + " choosing number " +
                        nextInt + " giving value " + ret.toString());
            }
            return ret;
        }

        public ArrayList<BranchingChoice> getInsertionCost(int request, boolean sorted) {
            ArrayList<BranchingChoice> branchingQueue = new ArrayList<>();
            int cvv = getCriticalVertex(request);
            int ncv = getCorrespondingVertex(cvv);
            int bestCvi = -1;
            int bestNcvi = -1;
            int bestChange = Integer.MIN_VALUE;
            for(int v = 0; v < nVehicles; ++v) {
                if (insertionObjChange[request].containsKey(v) && servingVehicle[request].contains(v)) {
                    for (int cvi: insertionObjChange[request].get(v).keySet()) {
                        for (int ncvi : insertionObjChange[request].get(v).get(cvi).keySet()) {
                            int change = insertionObjChange[request].get(v).get(cvi).get(ncvi);
                            if (change > bestChange) {
                                bestChange = change;
                                bestCvi = cvi;
                                bestNcvi = ncvi;
                                branchingQueue.clear();
                                branchingQueue.add(new BranchingChoice(request, servingVehicle[cvi].min(), cvi, ncvi, bestChange));
                            } else if (change == bestChange) {
                                branchingQueue.add(new BranchingChoice(request, servingVehicle[cvi].min(), cvi, ncvi, bestChange));
                            }
                        }
                    }
                }
            }
            return branchingQueue;
        }

        public ArrayList<BranchingChoice> getInsertionPoints(int request, boolean sorted) {
            // return an array of possible insertion points, sorted or not
            ArrayList<BranchingChoice> insertionPoints = new ArrayList<>();
            for (int v: insertionObjChange[request].keySet()) {
                for (int criticalNode : insertionObjChange[request].get(v).keySet()) {
                    for (int nonCriticalNode : insertionObjChange[request].get(v).get(criticalNode).keySet()) {
                        int change = insertionObjChange[request].get(v).get(criticalNode).get(nonCriticalNode);
                        insertionPoints.add(new BranchingChoice(request, v, criticalNode, nonCriticalNode, change));
                    }
                }
            }
            if (sorted) // sort by increasing value of heuristic
                Collections.sort(insertionPoints);
            return insertionPoints;
        }

        public int getNumInsertionPoints(int request) {
            int insertions = 0;
            for (int v: insertionObjChange[request].keySet())
                for (int criticalNode: insertionObjChange[request].get(v).keySet())
                    for (int nonCriticalNode: insertionObjChange[request].get(v).get(criticalNode).keySet())
                        ++insertions;
            return insertions;
        }

        /**
         * set the insertion cost of inserting request request into the road taken by vehicle v
         */
        public void setInsertionCost(int request, int v) {
            int cvv = getCriticalVertex(request);
            // int ncv = getCorrespondingVertex(cvv);

            int begin = getBeginDepot(v);
            int end = getEndDepot(v);

            int start = succ[begin].value();

            while(start != end){
                if(getArrivalTimeValue(pred[start].value(), cvv, true) <= servingTime[cvv].max() && getArrivalTimeValue(cvv, start, true) <= servingTime[start].max()){
                    // cvv node can be inserted between pred[start] and start
                    setBestServingTimeFail(request, v, start);
                }
                start = succ[start].value();
            }
            if(getArrivalTimeValue(pred[start].value(), cvv, true) <= servingTime[cvv].max() && getArrivalTimeValue(cvv, start, true) <= servingTime[start].max()){
                // cvv node can be inserted between pred[start] and start
                setBestServingTimeFail(request, v, start);
            }
        }

        public void setBestServingTimeFail(int request, int v, int start) {
            int begin = getBeginDepot(v);
            int end = getEndDepot(v);
            int cvv = getCriticalVertex(request);
            int ncv = getCorrespondingVertex(cvv);

            // lower bound on cvv serving time
            int cvvMinServingTime = Math.max(getArrivalTimeValue(pred[start].value(), cvv, true), twStart[cvv]);
            // upper bound on cvv serving time
            int cvvMaxServingTime = Math.min(servingTime[start].max() - travelTimeMatrix[start][cvv] - servingDuration[cvv], twEnd[cvv]);
            if (cvvMaxServingTime < cvvMinServingTime) // cvv cannot be inserted between pred[start] and start because of time constraints
                return;
            // change in heuristic value (slack) if cvv is inserted between pred[start] and start
            int changeCvv = servingTime[start].max() - (getArrivalTimeValue(pred[start].value(), cvv, true) + servingDuration[cvv] + travelTimeMatrix[cvv][start]);
            if (changeCvv < 0)
                return;

            // heuristic value for the increase in rooting cost
            changeCvv -= ALPHA * (travelTimeMatrix[cvv][start] + travelTimeMatrix[pred[start].value()][cvv] - travelTimeMatrix[pred[start].value()][start]);

            // fake the insertion
            succ[cvv].setValue(start);
            pred[cvv].setValue(pred[start].value());
            succ[pred[cvv].value()].setValue(cvv);
            pred[start].setValue(cvv);

            int minRideTime;        // lower bound on the ride time to reach the node
            int ncvMinServingTime;  // lower bound on ncv serving time
            int ncvMaxServingTime;  // upper bound on ncv serving time

            // change induced by the non critical vertex. Needs to consider every possible intersection point
            int current;
            int prev;
            if (isPickup(cvv)) { // ncv must be placed after cvv. Follow path from successor to successor
                current = start;
                prev = cvv;
                boolean done = false;
                while (current != begin && !done) { // as long as every node remaining in the route is not tried
                    if (prev == cvv) { // first iteration: can we insert ncv right after cvv?
                        minRideTime = travelTimeMatrix[cvv][ncv];
                    } else { // visit some node before inserting ncv
                        minRideTime = servingTime[prev].min() + servingDuration[prev] + travelTimeMatrix[prev][ncv] - (cvvMaxServingTime + servingDuration[cvv]);
                    }
                    if (minRideTime > darp.maxRideTime) {
                        done = true; // not possible to reach ncv with this route. Stop trying additional nodes
                    }

                    ncvMinServingTime = Math.max(getArrivalTimeValue(prev, ncv, true), twStart[ncv]);
                    ncvMaxServingTime = Math.min(servingTime[current].max() - travelTimeMatrix[current][ncv] - servingDuration[ncv], twEnd[ncv]);

                    int changeNcv = servingTime[current].max() - (getArrivalTimeValue(pred[current].value(), ncv, true) + servingDuration[ncv] + travelTimeMatrix[ncv][current]);
                    if (ncvMaxServingTime >= ncvMinServingTime && changeNcv >= 0) { // impossible to reach ncv using this node. Trying the next node in the route
                        // change in heuristic value (rooting cost) if ncv is inserted between pred[current] and current
                        changeNcv -= ALPHA * (travelTimeMatrix[ncv][current] + travelTimeMatrix[pred[current].value()][ncv] - travelTimeMatrix[pred[current].value()][current]);
                        // update the cost of inserting cvv before start and ncv before current
                        addToInsertionObjChange(request, v, start, current, changeCvv + changeNcv);
                        if (capaLeftInRoute[current].value() < vertexLoadChange[cvv])  // cannot insert anymore because of capacity
                            done = true; // no need to keep trying on this path as it is invalid, next nodes will also invalidate the capacity
                    }
                    prev = current;
                    current = succ[current].value();
                }
            }
            else { // ncv must be placed before cvv. Follow path from predecessor to predecessor
                current = cvv;
                prev = pred[cvv].value();
                boolean done = false;
                while (current != begin && (capaLeftInRoute[current].value() >= vertexLoadChange[ncv]) && !done) {
                    if (current == cvv) { // first insertion: can we place ncv just before cvv?
                        minRideTime = travelTimeMatrix[ncv][cvv];
                    } else {  // there is a least one node between ncv and cvv
                        minRideTime = cvvMinServingTime - (getArrivalTimeValue(prev, ncv, false) + servingDuration[ncv]);
                    }
                    if (minRideTime > darp.maxRideTime) { // not possible to place ncv on this route. Stop trying additional nodes
                        done = true;
                    }

                    ncvMinServingTime = Math.max(getArrivalTimeValue(prev, ncv, true), twStart[ncv]);
                    ncvMaxServingTime = Math.min(servingTime[current].max() - travelTimeMatrix[current][ncv] - servingDuration[ncv], twEnd[ncv]);
                    int changeNcv = servingTime[current].max() - (getArrivalTimeValue(pred[current].value(), ncv, true) + servingDuration[ncv] + travelTimeMatrix[ncv][current]);
                    if (ncvMaxServingTime >= ncvMinServingTime && changeNcv >= 0) {
                        // change in heuristic value induced by the rooting cost if ncv is inserted between pred[current] and current
                        changeNcv -= ALPHA * (travelTimeMatrix[ncv][current] + travelTimeMatrix[pred[current].value()][ncv] - travelTimeMatrix[pred[current].value()][current]);
                        // update the cost of inserting cvv before start and ncv before current
                        addToInsertionObjChange(request, v, start, current, changeCvv + changeNcv);
                    }

                    // change in heuristic value induced by the slack if ncv is inserted between pred[current] and current
                    current = prev;
                    prev = pred[prev].value();
                }
            }
            // removal of the fake insertion
            succ[pred[cvv].value()].setValue(succ[cvv].value());
            pred[succ[cvv].value()].setValue(pred[cvv].value());
            succ[cvv].setValue(cvv);
            pred[cvv].setValue(cvv);

        }

        /**
         * add the cost change for the insertion of request request for vehicle v, when the insertion of the request
         * is done after the node cvi and after the node ncvi
         */
        public void addToInsertionObjChange(int request, int v, int cvi, int ncvi, int change) {
            HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> objChange = insertionObjChange[request];
            if (!objChange.containsKey(v)) {
                objChange.put(v, new HashMap<>());
            }
            if (!objChange.get(v).containsKey(cvi)) {
                objChange.get(v).put(cvi, new HashMap<>());
            }
            objChange.get(v).get(cvi).put(ncvi, change);
            insertionObjChange[request] = objChange;
        }

        public void updateCapaLeftInRoute(int v, int start) {
            assert(isVehicle(v));
            int begin = getBeginDepot(v); // beginDepot
            int end = getEndDepot(v);
            if (start == -1) {
                start = succ[begin].value();
            }
            // assert(capaLeftInRoute[begin].value() == darp.vehicleCapacity);
            // assert(capaLeftInRoute[end].value() == darp.vehicleCapacity);
            int capacity = capaLeftInRoute[pred[start].value()].value();

            // assert (succ[start].value() != start);
            // assert (pred[start].value() != start);

            while (start != end) {
                //assert(isPickup(start) || isDelivery(start));
                capacity -= vertexLoadChange[start];
                capaLeftInRoute[start].setValue(capacity);
                start = succ[start].value();
            }
            // assert(capacity >= darp.vehicleCapacity-1);
        }

        // insert vertex i into route of j, just before j
        public boolean insertVertexIntoRoute(int i, int j) {
            assert(servingVehicle[j].isBound());
            assert(!isEnd(i));
            assert(!isEnd(pred[j].value()));
            // if time constraints do not allow to place i just before j
            if (!tryPost(lessOrEqual(getArrivalTime(i, j), servingTime[j]))) return false;
            // if time constraints do not allow to place previous pred of j before i (insertion)
            if (!tryPost(lessOrEqual(getArrivalTime(pred[j].value(), i), servingTime[i]))) return false;
            succ[i].setValue(j);
            pred[i].setValue(pred[j].value());
            succ[pred[i].value()].setValue(i);
            pred[j].setValue(i);
            return true;
        }

        /** utilitary functions */

        // arrival time variable if succi is visited just after i in the same route as the one of i
        public IntVar getArrivalTime(int i, int succi) {
            assert(!isEnd(i));
            int dist = travelTimeMatrix[i][succi];
            if (isStart(i)) {
                return plus(servingTime[i],dist);
            } else {
                return plus(servingTime[i],darp.stops[i].servingDuration()+dist);
            }
        }

        public int getArrivalTimeValue(int i, int succi, boolean getMin) {
            /*
            if (getMin)
                return getArrivalTime(i, succi).min();
            else
                return getArrivalTime(i, succi).max();

             */
            if (isStart(i)) {
                if (getMin)
                    return servingTime[i].min() + travelTimeMatrix[i][succi];
                return servingTime[i].max() + travelTimeMatrix[i][succi];
            } else {
                if (getMin)
                    return servingTime[i].min() + servingDuration[i] + travelTimeMatrix[i][succi];
                return servingTime[i].max() + servingDuration[i] + travelTimeMatrix[i][succi];

            }
        }

        public boolean tryPost(Constraint c) {
            try {
                cp.post(c);
            } catch(InconsistencyException e) {
                return false;
            }
            return true;
        }

        int getCorrespondingRequest(int i) {
            if (i < darp.nRequests) return i;
            else return  i - darp.nRequests;
        }

        int getCorrespondingPickup(int i ) {
            return i - darp.nRequests;
        }

        int getCorrespondingDelivery(int i) {
            return i + darp.nRequests;
        }

        int getCriticalVertex(int request) {
            DARPInstance.Stop r = darp.stops[request];
            if (r.twStart() > 0 || r.twEnd() < darp.timeHorizon) return request;
            else return  request + darp.nRequests;
        }

        int getCorrespondingVertex(int i) {
            if (isPickup(i)) {
                return getCorrespondingDelivery(i);
            } else return getCorrespondingPickup(i);
        }

        int getBeginDepot(int v) {
            return rangeStartDepotMin + v;
        }

        int getEndDepot(int v) {
            return rangeEndDepotMin + v;
        }

        boolean isPickup(int i) {
            return rangePickupMin <= i && i < rangePickupMax;
        }

        boolean isDelivery(int i) {
            return rangeDeliveryMin <= i && i < rangeDeliveryMax;
        }

        boolean isStart(int i) {
            return rangeStartDepotMin <= i && i < rangeStartDepotMax;
        }

        boolean isEnd(int i) {
            return rangeEndDepotMin <= i && i < rangeEndDepotMax;
        }

        boolean isVehicle(int i) {
            return 0 <= i && i < darp.nVehicles;
        }

        String currentRoutes() {
            StringBuilder routes = new StringBuilder();
            for (int begin = rangeStartDepotMin; begin < rangeStartDepotMax; ++begin) {
                int current = succ[begin].value();
                routes.append(String.format("%2d", begin));
                while (current != begin) {
                    routes.append(" -> ").append(String.format("%2d", current));
                    current = succ[current].value();
                }
                routes.append('\n');
            }
            return routes.toString();
        }

        void printCurrentRoutes() {
            int nodes_visited = 0;
            for (int begin = rangeStartDepotMin; begin < rangeStartDepotMax; ++begin) {
                ++nodes_visited;
                int end = begin + darp.nVehicles;
                int current = succ[begin].value();
                StringBuilder road = new StringBuilder(String.format("%2d", begin));
                while (current != begin) {
                    ++nodes_visited;
                    road.append(" -> ").append(String.format("%2d", current));
                    current = succ[current].value();
                }
                System.out.println(road);
            }
            System.out.println(nodes_visited + " nodes visited / " + this.numVars);
            System.out.println("----------");
        }

        private void printTimeVars() {
            for (int i = 0 ; i < numVars; ++i) {
                System.out.printf("node %d : [%d, %d]\n", i, servingTime[i].min(), servingTime[i].max());
            }
        }

    }

    public static void main(String[] args) {
        //DARPInstance instance = new DARPInstance("data/darp/simpleInstance2.txt");
        //DARPInstance instance = new DARPInstance("data/darp/Cordeau/a2-16.txt");
        //DARPInstance instance = new DARPInstance("data/darp/Cordeau2003/a3-24.txt");
        DARPInstance instance = new DARPInstance("data/darp/Cordeau2003/a13-144.txt");
        DARPLnsFfpa solver = new DARPLnsFfpa(instance);
        //solver.setVerbosity(1);
        DARPSolveStatistics solveStats = new DARPSolveStatistics(120);

        solver.solveAll(instance, solveStats);
        System.out.println(solveStats);
    }

}
