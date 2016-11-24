package minicp.examples.darp;

import minicp.engine.constraints.*;
import minicp.engine.core.IntVar;
import minicp.engine.core.SequenceVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static minicp.cp.Factory.*;
import static minicp.cp.BranchingScheme.firstFail;

/**
 * darp solver using a circuit constraint
 * aim at finding all solution for the instance
 */
public class DARPCircuit extends DARPSolver {

    @Override
    public DARPInstance.DARPSolution solve(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        return null;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        return null;
    }

    @Override
    public DARPInstance.DARPSolution solveLns(DARPInstance instance, DARPInstance.DARPSolution initSolution, DARPSolveStatistics darpSolveStatistics) {
        return null;
    }

    @Override
    public String description() {
        return "circuit";
    }

    @Override
    public void solveAll(DARPInstance instance, DARPSolveStatistics darpSolveStatistics) {
        DARPCircuit darpCircuit = new DARPCircuit(instance);
        DFSearch search = makeDfs(darpCircuit.cp, firstFail(darpCircuit.succ));
        search.onSolution(() -> {
            DARPInstance.DARPSolution solution = darpCircuit.constructSolution();
            System.out.println(solution);
            darpSolveStatistics.addSolution(solution);
        });
        SearchStatistics statistics = search.solve();
        darpSolveStatistics.setSearchStatistics(statistics);
    }

    public static IntVar elementVar(IntVar[] array, IntVar y) {
        Solver cp = y.getSolver();
        int min = IntStream.range(0, array.length).map(i -> array[i].min()).min().getAsInt();
        int max = IntStream.range(0, array.length).map(i -> array[i].max()).max().getAsInt();
        IntVar z = makeIntVar(cp, min,max);
        cp.post(new Element1DVar(array, y, z));
        return z;
    }

    IntVar[] succ;
    IntVar[] pred;
    IntVar[] time;
    IntVar[] load;
    Solver cp;
    DARPInstance instance;
    
    int numVars;
    int[] serviceDuration;
    int[][] dist;
    int[] loadChange;

    ArrayList<DARPInstance.Stop> stops;

    public DARPCircuit(DARPInstance instance) {
        this.instance = instance;
        cp = makeSolver();
        initInstanceVars();
        initCpVars();
        postConstraints();
    }

    public void initInstanceVars() {
        numVars = instance.nRequests * 2 + instance.nVehicles * 2;
        serviceDuration = new int[numVars];
        dist = new int[numVars][numVars];
        loadChange = new int[numVars];
        
        DARPInstance.Coordinate[] coords = new DARPInstance.Coordinate[numVars];
        for (int i = 0; i < 2* instance.nRequests; i++) {
            coords[i] = instance.stops[i].coord();
            loadChange[i] = instance.stops[i].loadChange();
            serviceDuration[i] = instance.stops[i].servingDuration();
        }
        for (int i = instance.nRequests * 2; i < instance.nRequests * 2 + instance.nVehicles; i++) {
            coords[i] = instance.startDepot.coord();
            loadChange[i] = 0;
            serviceDuration[i] = instance.startDepot.servingDuration();
        }
        for (int i = instance.nRequests * 2 + instance.nVehicles; i < numVars; i++) {
            coords[i] = instance.endDepot.coord();
            loadChange[i] = 0;
            serviceDuration[i] = instance.endDepot.servingDuration();
        }

        // travel time
        for (int i = 0; i < numVars; i++) {
            for (int j = i+1; j < numVars; j++) {
                dist[i][j] = (int) Math.round(coords[i].euclideanDistance(coords[j]));
                dist[j][i] = (int) Math.round(coords[j].euclideanDistance(coords[i]));
            }
        }

        // arraylist of stops used in the solutions. Indexing corresponds to the indexing used in this representation
        this.stops = new ArrayList<>(); // no need to specify end nodes, only pickup and drops
        stops.addAll(Arrays.asList(instance.stops).subList(0, 2 * instance.nRequests));
        
    }
    
