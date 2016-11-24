package minicp.examples.darp;

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.state.StateManager;
import minicp.state.StateSparseSet;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;
import minicp.util.io.InputReader;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.IntStream;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;

public class DARPParser {

    final static int SCALING = 100;

    public record Coordinate(double x, double y) {

        public double euclideanDistance(Coordinate o) {
            double dx = (x - o.x) * (x - o.x);
            double dy = (y - o.y) * (y - o.y);
            double val = Math.sqrt(dx + dy) * SCALING;
            return val;
        }

    }

    public record Stop(Coordinate coord, int servingDuration, int loadChange, int twStart, int twEnd) {
    }

    public record DarpInsertion(int request, int vehicle, int cvi, int ncvi, int change) implements Comparable<DarpInsertion> {
        public int compareTo(DarpInsertion other) {
            return this.change - other.change;
        }
    }

    public record DarpSolution(int[] succ, int[] pred, int[] servingVehicle, int objective) {}

    public static class DarpInstance {

        final int SCALING = DARPParser.SCALING;
        int nVehicles;
        int nRequests;
        int timeHorizon;
        int vehicleCapacity;
        int maxRideTime;  //Max time in vehicle for request (service excluded)

        // depot start /end data
        Stop startDepot;
        Stop endDepot;
        Stop [] stops;

        private Stop readLine(InputReader reader) {
            // depot start
            reader.getInt();
            return new Stop(new Coordinate(reader.getDouble(),reader.getDouble()), // y
                    reader.getInt() * SCALING, // serving dur
                    reader.getInt(), // load change
                    reader.getInt() * SCALING, // tw start
                    reader.getInt() * SCALING); // tw end
        }

        public DarpInstance(String file) {

            InputReader reader = new InputReader(file);
            nVehicles = reader.getInt();
            nRequests = reader.getInt();
            timeHorizon = reader.getInt() * SCALING;
            vehicleCapacity = reader.getInt();
            maxRideTime = reader.getInt() * SCALING;

            stops = new Stop [2*nRequests] ;

            // depot start
            startDepot = readLine(reader);

            // regular pickup/delivery requests
            for (int i = 0; i < 2*nRequests; i++) {
                stops[i] = readLine(reader);
            }

            // depot end
            endDepot = readLine(reader);
        }
    }

    static class DarpCP {
        // Variables related to modelling
        IntVar [] servingTime;
        IntVar [] servingVehicle;

        Random random = new Random(42);
        public double probUpdateSol = 0.07;

        StateInt[] succ;
        StateInt[] pred;
        StateInt[] capaLeftInRoute;

        //final int SCALING = 100;
        final int ALPHA = 80;

        int [] twStart, twEnd, vertexLoadChange;

        int [][] travelTimeMatrix;
        int numVars;

        Solver cp = makeSolver();
        DarpSolution bestSolution;
        int bestSolutionObjective = Integer.MAX_VALUE;
        DarpSolution currentSolution; // used for diversification in the LNS-FFPA
        int currentSolutionObjective = Integer.MAX_VALUE;

        // array of [requests] -> {vehicle -> {critical node -> {non critical node -> objective value change }}}
        // State<HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>>[] insertionObjChange;

        HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>>[] insertionObjChange;
        // array of [request]. Each request contains one entry {vehicle -> {critical node -> {non critical node -> objective value change }}}
        // StateMap<Integer, StateMap<Integer, StateMap<Integer, Integer>>>[] insertionObjChange;

        int rangeRequestMin, rangeRequestMax;
        int rangePickupMin, rangePickupMax;
        int rangeDeliveryMin, rangeDeliveryMax;
        int rangeDepotMin, rangeDepotMax;
        int rangeStartDepotMin, rangeStartDepotMax;
        int rangeEndDepotMin, rangeEndDepotMax;

        StateSparseSet customersLeft; // remaining requests that must be processed

        DarpInstance darp;

