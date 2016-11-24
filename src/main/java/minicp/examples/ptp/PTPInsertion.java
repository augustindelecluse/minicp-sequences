package minicp.examples.ptp;

import minicp.engine.constraints.LessOrEqual;
import minicp.engine.constraints.Sum;
import minicp.engine.constraints.sequence.*;
import minicp.engine.core.*;
import minicp.search.DFSearch;
import minicp.search.Objective;
import minicp.search.SearchStatistics;
import minicp.state.StateInt;
import minicp.state.StateSparseSet;
import minicp.util.Procedure;
import minicp.util.exception.InconsistencyException;

import java.util.*;

import static minicp.cp.BranchingScheme.*;
import static minicp.cp.Factory.*;
import static minicp.util.exception.InconsistencyException.INCONSISTENCY;

public class PTPInsertion {

    // TODO a pickup and a drop must belong to the same time window availability

    // instance data
    int verbosity = 0;
    int nNodes;             // 2 nodes per trip, 4 if there is a backward trip
    PTPInstance instance;   // instance to solve
    boolean sameVehicleBackward;
    //boolean mustServeAll; // true if all patients must be visited
    int[] duration;     // serving duration of each node
    int[] twStart;      // min starting time of each node
    int[] twEnd;        // max ending time of each node
    int[] loadChange;   // load change occurring at each node
    int[] maxCapacity;  // maximum capacity of each vehicle
    int[][] dist;       // distance between each node
    int[] patientId;    // id of the patient
    int[] minTransportTime;

    int[] patientRdv;
    int[] patientDur;
    int[] patientSrv;
    int[] patientRdvEnd;
    int[] patientLoad;

    int maxWaitTime;    // maximum waiting time in a vehicle for a patient
    int nTrips;         // number of trips that need to be served (1 or 2 per patient)
    int nPatients;      // number of patients
    int nTimeWindow;    // number of time window
    int nVehicle;
    int[] nodeToPlace;     // mapping of node to instance places
    int[] category;     // category of each trip
    //int[] vehicleMapping;   // mapping from time window to corresponding vehicle
    int[][] nodeOfPatient;  // nodes corresponding to a given patient
    int[] nodeToPatient; // node number to patient. -1 if the node if the node is not a patient

    // range for the nodes
    int firstPickup = 0;    // node of the first pickup
    int lastPickup;         // node of the last pickup + 1
    int firstDrop;          // node of the first drop
    int lastDrop;           // node of the last drop + 1
    int firstAvailability;  // node of the first time window availability
    int lastAvailability;   // node of the last time window availability
    int[][] Availability;   // node of each time window availability
    /*
     *  availability[v] = availability window for vehicle v
     *  availability[v][0] = start of first availability for vehicle v (its start node)
     *  availability[v][1] = end of first availability for vehicle v
     *  ...
     *  availability[v][n-1] = start of last availability for vehicle v
     *  availability[v][n] = end of last availability for vehicle v (its end node)
     */

    // decision variables
    Solver cp;          // solver
    IntVar[] time;      // arrival time of each node
    IntVar[] distance;  // traveled distance of each vehicle
    IntVar sumDistance; // traveled distance of all vehicles
    IntVar[] nServedPatients;   // number of served nodes per vehicle
    IntVar sumServedPatients; // total number of served nodes
    SequenceVar[] vehicles;     // 1 variable per vehicle
    Objective objective;

    // used for the search
    StateSparseSet patientsLeft; // patients that must still be served
    StateInt lastPatientInserted; // patient currently being inserted. -1 means last patient fully inserted
    StateInt lastPatientIdxInserted; // last index for nodeOfPatient that has been inserted
    StateInt lastVehicle;
    int[] insertions;           // used to get the current insertions
    Integer[] branchingRange;
    int[] heuristicVal;
    StateSparseSet tripsLeft;   // used for the remaining trips
    Procedure[] branching;      // used to assign branching procedure
    Random random;
    int seed = 42;
    private long startTime; // start time in millis
    private long maxRunTime; // run time in millis

    // store the best found solution
    Set<Integer> patientsInBestSol = new HashSet<>(); // patients in the best solution
    int[][] bestRoute; // 1 per vehicle, store the id of the nodes that are serviced
    int[] nNodesInBestRoute; // number of nodes serviced in the best routes, including the beginning and ending nodes
    Set<Integer> nodesToRelax = new HashSet<>(); // store the patients that needs to be relaxed

