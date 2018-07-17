package minicp.engine.core;

import minicp.reversible.*;
import minicp.search.AbstractSearchNode;
import minicp.search.DFSearch;
import minicp.util.InconsistencyException;
import minicp.util.Procedure;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MiniCP extends AbstractSearchNode implements Solver  {

    private Queue<Constraint> propagationQueue = new ArrayDeque<>();
    private List<Procedure> fixPointListeners = new LinkedList<Procedure>();

    private ReversibleStack<IntVar> vars = new ReversibleStack<>(this);

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