        public DarpCP(DarpInstance darpInstance) {
            this.darp = darpInstance;
            numVars = darpInstance.nRequests * 2 + 2 * darpInstance.nVehicles;

            rangeRequestMin = 0;
            rangeRequestMax = darp.nRequests;

            rangePickupMin = 0;
            rangePickupMax = darp.nRequests;
            rangeDeliveryMin = rangePickupMax;
            rangeDeliveryMax = 2*darp.nRequests;
            rangeDepotMin = rangeDeliveryMax;
            rangeDepotMax = 2*darp.nRequests + darp.nVehicles * 2;

            rangeStartDepotMin = rangeDeliveryMax;
            rangeStartDepotMax = rangeDeliveryMax + darp.nVehicles;
            rangeEndDepotMin = rangeStartDepotMax;
            rangeEndDepotMax = rangeEndDepotMin + darp.nVehicles;

            // travelTimeMatrix and vertexLoadChange

            travelTimeMatrix = new int[numVars][numVars];
            Coordinate [] coords = new Coordinate[numVars];
            vertexLoadChange = new int[numVars];
            for (int i = 0; i < 2*darp.nRequests; i++) {
                coords[i] = darp.stops[i].coord;
                vertexLoadChange[i] = darp.stops[i].loadChange;
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                coords[i] = darp.startDepot.coord;
                vertexLoadChange[i] = 0;
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                coords[i] = darp.endDepot.coord;
                vertexLoadChange[i] = 0;
            }

            for (int i = 0; i < numVars; i++) {
                for (int j = i+1; j < numVars; j++) {
                    travelTimeMatrix[i][j] = (int) Math.round(coords[i].euclideanDistance(coords[j]));// * this.darp.SCALING;
                    travelTimeMatrix[j][i] = (int) Math.round(coords[j].euclideanDistance(coords[i]));// * this.darp.SCALING;
                }
            }

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
            // hashmap initialization
            //insertionObjChange = new State[darp.nRequests];
            //for (int i = 0; i < darp.nRequests; i++) {
            //    insertionObjChange[i] = cp.getStateManager().makeStateRef(new HashMap<>());
            //}
            insertionObjChange = new HashMap[darp.nRequests];
            for (int i=0; i < darp.nRequests ; ++i)
                insertionObjChange[i] = new HashMap<>();

            // cp constraints

            // serving time variable creation with time windows
            //System.out.println("posting serving time ...");
            servingTime = new IntVar[numVars];
            for (int i = 0; i < 2*darp.nRequests; i++) {
                //servingTime[i] = makeIntVar(cp,darp.stops[i].twStart * SCALING, darp.stops[i].twEnd * SCALING);
                servingTime[i] = makeIntVar(cp,darp.stops[i].twStart, darp.stops[i].twEnd);
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                //servingTime[i] = makeIntVar(cp,darp.startDepot.twStart * SCALING, darp.startDepot.twEnd * SCALING);
                servingTime[i] = makeIntVar(cp,darp.startDepot.twStart, darp.startDepot.twEnd);
            }
            for (int i = rangeEndDepotMin; i < rangeEndDepotMax; i++) {
                //servingTime[i] = makeIntVar(cp,darp.endDepot.twStart * SCALING, darp.endDepot.twEnd * SCALING);
                servingTime[i] = makeIntVar(cp,darp.endDepot.twStart, darp.endDepot.twEnd);
            }

            // serving vehicles
            //System.out.println("posting serving vehicles ...");
            servingVehicle = new IntVar[numVars];

            for (int i = 0; i < 2*darp.nRequests; i++) {
                servingVehicle[i] = makeIntVar(cp,darp.nVehicles);
            }
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                servingVehicle[i] = makeIntVar(cp,i-rangeDepotMin, i-rangeDepotMin);
                servingVehicle[i + darp.nVehicles] = makeIntVar(cp, i -rangeDepotMin, i -rangeDepotMin);
            }

            // post precedences
            //System.out.println("posting precedences ...");
            // serving time pickup + serving + travel time to delivery <= serving time delivery
            for (int i = 0; i < darp.nRequests; i++) {
                int servingDuration = darp.stops[i].servingDuration;
                int timeToDelivery = travelTimeMatrix[i][i+darp.nRequests];
                cp.post(lessOrEqual(plus(servingTime[i],servingDuration + timeToDelivery),
                        servingTime[i+darp.nRequests]));
            }

            // serving time end depot i - serving time start depot i <= timeHorizon
            //System.out.println("posting horizon ...");
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax; i++) {
                cp.post(lessOrEqual(sum(servingTime[i+darp.nVehicles], minus(servingTime[i])),
                        darp.timeHorizon));
            }