    public PTPInsertion(PTPInstance instance) {
        sameVehicleBackward = instance.isSameVehicleBackward();
        this.instance = instance;
        cp = makeSolver();
        nTrips = instance.getNForwardTrips() + instance.getNBackwardTrips(); // number of trips
        nPatients = instance.getPatients().length;
        nVehicle = instance.getVehicles().length;
        nTimeWindow = 0; // number of time windows
        for (PTPInstance.Vehicle vehicle: instance.getVehicles())
            nTimeWindow += vehicle.availability().length;

        nNodes = nTrips * 2 + nTimeWindow * 2; // two nodes per trip and two nodes per time windows
        maxWaitTime = PTPInstance.toMinute(instance.getMaxWaitTime());
        lastPickup = nTrips;
        firstDrop = nTrips;
        lastDrop = nTrips * 2;
        random = new Random(seed);

        duration = new int[nNodes];
        loadChange = new int[nNodes];
        dist = new int[nNodes][nNodes];
        twStart = new int[nNodes];
        twEnd = new int[nNodes];
        nodeToPlace = new int[nNodes];
        //vehicleMapping = new int[nTimeWindow];
        branching = new Procedure[nPatients * nVehicle + 1];
        branchingRange = new Integer[nPatients * nVehicle + 1];
        heuristicVal = new int[nPatients * nVehicle + 1];

        insertions = new int[nNodes];

        patientRdv = new int[nPatients];
        patientDur = new int[nPatients];
        patientSrv = new int[nPatients];
        patientRdvEnd = new int[nPatients];
        patientLoad = new int[nPatients];
        patientId = new int[nPatients];
        int endTime = 24 * 60; // midnight

        nNodesInBestRoute = new int[nVehicle];
        bestRoute = new int[nVehicle][nNodes];
        nodeToPatient = new int[nNodes];
        Arrays.fill(nodeToPatient, -1);

        category = new int[nTrips];
        maxCapacity = new int[nVehicle];
        minTransportTime = new int[nTrips];
        nodeOfPatient = new int[instance.getPatients().length][];

        // offset used: minimum time window start for the vehicles
        int offset = Integer.MAX_VALUE;
        for (PTPInstance.Vehicle vehicle: instance.getVehicles()) {
            for (PTPInstance.TimeWindow tw: vehicle.availability()) {
                offset = Math.min(offset, PTPInstance.toMinute(tw.twStart()));
            }
        }
        instance.setOffset(offset);

        /*
         * nodes ordering:
         * - 0..nTrips*2-1: pickup and deliveries. node[i] is pickup for node[i+nTrips]
         *  - 0..nTrips-1: pickup
         *  - nTrips..nTrips*2-1: delivery
         * - nTrips*2..nNodes: start, intermediates and end nodes for vehicles
         *  - nTrips*2 + i: start node of vehicle i
         *  - nTrips*2+nTimeWindow(i) + i: end node of vehicle i
         *  - nTrips*2 + i + (j*2): begin of time window j of vehicle i
         *  - nTrips*2 + i + (j*2 + 1): end of time window j of vehicle i
         */

        int i = 0;
        int p = 0;
        for (PTPInstance.Patient patient: instance.getPatients()) {
            patientId[p] = patient.id();
            patientRdv[p] = PTPInstance.toMinute(patient.rdvTime());
            patientDur[p] = PTPInstance.toMinute(patient.rdvDuration());
            patientSrv[p] = PTPInstance.toMinute(patient.srvDuration());
            patientRdvEnd[p] = patientRdv[p] + patientDur[p];
            patientLoad[p] = patient.load();

            int nNodesPatients = (patient.start() != -1 ? 2 : 0) + (patient.end() != -1 ? 2 : 0);
            nodeOfPatient[p] = new int[nNodesPatients];
            int n = 0;
            //int rdvTime = PTPInstance.toMinute(patient.rdvTime());
            //int rdvDuration = PTPInstance.toMinute(patient.rdvDuration());
            if (patient.start() != -1) { // forward activity
                int dist = instance.dist(patient.start(), patient.dest());
                minTransportTime[i] = dist + patientSrv[p] * 2;
                duration[i] = patientSrv[p];
                category[i] = patient.category();
                nodeToPlace[i] = patient.start(); // start node
                loadChange[i] = patient.load();
                twStart[i] = Math.max(0, patientRdv[p] - maxWaitTime); // earliest departure
                twEnd[i] = patientRdv[p]; // latest departure to still arrive at rdvTime
                nodeToPatient[i] = p;


                duration[i + nTrips] = patientSrv[p];
                nodeToPlace[i + nTrips] = patient.dest(); // dest node
                loadChange[i + nTrips] = - patient.load();
                twStart[i + nTrips] = Math.max(0, patientRdv[p] - maxWaitTime);
                twEnd[i + nTrips] = patientRdv[p] - patientSrv[p]; // latest arrival time
                nodeToPatient[i + nTrips] = p;

                nodeOfPatient[p][n++] = i;
                nodeOfPatient[p][n++] = i +nTrips;
                ++i;
            }
            if (patient.end() != -1) { // backward activity
                int dist = instance.dist(patient.dest(), patient.end());
                minTransportTime[i] = dist + patientSrv[p] * 2;
                category[i] = patient.category();
                nodeToPlace[i] = patient.dest(); // dest node
                loadChange[i] = patient.load();
                twStart[i] = patientRdvEnd[p]; // earliest departure time
                twEnd[i] = Math.min(endTime, patientRdvEnd[p] + maxWaitTime); // latest departure time to still arrive at rdvTime + rdvDuration + maxWaitTime
                duration[i] = patientSrv[p];
                nodeToPatient[i] = p;

                nodeToPlace[i + nTrips] = patient.end(); // end node
                loadChange[i + nTrips] = - patient.load();
                twStart[i + nTrips] = patientRdvEnd[p]; // earliest arrival time
                twEnd[i + nTrips] = Math.min(endTime, patientRdvEnd[p] + maxWaitTime - patientSrv[p]); // latest arrival time
                duration[i + nTrips] = patientSrv[p];
                nodeToPatient[i + nTrips] = p;

                nodeOfPatient[p][n++] = i;
                nodeOfPatient[p][n++] = i +nTrips;
                ++i;
            }
            assert n == nodeOfPatient[p].length;
            ++p;
        }
        assert i == nTrips;
        assert p == nodeOfPatient.length;
        int v = 0;
        i = nTrips * 2;
        //int vehicle_idx = 0;
        Availability = new int[nVehicle][];
        for (PTPInstance.Vehicle vehicle: instance.getVehicles()) {
            maxCapacity[v] = vehicle.capacity();
            Availability[v] = new int[vehicle.availability().length * 2];
            int j = 0;
            for (PTPInstance.TimeWindow tw: vehicle.availability()) {
                nodeToPlace[i] = vehicle.start(); // start of time window
                loadChange[i] = 0;
                twStart[i] = PTPInstance.toMinute(tw.twStart());
                twEnd[i] = PTPInstance.toMinute(tw.twStart());
                Availability[v][j++] = i;
                ++i;

                nodeToPlace[i] = vehicle.end(); // end of time window
                loadChange[i] = 0;
                twStart[i] = PTPInstance.toMinute(tw.twEnd());
                twEnd[i] = PTPInstance.toMinute(tw.twEnd());
                Availability[v][j++] = i;
                //vehicleMapping[v] = vehicle_idx;
                ++i;
            }
            ++v;
            if (j != vehicle.availability().length * 2)
                throw new RuntimeException();
        }
        if (i != nNodes)
            throw new RuntimeException();
        if (v != nVehicle)
            throw new RuntimeException();
        for (i = 0 ; i < nNodes ; ++i)
            for (int j = 0 ; j < nNodes ; ++j)
                dist[i][j] = instance.dist(nodeToPlace[i], nodeToPlace[j]);
        int a = 0;
    }

