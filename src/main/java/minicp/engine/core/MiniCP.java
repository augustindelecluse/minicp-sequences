package minicp.engine.core;

import minicp.cp.Factory;
import minicp.engine.constraints.Minimize;
import minicp.search.Objective;
import minicp.state.StateManager;
import minicp.state.StateStack;
import minicp.util.exception.InconsistencyException;
import minicp.util.Procedure;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MiniCP implements Solver {

    private Queue<Constraint> propagationQueue = new ArrayDeque<>();
    private List<Procedure> fixPointListeners = new LinkedList<>();

    private final StateManager sm;

    private final StateStack<IntVar> vars;

    public MiniCP(StateManager sm) {
        this.sm = sm;
        vars = new StateStack<>(sm);
    }

    public StateManager getStateManager() {
        return sm;
    }

    public void schedule(Constraint c) {
        c.schedule(propagationQueue);
        /*
        if (c.isActive() && !c.isScheduled()) {
            c.setScheduled(true);
            propagationQueue.add(c);
        }
        */
    }

    public int registerVar(IntVar x) {
        vars.push(x);
        return vars.size() - 1;
    }

    public void onFixPoint(Procedure listener) {
        fixPointListeners.add(listener);
    }

    private void notifyFixPoint() {
        fixPointListeners.forEach(s -> s.call());
    }

    public void fixPoint() {
        notifyFixPoint();
        try {
            while (!propagationQueue.isEmpty()) {
                propagationQueue.remove().process();
                /*
                Constraint c = propagationQueue.remove();
                c.setScheduled(false);
                if (c.isActive())
                    c.propagate();
                */
            }
        } catch (InconsistencyException e) {
            // empty the queue and unset the scheduled status
            while (!propagationQueue.isEmpty())
                propagationQueue.remove().setScheduled(false);
            throw e;
        }
    }

    public Objective minimize(IntVar x) {
        return new Minimize(x);
    }

    public Objective maximize(IntVar x) {
        return minimize(Factory.minus(x));
    }

    public void post(Constraint c) {
        post(c, true);
    }

    public void post(Constraint c, boolean enforceFixPoint) {
        c.post();
        if (enforceFixPoint) fixPoint();
    }

    public void post(BoolVar b) {
        b.assign(true);
        fixPoint();
    }
}