            // post max ride times
            //System.out.println("posting max ride times ...");
            for (int i = 0; i < darp.nRequests; i++) {
                // cp.post(lessOrEqual(sum(servingTime[i+darp.nRequests],minus(servingTime[i])),darp.maxRideTime * SCALING));
                int servingDuration = darp.stops[i].servingDuration;
                cp.post(lessOrEqual(sum(servingTime[i+darp.nRequests], minus(plus(servingTime[i], servingDuration)) ),
                        darp.maxRideTime));
            }

            // post same vehicle for pickup and delivery
            //System.out.println("posting vehicle consistency ...");
            for (int i = 0; i < darp.nRequests; i++) {
                cp.post(equal(servingVehicle[i], servingVehicle[i+darp.nRequests]));
            }

            customersLeft = new StateSparseSet(cp.getStateManager(),darp.nRequests,0);
        }

        // insert vertex i into route of j, just before j
        public boolean insertVertexIntoRoute(int i, int j) {
            assert(servingVehicle[j].isBound());
            assert(!isEnd(i));
            assert(!isEnd(pred[j].value()));
            // if time constraints do not allow to place i just before j
            if (!tryPost(lessOrEqual(getArrivalTime(i, j),servingTime[j]))) return false;
            // if time constraints do not allow to place previous pred of j before i (insertion)
            if (!tryPost(lessOrEqual(getArrivalTime(pred[j].value(), i), servingTime[i]))) return false;
            succ[i].setValue(j);
            pred[i].setValue(pred[j].value());
            succ[pred[i].value()].setValue(i);
            pred[j].setValue(i);
            return true;
        }

        // arrival time variable if succi is visited just after i in the same route as the one of i
        public IntVar getArrivalTime(int i, int succi) {
            assert(!isEnd(i));
            int dist = travelTimeMatrix[i][succi];
            if (isStart(i)) {
                return plus(servingTime[i],dist);
            } else {
                return plus(servingTime[i],darp.stops[i].servingDuration+dist);
            }
        }

        public int getArrivalTimeValue(int i, int succi, boolean getMin) {
            if (getMin)
                return getArrivalTime(i, succi).min();
            else
                return getArrivalTime(i, succi).max();
        }

        public boolean tryPost(Constraint c) {
            try {
                cp.post(c);
            } catch(InconsistencyException e) {
                return false;
            }
            return true;
        }

        public boolean solve() {
            // construct the dictionary
            for (int v = 0; v < this.darp.nVehicles; ++v)
                for (int i = 0; i < this.darp.nRequests; ++i)
                    setInsertionCost(i, v);
            DFSearch search = makeDfs(this.cp, this::treeSearch);
            search.onSolution(() -> {
                DarpSolution solution = new DarpSolution(stateIntArrayToInt(succ), stateIntArrayToInt(pred), intVarArrayToInt(servingVehicle), getObjectiveValue());
                // System.out.println("solution found with value " + solution.objective);
                // updateSolutionFfpa(solution);
                // printCurrentRoutes();
            });
            search.onFailure(() -> {
                // System.out.println("Cannot find a solution");
            });

            double remainingTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9 + 20.;
            SearchStatistics stats = search.solveSubjectTo(searchStatistics -> {
                boolean criterion = searchStatistics.numberOfSolutions() > 0;
                boolean availableTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9 > remainingTime;
                if (criterion || availableTime)
                    return true;
                //printCurrentRoutes();
                return false;
            }, () -> {
               ;
            });
            //System.out.println(stats);
            return stats.numberOfSolutions() > 0;
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
            while (relaxEnd < nRelax && relaxEnd < darp.nRequests) {
                toRelax = relaxEnd + random.nextInt(darp.nRequests - relaxEnd);
                cRelaxed = possibleCustomers[toRelax];
                possibleCustomers[toRelax] = possibleCustomers[relaxEnd];
                possibleCustomers[relaxEnd] = cRelaxed;
                ++relaxEnd;
            }

            // set a new customersleft array of value and relax some values
            // customersLeft = new StateSparseSet(cp.getStateManager(),darp.nRequests,0);
            for (int i = 0; i < nRelax ; ++i) { // relaxed customers
                succ[i].setValue(i);
                pred[i].setValue(i);
                succ[i + darp.nRequests].setValue(i);
                pred[i + darp.nRequests].setValue(i);
            }
            for (int i = nRelax; i < darp.nRequests ; ++i) { //
                customersLeft.remove(possibleCustomers[i]);
            }
            int[] solSucc = currentSolution.succ;
            int[] solPred = currentSolution.pred;
            int[] solServingVehicle = currentSolution.servingVehicle;
            for (int v = 0; v < darp.nVehicles ;  ++v) {
                int begin = getBeginDepot(v);
                int end = getEndDepot(v);
                int current = solSucc[begin];
                int prev = begin;
                while (current != end) {
                    if (!customersLeft.contains(getCorrespondingRequest(current))) {
                        pred[current].setValue(prev);
                        succ[prev].setValue(current);
                        cp.post(lessOrEqual(getArrivalTime(prev, current), servingTime[current])); // setup of the serving time
                        cp.post(equal(servingVehicle[current], solServingVehicle[current])); // the vehicle goes through this node
                        prev = current; // only updated when a non-relaxed request is met, to complete the partial route
                    }
                    current = solSucc[current];
                }
                if (prev != end) {
                    pred[current].setValue(prev);
                    succ[prev].setValue(current);
                    cp.post(lessOrEqual(getArrivalTime(prev, end), servingTime[end]));
                }
                updateCapaLeftInRoute(v, -1);  // reset the capacity in the route
            }
        }

        public void lnsFfpaSolve() {
            double currentTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9;
            double timeMax = 5.33 * 60; // in seconds
            DFSearch search = makeDfs(this.cp, this::treeSearch);
            SearchStatistics stats;
            search.onSolution( () -> {
                DarpSolution solution = new DarpSolution(stateIntArrayToInt(succ), stateIntArrayToInt(pred), intVarArrayToInt(servingVehicle), getObjectiveValue());
                updateSolutionFfpa(solution);
            });
            stats = search.solveSubjectTo(searchStatistics -> searchStatistics.numberOfSolutions() > 0, () -> {}); // find first a feasible solution
            System.out.println(stats);
            if (stats.numberOfSolutions() > 0) {
                int range = 5;
                int numIters = 300;
                int failureLimit = 1000;
                int maxRange = darp.nRequests / 2 + range;
                for (int minNeighborhood = 2; minNeighborhood <= maxRange && currentTime < timeMax; ++minNeighborhood) {
                    if (minNeighborhood == maxRange)
                        minNeighborhood = 2; // reset of the neighborhood
                    for (int offsetNeighborhood = 0; offsetNeighborhood < range && currentTime < timeMax; ++offsetNeighborhood) {
                        for (int i = 0; i < numIters && currentTime < timeMax; ++i) {
                            int finalMinNeighborhood = minNeighborhood;
                            int finalOffsetNeighborhood = offsetNeighborhood;
                            stats = search.solveSubjectTo(searchStatistics -> searchStatistics.numberOfFailures() > failureLimit, () -> relax(finalMinNeighborhood + finalOffsetNeighborhood));
                            // System.out.println(stats);
                            currentTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9;
                        }
                    }
                }
            } else {
                System.out.println("no solution found");
            }
        }

        public void lnsSolve() {
            double currentTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9;
            double timeMax = 60.;
            DFSearch search = makeDfs(this.cp, this::treeSearch);
            search.onSolution( () -> {
                DarpSolution solution = new DarpSolution(stateIntArrayToInt(succ), stateIntArrayToInt(pred), intVarArrayToInt(servingVehicle), getObjectiveValue());
                updateSolution(solution);
            });
            SearchStatistics stats = search.solve(); // find first a feasible solution
            System.out.println(stats);
            if (stats.numberOfSolutions() > 0) {
                int range = 5;
                int numIters = 300;
                int failureLimit = 1000;
                int maxRange = darp.nRequests / 2 + range;
                for (int minNeighborhood = 2; minNeighborhood <= maxRange && currentTime < timeMax; ++minNeighborhood) {
                    if (minNeighborhood == maxRange)
                        minNeighborhood = 2; // reset of the neighborhood
                    for (int offsetNeighborhood = 0; offsetNeighborhood < range && currentTime < timeMax; ++offsetNeighborhood) {
                        for (int i = 0; i < numIters && currentTime < timeMax; ++i) {
                            int finalMinNeighborhood = minNeighborhood;
                            int finalOffsetNeighborhood = offsetNeighborhood;
                            search.solveSubjectTo(searchStatistics -> searchStatistics.numberOfFailures() > failureLimit, () -> relax(finalMinNeighborhood + finalOffsetNeighborhood));
                            currentTime = ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime() / 10E9;
                        }
                    }
                }
            } else {
                System.out.println("no solution found");
            }
        }

        public void updateSolution(DarpSolution solution) {
            if (solution.objective < currentSolutionObjective) { // only current solution is used here
                currentSolution = solution;
                currentSolutionObjective = solution.objective;
                System.out.println("solution found with value " + solution.objective);
                printCurrentRoutes();
            }
        }

        public void updateSolutionFfpa(DarpSolution solution) {
            double prob = random.nextDouble();
            if (solution.objective < currentSolutionObjective || prob < probUpdateSol) {
                currentSolution = solution;
                currentSolutionObjective = solution.objective;
                if (currentSolution.objective < bestSolutionObjective) {
                    bestSolution = currentSolution;
                    bestSolutionObjective = solution.objective;
                    System.out.println("current solution: " + solution.objective);
                    System.out.println("best solution: " + bestSolution.objective);
                    printCurrentRoutes();
                }
            }
        }

        public Procedure[] treeSearch() {
            if (customersLeft.size() == 0) {
                return EMPTY;
            }
            ArrayList<DarpInsertion> insertions = getUnassignedRequest();
            if (insertions.size() == 0) {
                throw new InconsistencyException(); // the renaining requests cannot be inserted
            }
            Procedure[] branching = new Procedure[insertions.size()];

            int i = 0;
            for (DarpInsertion insert : insertions) { // list of insertion points, sorted by the heuristic value
                branching[i] = () -> {
                    // the vehicle goes through the request
                    if (!tryPost(equal(servingVehicle[insert.request], servingVehicle[insert.cvi]))) {
                        //System.out.println("failed to assign a serving vehicle");
                        throw new InconsistencyException(); // vehicle assignment failed
                    }
                    // constraint for the critical vertex
                    int critical = getCriticalVertex(insert.request);
                    if (!insertVertexIntoRoute(critical, insert.cvi)) {
                        //System.out.println("failed to insert critical node");
                        throw new InconsistencyException(); // insertion failed
                    }
                    // constraint for the non critical vertex
                    if (!insertVertexIntoRoute(getCorrespondingVertex(critical), insert.ncvi)) {
                        //System.out.println("failed to insert non critical node");
                        throw new InconsistencyException(); // insertion failed
                    }
                    // update the capacity
                    updateCapaLeftInRoute(insert.vehicle, insert.request);
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
        public ArrayList<DarpInsertion> getUnassignedRequest() {
            int minVehicles   = Integer.MAX_VALUE;  // minimum number of vehicles found
            int minInsertions = Integer.MAX_VALUE;  // minimum number of insertion points found
            int maxObjChange  = Integer.MIN_VALUE;  // maximum increase in objective value found
            ArrayList<ArrayList<DarpInsertion>> insertions = new ArrayList<>(); // best insertions found

            // the request can be inserted in the fewest vehicles
            int nbRemaining = customersLeft.size();
            if (nbRemaining == 0) { // no more request to insert. Should be detected by the search before calling this function
                return null;
            }
            int[] remainingRequest = new int[nbRemaining];
            customersLeft.fillArray(remainingRequest);
            for (int request: remainingRequest) { // update insertionObjChange
                insertionObjChange[request] = new HashMap<>();
                for (int v = 0; v < darp.nVehicles ; ++v) {
                    setInsertionCost(request, v);
                }
            }

            for (int request : remainingRequest) {
                int nbVehicle = servingVehicle[request].size();
                int nbInsertions = getNumInsertionPoints(request);
                int objChange = Integer.MIN_VALUE;
                if (nbVehicle < minVehicles) {
                    // new min number of vehicles has been found
                    minVehicles = nbVehicle;
                    // add insertions option
                    insertions.clear();
                    insertions.add(getInsertionPoints(request, false));
                }
                else if (nbVehicle == minVehicles && nbInsertions < minInsertions) {
                    // min number of vehicles and less insertions possibles
                    minInsertions = nbInsertions;
                    // add insertions option
                    insertions.clear();
                    insertions.add(getInsertionPoints(request, false));
                }
                else if (nbVehicle == minVehicles && nbInsertions == minInsertions && objChange >= maxObjChange) {
                    // TODO best insertion point produces smallest amount in objective value? should reduce diversity, maybe not a good idea. Was absent from the comet code anyway
                    maxObjChange = objChange;
                    // add insertions option
                    insertions.add(getInsertionPoints(request, false));
                }
            }

            // sort by increasing order of heuristic
            ArrayList<DarpInsertion> chosen = insertions.get(random.nextInt(insertions.size()));
            Collections.sort(chosen);
            return chosen;
        }

        public ArrayList<DarpInsertion> getInsertionPoints(int request, boolean sorted) {
            // return an array of possible insertion points, sorted or not
            ArrayList<DarpInsertion> insertionPoints = new ArrayList<>();
            for (int v: insertionObjChange[request].keySet()) {
                for (int criticalNode : insertionObjChange[request].get(v).keySet()) {
                    for (int nonCriticalNode : insertionObjChange[request].get(v).get(criticalNode).keySet()) {
                        int change = insertionObjChange[request].get(v).get(criticalNode).get(nonCriticalNode);
                        insertionPoints.add(new DarpInsertion(request, v, criticalNode, nonCriticalNode, change));
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
            /*
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
            */
            while (start != end && servingTime[start].max() - darp.stops[cvv].servingDuration - travelTimeMatrix[cvv][start] < servingTime[cvv].min()) {
                start = succ[start].value();
            }
            while (start != begin &&  getArrivalTimeValue(pred[start].value(), cvv, true) <= servingTime[cvv].max()) {
                setBestServingTimeFail(request, v, start);
                if (start == end)
                    break;
                start = succ[start].value();
            }

        }

        public void setBestServingTimeFail(int request, int v, int start) {
            int begin = getBeginDepot(v);
            int end = getEndDepot(v);
            int cvv = getCriticalVertex(request);
            int ncv = getCorrespondingVertex(cvv);

            // lower bound on cvv serving time
            int cvvMinServingTime = Math.max(getArrivalTimeValue(pred[start].value(), cvv, true), darp.stops[cvv].twStart);
            // upper bound on cvv serving time
            int cvvMaxServingTime = Math.min(servingTime[start].max() - travelTimeMatrix[start][cvv] - darp.stops[cvv].servingDuration, darp.stops[cvv].twEnd);
            if (cvvMaxServingTime < cvvMinServingTime) // cvv cannot be inserted between pred[start] and start because of time constraints
                return;
            // change in heuristic value (slack) if cvv is inserted between pred[start] and start
            int changeCvv = servingTime[start].max() - (getArrivalTimeValue(pred[start].value(), cvv, true) + darp.stops[cvv].servingDuration + travelTimeMatrix[cvv][start]);
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
                while (current != begin) { // as long as every node remaining in the route is not tried
                    if (prev == cvv) { // first iteration: can we insert ncv right after cvv?
                        minRideTime = travelTimeMatrix[cvv][ncv];
                    } else { // visit some node before inserting ncv
                        minRideTime = servingTime[prev].min() + darp.stops[prev].servingDuration + travelTimeMatrix[prev][ncv] - (cvvMaxServingTime + darp.stops[cvv].servingDuration);
                    }
                    if (minRideTime > darp.maxRideTime) {
                        break; // not possible to reach ncv with this route. Stop trying additional nodes
                    }

                    ncvMinServingTime = Math.max(getArrivalTimeValue(prev, ncv, true), darp.stops[ncv].twStart);
                    ncvMaxServingTime = Math.min(servingTime[current].max() - travelTimeMatrix[current][ncv] - darp.stops[ncv].servingDuration, darp.stops[ncv].twEnd);
                    if (ncvMaxServingTime < ncvMinServingTime) { // impossible to reach ncv using this node. Trying the next node in the route
                        prev = current;
                        current = succ[current].value();
                        continue;
                    }

                    // change in heuristic value induced by the slack if ncv is inserted between pred[current] and current
                    int changeNcv = servingTime[current].max() - (getArrivalTimeValue(pred[current].value(), ncv, true) + darp.stops[ncv].servingDuration + travelTimeMatrix[ncv][current]);
                    if (changeNcv < 0) { // slack does not permit to insert the node. Trying the next node in the route
                        prev = current;
                        current = succ[current].value();
                        continue;
                    }

                    // change in heuristic value (rooting cost) if ncv is inserted between pred[current] and current
                    changeNcv -= ALPHA * (travelTimeMatrix[ncv][current] + travelTimeMatrix[pred[current].value()][ncv] - travelTimeMatrix[pred[current].value()][current]);
                    // update the cost of inserting cvv before start and ncv before current
                    addToInsertionObjChange(request, v, start, current, changeCvv + changeNcv);

                    if (capaLeftInRoute[current].value() < vertexLoadChange[cvv])  // cannot insert anymore because of capacity
                        break; // no need to keep trying on this path as it is invalid, next nodes will also invalidate the capacity

                    prev = current;
                    current = succ[current].value();
                }
            }
            else { // ncv must be placed before cvv. Follow path from predecessor to predecessor
                current = cvv;
                prev = pred[cvv].value();
                while (current != begin && (capaLeftInRoute[current].value() >= vertexLoadChange[ncv])) {
                    if (current == cvv) { // first insertion: can we place ncv just before cvv?
                        minRideTime = travelTimeMatrix[ncv][cvv];
                    } else {  // there is a least one node between ncv and cvv
                        minRideTime = cvvMinServingTime - (getArrivalTimeValue(prev, ncv, false) + darp.stops[ncv].servingDuration);
                    }
                    if (minRideTime > darp.maxRideTime) { // not possible to place ncv on this route. Stop trying additional nodes
                        break;
                    }

                    ncvMinServingTime = Math.max(getArrivalTimeValue(prev, ncv, true), darp.stops[ncv].twStart);
                    ncvMaxServingTime = Math.min(servingTime[current].max() - travelTimeMatrix[current][ncv] - darp.stops[ncv].servingDuration, darp.stops[ncv].twEnd);
                    if (ncvMaxServingTime < ncvMinServingTime) {
                        current = prev;
                        prev = pred[prev].value();
                        continue;
                    }

                    // change in heuristic value induced by the slack if ncv is inserted between pred[current] and current
                    int changeNcv = servingTime[current].max() - (getArrivalTimeValue(pred[current].value(), ncv, true) + darp.stops[ncv].servingDuration + travelTimeMatrix[ncv][current]);
                    if (changeNcv < 0) {
                        current = prev;
                        prev = pred[prev].value();
                        continue;
                    }

                    // change in heuristic value induced by the rooting cost if ncv is inserted between pred[current] and current
                    changeNcv -= ALPHA * (travelTimeMatrix[ncv][current] + travelTimeMatrix[pred[current].value()][ncv] - travelTimeMatrix[pred[current].value()][current]);
                    // update the cost of inserting cvv before start and ncv before current
                    addToInsertionObjChange(request, v, start, current, changeCvv + changeNcv);

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
            //HashMap<Integer, HashMap<Integer, HashMap<Integer, Integer>>> objChange = insertionObjChange[request].value();
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

        int[] stateIntArrayToInt(StateInt[] states) {
            int[] values = new int[states.length];
            for (int i = 0; i < states.length ; ++i)
                values[i] = states[i].value();
            return values;
        }

        int[] intVarArrayToInt(IntVar[] intVars) {
            int[] values = new int[intVars.length];
            for (int i = 0; i < intVars.length ; ++i)
                values[i] = intVars[i].min(); // assume that the array is bound
            return values;
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

        /**
         * objective value, expressed as the sum of travel time
         * @return
         */
        int getObjectiveValue() {
            int objective = 0;
            for (int i = rangeStartDepotMin; i < rangeStartDepotMax ; ++i) {
                int current = succ[i].value();
                int prev = i;
                int end = i + darp.nVehicles;
                while (prev != end) {
                    prev = current;
                    current = succ[current].value();
                    objective += travelTimeMatrix[prev][current];
                }
            }
            return objective;
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
            Stop r = darp.stops[request];
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
    }

    public static void main(String[] args) {
        DarpInstance instance = new DarpInstance("data/darp/Cordeau/a8-96.txt");
        DarpCP solver = new DarpCP(instance);
        solver.lnsFfpaSolve();
    }

}