    public void initCpVars() {
        patientsLeft = new StateSparseSet(cp.getStateManager(), nPatients, 0);
        lastPatientInserted = cp.getStateManager().makeStateInt(-1);
        lastPatientIdxInserted = cp.getStateManager().makeStateInt(0);
        lastVehicle = cp.getStateManager().makeStateInt(0);

        time = new IntVar[nNodes];
        distance = new IntVar[nVehicle];
        vehicles = new SequenceVar[nVehicle];
        tripsLeft = new StateSparseSet(cp.getStateManager(), nTrips, 0);
        int sumMaxDist = 0;
        for (int i = 0 ; i < nNodes; ++i) {
            time[i] = makeIntVar(cp, twStart[i], twEnd[i], true);
        }
        for (int v = 0; v < nVehicle; ++v) {
            int nWindow = Availability[v].length;
            int start = Availability[v][0];
            int end = Availability[v][nWindow-1];
            // TODO create with set of nodes
            vehicles[v] = new SequenceVarImpl(cp, nNodes, start, end);
            int maxDist = twEnd[end] - twStart[start];
            distance[v] = makeIntVar(cp, 0, maxDist);
            sumMaxDist += maxDist;
        }
        sumDistance = makeIntVar(cp, 0, sumMaxDist);
        nServedPatients = makeIntVarArray(cp, nVehicle, nPatients);
        sumServedPatients = makeIntVar(cp, 0, nPatients);
    }

