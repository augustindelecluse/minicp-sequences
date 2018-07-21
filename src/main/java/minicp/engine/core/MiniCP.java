package minicp.engine.core;

import minicp.search.AbstractSearcher;
import minicp.search.SearchObserver;
import minicp.state.StateManager;
import minicp.state.StateStack;
import minicp.util.InconsistencyException;
import minicp.util.Procedure;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MiniCP implements Solver  {

    private Queue<Constraint> propagationQueue = new ArrayDeque<>();
    private List<Procedure> fixPointListeners = new LinkedList<Procedure>();

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
        if (c.isActive() && !c.isScheduled()) {
            c.setScheduled(true);
            propagationQueue.add(c);
        }
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
                Constraint c = propagationQueue.remove();
                c.setScheduled(false);
                if (c.isActive()) {
                    c.propagate();
                }
            }
        } catch (InconsistencyException e) {
            // empty the queue and unset the scheduled status
            while (!propagationQueue.isEmpty())
                propagationQueue.remove().setScheduled(false);
            throw e;
        }
    }

    public void post(Constraint c) {
        post(c,true);
    }

    public void post(Constraint c, boolean enforceFixPoint)  {
        c.post();
        if (enforceFixPoint) fixPoint();
    }
    public void post(BoolVar b) {
        b.assign(true);
        fixPoint();
    }    
}
