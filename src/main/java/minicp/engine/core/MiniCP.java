package minicp.engine.core;

import minicp.reversible.*;
import minicp.util.InconsistencyException;

import java.util.ArrayDeque;
import java.util.Queue;


public class MiniCP implements Solver {

    private TrailImpl trail = new TrailImpl();
    private Queue<Constraint> propagationQueue = new ArrayDeque<>();

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


    public Trail getTrail() {
        return trail;
    }

    public void fixPoint() {
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
    
    public void withNewState(Body body) {
        trail.push();
        body.call();
        trail.pop();
    }

}