    public void postConstraints() {
        //System.out.println("excluding unavailable nodes");
        // availability nodes for other vehicles are excluded
        for (int v1 = 0; v1 < nVehicle ; ++v1) {
            for (int v2 = 0 ; v2 < nVehicle ; ++v2) {
                if (v1 == v2)
                    continue;
                for (int node: Availability[v2]) { // the node belong to another vehicle
                    //System.out.println("excluding " + node + " for " + vehicles[v1]);
                    cp.post(new Exclude(vehicles[v1], node));
                }
            }
        }
        //System.out.println("scheduling intermediate depots");

        // a vehicle always visit the nodes in its time windows availabilities
        for (int v = 0 ; v < nVehicle ; ++v) {
            SequenceVar vehicle = vehicles[v];
            int pred = -1;
            for (int i = 0; i < Availability[v].length - 1; ++i) {
                int current = Availability[v][i];
                if (pred != -1) {
                    if (!vehicle.isScheduled(current)) { // if the node is not already assigned to the vehicle
                        cp.post(new Schedule(vehicle, current, pred));
                    }
                }
                pred = current;
            }
            for (int i = 1; i < Availability[v].length-1; i += 2) {
                int current = Availability[v][i];
                //System.out.println("vehicle" + vehicle + ": no edge from " + current);
                cp.post(new NoEdgeFrom(vehicle, current)); // no node cannot be inserted after the end node of a time window
            }
        }

        //System.out.println("exclude nodes from vehicles that cannot serve them");
        // exclude the nodes from vehicles that cannot serve them
        PTPInstance.Vehicle[] instanceVehicles = instance.getVehicles();
        for (int trip = 0; trip < nTrips; ++trip) {
            for (int v = 0 ; v < nVehicle ; ++v) {
                PTPInstance.Vehicle vehicle = instanceVehicles[v];
                if (!vehicle.canTake(category[trip])) {
                    cp.post(new Exclude(vehicles[v], trip)); // the vehicle cannot serve the node
                    cp.post(new Exclude(vehicles[v], trip + nTrips));
                }
            }
        }

        //System.out.println("vehicle usage for return trip");
        // vehicle usage for the return trip
        for (int[] ints : nodeOfPatient) {
            if (ints.length == 4) { // only use this constraint when a return trip is specified
                if (sameVehicleBackward) {
                    for (int v = 0; v < nVehicle; ++v) {
                        // the trip to hospital must occur before the trip back home
                        cp.post(new Precedence(vehicles[v], ints));
                    }
                } else {
                    // if the node related to a patient is serviced, all other nodes related to it must be serviced as well
                    cp.post(new Dependence1D(vehicles, ints));
                    // the trip to hospital must occur before the trip back home
                    cp.post(new LessOrEqual(time[ints[1]], time[ints[2]]));
                }
            }
        }

        //System.out.println("pickup before delivery");
        // pickup before delivery
        for (int i = 0 ; i < nTrips ; ++i) {
            // pickup occurs before delivery
            cp.post(lessOrEqual(time[i], time[i + nTrips]));
            // a patient cannot stay for too long in a vehicle
            //cp.post(lessOrEqual(sum(time[i + nTrips], minus(time[i])), maxWaitTime));
            //cp.post(lessOrEqual(plus(time[i], minTransportTime[i]), plus(time[i + nTrips], duration[i+nTrips])));
        }

        //System.out.println("time difference between forward and backward trip");
        // if there is a backward trip, the return trip occurs after time[arrivalHospital] + srvDuration
        for (int p = 0; p < nPatients; ++p) {
            if (nodeOfPatient[p].length == 4) { // return trip
                int arrivalHospital = nodeOfPatient[p][1];
                int departureHospital = nodeOfPatient[p][2];
                // time[arrival] + srvDuration + rdvDuration <= time[departure]
                cp.post(lessOrEqual(plus(time[arrivalHospital], patientDur[p] + patientSrv[p]),
                        time[departureHospital]));
            }
        }

        //System.out.println("time transition between nodes");
        // time transitions between trips
        int v = 0;
        for (SequenceVar vehicle: vehicles) {
            // serve the node in their correct time window
            cp.post(new TransitionTimes(vehicle, time, dist, duration));
            // serve the node in their correct time window and compute the distance traveled by the vehicle
            // cp.post(new TransitionTimes(vehicle, time, distance[v++], dist, duration));
        }

        //System.out.println("vehicle capacity");
        // a vehicle has a limited capacity
        int nActivities = nTrips + (nTimeWindow - nVehicle);
        /*
        a fake activity is created for time windows
        if there is more than 1 time window ([beginTw0, endTw0, beginTw1, endTw1])
        we construct an activity [endTw0 -> beginTw1] with a load change == maxCapacity
        this means that no activity might begin between [beginTw0 .. endTw0] and end between [beginTw1 .. endTw1]
         */
        int[] starts = new int[nActivities];
        int[] ends = new int[nActivities];
        int j = 0;
        for ( ; j < nTrips ; ++j) {
            starts[j] = j;
            ends[j] = j + nTrips;
        }
        for (v = 0 ; v < nVehicle ; ++v) {
            if (Availability[v].length > 2) { // the vehicle has more than one time window availability
                for (int i = 1; i < Availability[v].length - 1 ; i += 2) { // for each pair of two time window availability
                    int beginActivity = Availability[v][i]; // end of the last time window
                    int endActivity = Availability[v][i+1]; // begin of the next time window
                    starts[j] = beginActivity;
                    ends[j] = endActivity;
                    loadChange[beginActivity] = maxCapacity[v];
                    loadChange[endActivity] = -maxCapacity[v];
                    ++j;
                }
            }
        }
        if (j != nActivities)
            throw new RuntimeException();

        //int[] starts = IntStream.range(0, nTrips).toArray();
        //int[] ends = IntStream.range(nTrips, nTrips * 2).toArray();
        v = 0;
        for (SequenceVar vehicle: vehicles) {
            // respect the capacity in the vehicle
            cp.post(new Cumulative(vehicle, starts, ends, maxCapacity[v++], loadChange));
        }
        //System.out.println("disjoint");
        // a node can be served once and can be excluded from all vehicles if it cannot be served
        cp.post(new Disjoint(false, vehicles));
        // sumDistance is the sum of all distances
        //cp.post(new Sum(distance, sumDistance));
        // number of nodes served by each vehicle
        v = 0;
        int[] patients = new int[nodeOfPatient.length];
        for (int i = 0 ; i < patients.length; ++i) {
            patients[i] = nodeOfPatient[i][0];
        }
        //System.out.println("objective");
        for (SequenceVar vehicle: vehicles) {
            //cp.post(new NScheduled(vehicle, nServedPatients[v++]));
            cp.post(new Count(vehicle, nServedPatients[v++], patients));
        }
        // totalServedPatients is the sum of all served nodes
        cp.post(new Sum(nServedPatients, sumServedPatients));
    }

    public void setSeed(int seed) {
        this.seed = seed;
        random.setSeed(seed);
    }

    public int getSeed() {
        return this.seed;
    }

    public int getNBestPatientServiced() {
        return patientsInBestSol.size();
    }

    private boolean isFinished() {
        return System.currentTimeMillis() - startTime >= maxRunTime;
    }

    private boolean isRunning() {
        return !isFinished();
    }

    /**
     * solve a PTP using the provided maximum time
     * @param maxRunTime maximum run time in seconds
     */
    public void solve(long maxRunTime) {
        this.maxRunTime = maxRunTime * 1000; // convert to millis
        this.startTime = System.currentTimeMillis();
        solve();
    }