    public void initCpVars() {
        succ = new IntVar[numVars];
        pred = new IntVar[numVars];
        time = new IntVar[numVars];
        load = new IntVar[numVars];
        // for stop nodes
        for (int i = 0; i < instance.nRequests * 2 ; ++i) {
            succ[i] = makeIntVar(cp, 0, numVars);
            pred[i] = makeIntVar(cp, 0, numVars);
            load[i] = makeIntVar(cp, 0, instance.vehicleCapacity);
            time[i] = makeIntVar(cp, instance.stops[i].twStart(), instance.stops[i].twEnd());
        }
        // for begin depot
        for (int i = instance.nRequests * 2; i < instance.nRequests * 2 + instance.nVehicles ; ++i) {
            succ[i] = makeIntVar(cp, 0, numVars);
            int predI = i == instance.nRequests * 2 ? numVars - 1 : i + instance.nVehicles - 1;
            pred[i] = makeIntVar(cp, predI, predI);
            load[i] = makeIntVar(cp, 0, 0);
            time[i] = makeIntVar(cp, instance.startDepot.twStart(), instance.startDepot.twEnd());
        }
        // for end depot
        for (int i = instance.nRequests * 2 + instance.nVehicles; i < numVars ; ++i) {
            int succI = i == numVars - 1 ? instance.nRequests * 2 : i - instance.nVehicles + 1;
            succ[i] = makeIntVar(cp, succI, succI);
            pred[i] = makeIntVar(cp, 0, numVars);
            load[i] = makeIntVar(cp, 0, 0);
            time[i] = makeIntVar(cp, instance.endDepot.twStart(), instance.endDepot.twEnd());
        }
    }

    public void postConstraints() {
        /* for all variables */
        cp.post(new Circuit(succ));
        cp.post(new Circuit(pred));
        // channeling between pred and succ vectors
        for (int i = 0; i < numVars; i++) {
            cp.post(equal(elementVar(succ,pred[i]),i));
            cp.post(equal(elementVar(pred,succ[i]),i));
        }

        /* for stop nodes only */
        for (int i = 0; i < instance.nRequests ; ++i) { // pickup before delivery
            cp.post(lessOrEqual(time[i], time[i + instance.nRequests]));
        }

        for (int i = 0; i < instance.nRequests ; ++i) { // max ride time
            IntVar val = sum(time[i + instance.nRequests], minus(plus(time[i], serviceDuration[i])));
            cp.post(lessOrEqual(val, instance.maxRideTime));
        }

        // time transition
        for (int i = 0 ; i < instance.nRequests * 2 ; ++i) {
            // time[pred[i]] + service[pred[i]] + dist[pred[i]][i] <= time[i]
            IntVar timePred = elementVar(time, pred[i]);
            IntVar servicePred = element(serviceDuration, pred[i]);
            IntVar distance = element(dist[i], pred[i]);
            cp.post(new LessOrEqual(sum(timePred, servicePred, distance), time[i]));
        }

        // load update
        for (int i = 0; i <  instance.nRequests * 2; i++) {
            // loadAt[i] = loadAt[[pred[i]] + loadChange[i]
            IntVar loadPred = elementVar(load, pred[i]);
            cp.post(equal(load[i], plus(loadPred, loadChange[i])));
        }

        /* for depots only */
        for (int i = instance.nRequests * 2; i < instance.nRequests *2 + instance.nVehicles ; ++i) {
            // time[end] - time[begin] <= maxRouteDuration
            cp.post(lessOrEqual(sum(time[instance.nVehicles + i], minus(time[i])), instance.timeHorizon));
            // time[begin] <= time[end]
            cp.post(lessOrEqual(time[i], time[i + instance.nVehicles]));
        }
    }

    public DARPInstance.DARPSolution constructSolution() {
        DARPInstance.DARPSolution solution = instance.constructSolution(stops);
        //printCurrentRoutes();
        return addSolutionRoute(solution);
    }

    private DARPInstance.DARPSolution addSolutionRoute(DARPInstance.DARPSolution initSolution) {
        for (int vehicle = 0; vehicle < instance.nVehicles; ++vehicle) {
            int current = succ[instance.nRequests * 2 + vehicle].min(); // depot start
            int end = instance.nRequests * 2 + instance.nVehicles + vehicle; // corresponding depot end
            while (current != end) {
                initSolution.addStop(vehicle, current);
                current = succ[current].min();
            }
        }
        return initSolution;
    }

    public static void main(String[] args) {
        DARPInstance instance = new DARPInstance("data/darp/simpleInstance2.txt");
        DARPCircuit solver = new DARPCircuit(instance);
        DARPSolveStatistics solveStats = new DARPSolveStatistics();
        solver.solveAll(instance, solveStats);
        System.out.println(solveStats);
    }
}