    public void solve() {
        initCpVars();
        //printPatientView();
        postConstraints();
        DFSearch search = makeDfs(cp, this::patientBranching);
        search.onSolution(() -> {
            registerBestSolution();
            toPTPSolution();
            //printCurrentRoutes();
            //printPatientsVehicle();
            //printPatientView();
            //printVehicleView();
            if (verbosity > 0) {
                System.out.println("patients served = " + sumServedPatients);
                System.out.println("---------------");
            }
            //System.exit(0);
        });
        //Objective objective = cp.minimize(sumDistance);
        objective = cp.maximize(sumServedPatients);
        SearchStatistics stats = search.optimize(objective, searchStatistics -> searchStatistics.numberOfSolutions() == 1 || isFinished());
        //SearchStatistics stats = search.optimize(objective);
        if (stats.isCompleted())
            System.exit(0);
        //SearchStatistics stats = search.solve(searchStatistics -> searchStatistics.numberOfSolutions() == 1 || isFinished());
        //System.out.println(stats);

        int range = 5;
        int numIters = 300;
        int failureLimitFinal = 50_000;
        int maxRange = nTrips / 2 - range;
        boolean running = isRunning();
        for (int minNeighborhood = 1; minNeighborhood <= maxRange && running; ++minNeighborhood) {
            if (minNeighborhood == maxRange)
                minNeighborhood = 1; // reset of the neighborhood
            for (int offsetNeighborhood = 0; offsetNeighborhood < range && running; ++offsetNeighborhood) {
                for (int i = 0; i < numIters && running; ++i) {
                    int nRelax = minNeighborhood + offsetNeighborhood;
                    stats = search.optimizeSubjectTo(objective,
                            searchStatistics -> (
                                    searchStatistics.numberOfSolutions() == 1 ||
                                            isFinished() ||
                                            searchStatistics.numberOfFailures() > failureLimitFinal),
                            () -> relax(nRelax));
                    //System.out.println(stats);
                    running = isRunning();
                }
            }
        }
        // write to json
        //toPTPSolution();
    }

    /**
     * insert one patient at a time. Once a node for a patient has been inserted, try to insert all other nodes related to it
     * until he has been served
     * @return branching
     */
    public Procedure[] patientBranching() {
        //printPatientsVehicle();
        int minInsert;
        boolean excludeFromAll = false;
        int patient;
        int nodeToInsert;
        if (lastPatientInserted.value() < 0) { // the last patient was fully inserted
            int size = patientsLeft.fillArray(insertions);
            if (size == 0) {
                return EMPTY;
            }
            minInsert = Integer.MAX_VALUE; // min number of insertions found
            int nInsert;
            int cnt = 0; // number of requests with a min number of insertions
            for (int i = 0; i < size; ++i) { // find the request with the min number of insertions
                nInsert = 0;
                patient = insertions[i];
                // compute the number of insertions points for the vehicle
                for (SequenceVar vehicle : vehicles) { // for all vehicle
                    int nInsertVehicleTrip0 = 1;
                    int nInsertVehicleTrip1 = 1;
                    for (int j = 0; j < nodeOfPatient[patient].length ; ++j) {
                        int node = nodeOfPatient[patient][j];
                        if (j < 2) { // trip 0
                            nInsertVehicleTrip0 *= vehicle.nScheduledInsertions(node);
                        } else { // trip 1
                            nInsertVehicleTrip1 *= vehicle.nScheduledInsertions(node);
                        }
                    }
                    if (sameVehicleBackward) {
                        nInsert += nInsertVehicleTrip0 * nInsertVehicleTrip1;
                    } else {
                        if (nodeOfPatient[patient].length == 2) // only one trip
                            nInsertVehicleTrip1 = 0;
                        nInsert += nInsertVehicleTrip0 + nInsertVehicleTrip1;
                    }
                }

                if (nInsert < minInsert && nInsert > 0) {
                    minInsert = nInsert;
                    insertions[0] = patient;
                    cnt = 1;
                } else if (nInsert == minInsert) {
                    insertions[cnt++] = patient;
                }
            }
            if (minInsert == 0 || minInsert == Integer.MAX_VALUE) {
                // the remaining patients cannot be included within the solution
                return new Procedure[] {
                        () -> {
                            for (SequenceVar vehicle: vehicles)
                                cp.post(new ExcludeAllPossible(vehicle));
                            patientsLeft.removeAll();
                        }
                };
            }

            patient = insertions[random.nextInt(cnt)];
            int finalPatient1 = patient;
            nodeToInsert = nodeOfPatient[patient][0];

            int i = 0;
            for (int v = 0 ; v < nVehicle ; ++v) {
                SequenceVar vehicle = vehicles[v];
                int finalVehicle = v;
                int n = vehicle.fillScheduledInsertions(nodeToInsert, insertions);
                for (int j = 0 ; j < n; ++j) {
                    int pred = insertions[j];
                    branchingRange[i] = i;
                    heuristicVal[i] = getHeuristicVal(vehicle, nodeToInsert, pred);
                    branching[i++] = () -> {
                        cp.post(new Schedule(vehicle, nodeToInsert, pred));
                        lastVehicle.setValue(finalVehicle);
                        lastPatientInserted.setValue(finalPatient1);
                        lastPatientIdxInserted.setValue(0);
                    };
                }
            }
            excludeFromAll = true; // last possibility is to exclude from all sequences
            minInsert = i;

        } else { // the last patient was not fully inserted
            // get the values for the last parameters
            int v = lastVehicle.value();
            patient = lastPatientInserted.value();
            nodeToInsert = nodeOfPatient[patient][lastPatientIdxInserted.value() + 1];
            boolean fullyInserted = lastPatientIdxInserted.value() + 2 == nodeOfPatient[patient].length;
            int finalPatient = patient;
            boolean insertInAllVehicles = sameVehicleBackward && lastPatientIdxInserted.value() == 1;

            int i = 0;
            for (int j = 0 ; j < nVehicle ; ++j) {
                SequenceVar vehicle;
                if (insertInAllVehicles) {
                    vehicle = vehicles[j]; // take some vehicle
                } else {
                    vehicle = vehicles[v]; // take the old vehicle
                }
                minInsert = vehicle.fillScheduledInsertions(nodeToInsert, insertions);

                for (int k = 0 ; k < minInsert; ++k) {
                    int pred = insertions[k];
                    if (fullyInserted) {
                        branchingRange[i] = i;
                        heuristicVal[i] = getHeuristicVal(vehicle, nodeToInsert, pred);
                        branching[i++] = () -> {
                            cp.post(new Schedule(vehicle, nodeToInsert, pred));
                            patientsLeft.remove(finalPatient);
                            lastPatientInserted.setValue(-1);
                        };
                    } else {
                        branchingRange[i] = i;
                        heuristicVal[i] = getHeuristicVal(vehicle, nodeToInsert, pred);
                        branching[i++] = () -> {
                            cp.post(new Schedule(vehicle, nodeToInsert, pred));
                            lastPatientIdxInserted.increment();
                        };
                    }
                }

                if (!insertInAllVehicles)
                    break;
            }

            minInsert = i;
            if (minInsert == 0) { // the nodes cannot be included
                return new Procedure[]{
                        () -> { throw INCONSISTENCY;}
                };
            }
        }

        Arrays.sort(branchingRange, 0, minInsert, Comparator.comparingInt(j -> heuristicVal[j]));
        // map the branching before to the branching after ordering
        Procedure[] branchingSorted = excludeFromAll ? new Procedure[minInsert+1] : new Procedure[minInsert];
        for (int i = 0 ; i < minInsert ; ++i)
            branchingSorted[i] = branching[branchingRange[i]];

        if (excludeFromAll) { // last branching option must be to exclude the patient from all vehicles
            int finalPatient2 = patient;
            // exclude from all vehicles
            branchingSorted[minInsert] = () -> {
                for (SequenceVar vehicle: vehicles) {
                    cp.post(new Exclude(vehicle, nodeToInsert), false);
                }
                cp.fixPoint();
                patientsLeft.remove(finalPatient2);
            };
        }
        return branchingSorted;


    }

    int getHeuristicVal(SequenceVar route, int node, int pred) {
        int succ = route.nextMember(pred);
        int slack = time[succ].max() - (time[pred].min() + duration[pred] + dist[pred][node] + duration[node] + dist[node][succ]);
        int objChange = dist[node][succ] + dist[pred][node] - dist[pred][succ];
        return 80 * objChange - 1 * slack;
    }

    public Procedure[] simpleBranching() {
        // select the route with the least number of possible nodes
        SequenceVar route = selectMin(vehicles,
                s -> !s.isBound(),
                SequenceVar::nPossibleNode);
        if (route == null)
            return EMPTY;
        else {
            // select the node to insert
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
            // branch on every scheduled insertion and removal of the node for this route
            Procedure[] branchingSorted;
            int finalNode = node;
            minInsert = route.fillScheduledInsertions(node, insertions);
            branchingSorted = new Procedure[minInsert + 1];

            for (int i = 0; i < minInsert; ++i) {
                int pred = insertions[i]; // insert the node into the route
                branchingSorted[i] = () -> {
                    //System.out.println("schedule " + finalNode + " into " + route);
                    cp.post(new Schedule(route, finalNode, pred));
                };
            }
            branchingSorted[minInsert] = () -> { // exclude the node from the route
                    //System.out.println("exclude " + finalNode + " from " + route);
                    cp.post(new Exclude(route, finalNode));
            };
            return branchingSorted;
        }
    }

    /**
     * relax nRelax from the current best found solution
     * @param nRelax number of patients to relax
     */
    public void relax(int nRelax) {
        //System.out.println("---------------");
        //printCurrentBestSol();
        //int[] possiblePatients = IntStream.range(0, nPatients).toArray();
        try {
            int[] possiblePatients = patientsInBestSol.stream().mapToInt(Number::intValue).toArray();
            int relaxEnd = 0;
            int toRelax;
            int pRelaxed;
            int maxPatientRelax = patientsInBestSol.size();
            nodesToRelax.clear();
            while (relaxEnd < nRelax && relaxEnd < maxPatientRelax) { // relax as many requests as asked
                toRelax = relaxEnd + random.nextInt(maxPatientRelax - relaxEnd);
                pRelaxed = possiblePatients[toRelax];
                possiblePatients[toRelax] = possiblePatients[relaxEnd];
                possiblePatients[relaxEnd] = pRelaxed;
                for (int n : nodeOfPatient[pRelaxed]) {
                    nodesToRelax.add(n);
                }
                ++relaxEnd;
            }
            // possiblePatients[0..relaxEnd-1] contains the relaxed customers
            for (int i = relaxEnd; i < possiblePatients.length; ++i)
                patientsLeft.remove(possiblePatients[i]);
            // assign the other patients to their previous best found value
            for (int v = 0; v < nVehicle; ++v) {
                SequenceVar vehicle = vehicles[v];
                int pred = vehicle.begin();
                //System.out.println(vehicle.toString());
                for (int i = 0; i < nNodesInBestRoute[v]; ++i) {
                    int current = bestRoute[v][i];
                    if (i != 0 && !nodesToRelax.contains(current)) {
                        //System.out.println("inserting " + current + " after " + pred);
                        cp.post(new Schedule(vehicle, current, pred));
                        pred = current;
                    }
                }
                //System.out.println(vehicle.toString());
            }
        } catch (InconsistencyException e) {
            //System.out.println("inconsistency");
            throw e;
        }
    }

    /**
     * iterate over the nodes serviced by a vehicle and assign them to the current solution
     */
    public void registerBestSolution() {
        patientsInBestSol.clear();
        // register the route for the vehicle
        for (int v = 0 ; v < nVehicle ; ++v) {
            nNodesInBestRoute[v] = vehicles[v].fillOrder(bestRoute[v], true);
            // register the patients serviced in the best solution
            for (int i = 1 ; i < nNodesInBestRoute[v]-1 ; ++i) {
                int node = bestRoute[v][i];
                int patient = nodeToPatient[node];
                if (patient != -1) {
                    patientsInBestSol.add(patient);
                }
            }
        }
    }

    /**
     * write the values of the best solution into a PTPSolution object
     * @return PTP solution corresponding to the best found solution
     */
    public PTPInstance.PTPSolution toPTPSolution() {
        PTPInstance.PTPSolution solution = instance.initializeSolution();
        // register the route for the vehicle
        for (int v = 0 ; v < nVehicle ; ++v) {
            ArrayList<PTPInstance.PTPStep> stepList = new ArrayList<>();
            // register the patients serviced in the best solution
            for (int i = 1 ; i < nNodesInBestRoute[v]-1 ; ++i) {
                int node = bestRoute[v][i];
                int patient = nodeToPatient[node];
                if (patient == -1) {
                    continue; // node that are not patient are not written to the solution file
                }
                int place = nodeToPlace[node];
                int patientId = this.patientId[patient];
                String time = PTPInstance.stringDuration(this.time[node].min());
                int operation = operationFromPatient(patient, node);
                if (instance.getPatients()[patient].start() == -1) // the operation is actually only a return trip
                    operation += 2;
                PTPInstance.PTPStep step = instance.step(place, time, patientId, operation);
                stepList.add(step);
            }
            solution.addVisit(v, stepList);
        }
        solution.toJson("data/ptp/solutions");
        return solution;
    }

    public void printCurrentBestSol() {
        for (int v= 0 ;v < nVehicle ; ++v) {
            for (int i = 0 ; i < nNodesInBestRoute[v] ; ++i) {
                System.out.printf("%d ", bestRoute[v][i]);
            }
            System.out.println();
        }
    }

    /**
     * print the list of nodes serviced by each vehicle
     */
    public void printCurrentRoutes() {
        int nodesVisited = 0;
        int v = 0;
        for (SequenceVar s: vehicles) {
            System.out.println("vehicle " + (v++) + ": "+ s.ordering(true, " "));
            nodesVisited += s.nScheduledNode(true);
        }
        System.out.printf("%d nodes visited / %d, patients served = %d / %d, traveled distance = %d\n",
                nodesVisited, nNodes, sumServedPatients.min(), nodeOfPatient.length, sumDistance.min());
        System.out.println("----------");
    }

    /**
     * print the list of patients and the vehicle that takes each of their node
     */
    public void printPatientsVehicle() {
        StringBuilder stringBuilder = new StringBuilder("vehicle id per patient");
        int patient = 0;
        for (int[] nodePatients: nodeOfPatient) {
            stringBuilder.append(String.format("\npatient %3d: ", patient++));
            for (int n: nodePatients) {
                boolean foundScheduled = false;
                int transportedIn = -1;
                int v = 0;
                for (SequenceVar s : vehicles) {
                    if (s.isScheduled(n)) {
                        transportedIn = v;
                        foundScheduled = true;
                        break;
                    }
                    v++;
                }
                if (foundScheduled) {
                    stringBuilder.append(String.format("%2d ", transportedIn));
                } else {
                    stringBuilder.append(" ? ");
                }
            }
        }
        System.out.println(stringBuilder.toString());
    }

    /**
     * print detailed information about the transportation of patients
     *  - their category
     *  - if they are served
     *  - which vehicle serviced them (and the category of the vehicle)
     *  - the time window of each node
     *  - the duration of their trip
     */
    public void printPatientView() {
        StringBuilder stringBuilder = new StringBuilder();
        String[] vehicleDesc = new String[nVehicle];
        int v = 0;
        for (PTPInstance.Vehicle vehicle: instance.getVehicles()) {
            vehicleDesc[v] = "vehicle " + v + " with cat " + Arrays.toString(vehicle.canTake());
            ++v;
        }
        int maxSize = -1;
        for (String vd: vehicleDesc) {
            maxSize = Math.max(vd.length(), maxSize);
        }
        for (int i = 0 ; i < nVehicle ; ++i) {
            int spaceAfter = maxSize - vehicleDesc[i].length();
            if (spaceAfter > 0)
                vehicleDesc[i] = vehicleDesc[i] + new String(new char[spaceAfter]).replace("\0", " ");
        }

        int patient = 0;
        for (int[] nodePatients: nodeOfPatient) {
            int cat = category[nodePatients[0]];
            stringBuilder.append(String.format("\npatient %3d (cat %d): ", patient++, cat));
            int i = 0;
            int trip_0_min_start = -1;
            int trip_0_max_start = -1;
            int trip_1_min_start = -1;
            int trip_1_max_start = -1;
            for (int n: nodePatients) {
                boolean foundScheduled = false;
                int transportedIn = -1;
                v = 0;
                for (SequenceVar s : vehicles) {
                    if (s.isScheduled(n)) {
                        transportedIn = v;
                        foundScheduled = true;
                        break;
                    }
                    v++;
                }
                if (i == 0) {
                    trip_0_min_start = time[n].min();
                    trip_0_max_start = time[n].max();
                } else if (i == 2) {
                    trip_1_min_start = time[n].min();
                    trip_1_max_start = time[n].max();
                }
                int place = nodeToPlace[n];
                String startOrEnd = i % 2 == 0 ? "start" : " end ";
                String trip_0_travel_time = String.format("Trip 0: Elapsed: [%5d..%5d]. Max: %d",
                        time[n].min() - trip_0_min_start, time[n].max() - trip_0_max_start, time[n].max() - trip_0_min_start);
                String trip_1_travel_time;
                if (i >= 2) {
                    trip_1_travel_time = String.format(". Trip 1: Elapsed: [%5d..%5d]. Max: %d",
                            time[n].min() - trip_1_min_start, time[n].max() - trip_1_max_start, time[n].max() - trip_1_min_start);
                } else {
                    trip_1_travel_time = "";
                }
                String visitedBy = foundScheduled ? String.format("visited by %s", vehicleDesc[transportedIn]) : "not serviced";
                stringBuilder.append(String.format("\n\tnode %3d, place=%2d (%s): %s at [%5d..%5d]. %s%s",
                        n, place, startOrEnd,
                        visitedBy, time[n].min(), time[n].max(), trip_0_travel_time, trip_1_travel_time));
                i++;
            }
        }
        System.out.println(stringBuilder);
        System.out.println("----------");
    }

    /**
     * print detailed information about the trip of a vehicle
     *  - its category
     *  - each node serviced
     *      - whose patient it is related to
     *      - the time of visit
     *      - the distance to the previous node
     *      - the load occurring at the node
     */
    public void printVehicleView() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int v = 0 ; v < nVehicle ; ++v) {
            SequenceVar vehicle = vehicles[v];
            stringBuilder.append(String.format("Vehicle %d. Max capacity = %d. Categories = ", v, maxCapacity[v]));
            stringBuilder.append(Arrays.toString(instance.getVehicles()[v].canTake()));
            int current = vehicle.begin();
            int[] twAvailability = Availability[v];
            int i = 0;
            int capacity = 0;
            while (current != vehicle.end()) {
                int id = nodeToPlace[current];
                capacity += loadChange[current];
                if (current == twAvailability[i]) { // node from time window availability
                    String timeWindowDescription = (i % 2 == 0 ? "(start) " : "( end ) ") + "of tw " + (i/2);
                    stringBuilder.append(String.format("\n\tnode %3d, id=%2d %s. Load = %d / %d. Time visit = [%5d..%5d] / [%s..%s]",
                            current, id, timeWindowDescription, capacity, maxCapacity[v],
                            time[current].min(), time[current].max(),
                            PTPInstance.stringDuration(time[current].min()), PTPInstance.stringDuration(time[current].max())));
                    ++i;
                } else {
                    char nodeType = current < firstDrop ? 'P' : 'D';
                    int patient = patientFromNode(current);
                    int trip = tripFromPatient(patient, current) + 1;
                    int nTripPatient = nodeOfPatient[patient].length / 2;
                    stringBuilder.append(String.format("\n\t\tnode %3d, id=%2d (%c. for patient %2d (cat %d) on trip %d/%d). Load = %d / %d. Time visit = [%5d..%5d] / [%s..%s]",
                            current, id, nodeType, patient, category[trip], trip, nTripPatient, capacity, maxCapacity[v],
                            time[current].min(), time[current].max(),
                            PTPInstance.stringDuration(time[current].min()), PTPInstance.stringDuration(time[current].max())));
                }
                current = vehicle.nextMember(current);
            }
            capacity += loadChange[current];
            String timeWindowDescription = "( end ) of tw " + (i/2);
            stringBuilder.append(String.format("\n\tnode %3d %s. Load = %d / %d. Time visit = [%5d..%5d]",
                    current, timeWindowDescription, capacity, maxCapacity[v],
                    time[current].min(), time[current].max()));
            ++i;
            stringBuilder.append("\n");
        }
        System.out.println(stringBuilder);
    }

    private int patientFromNode(int node) {
        for (int p = 0 ; p < nPatients ; ++p) {
            for (int n: nodeOfPatient[p]) {
                if (n == node)
                    return p;
            }
        }
        return -1;
    }

    /**
     * return the trip of the patient
     *  0 if forward trip
     *  1 if backward trip
     *  -1 otherwise
     * @param patient patient
     * @param node related node of patient
     * @return operation related to the node
     */
    private int tripFromPatient(int patient, int node) {
        int j = 0;
        for (int v: nodeOfPatient[patient]) {
            if (v == node)
                return j / 2;
            ++j;
        }
        return -1;
    }

    private int operationFromPatient(int patient, int node) {
        int j = 0;
        for (int v: nodeOfPatient[patient]) {
            if (v == node)
                return j;
            ++j;
        }
        return -1;
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    public static void main(String[] args) {
        //PTPInstance instance = new PTPInstance("data/ptp/easy/PTP-RAND-1_36_14_144.json");
        PTPInstance instance = new PTPInstance(args[0]);
        int maxRunTime = args.length > 1 ? Integer.parseInt(args[1]) : 300;
        PTPInsertion insertion = new PTPInsertion(instance);
        insertion.setVerbosity(1);
        insertion.solve(maxRunTime);
        System.out.println("patients serviced: " + insertion.getNBestPatientServiced());
    }
}
