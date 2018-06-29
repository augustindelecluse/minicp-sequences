package minicp.engine.core;

import minicp.reversible.Trail;
import minicp.reversible.TrailImpl;
import minicp.util.InconsistencyException;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Vector;

public class MiniCP implements Solver {
    private Trail trail = new TrailImpl();
    private Queue<Constraint> propagationQueue = new ArrayDeque<>();
    private Vector<IntVar> vars = new Vector<>(2);
    public void registerVar(IntVar x) {
        vars.add(x);
    }

    public void push() { trail.push();}
    public void pop()  { trail.pop();}

    public Trail getTrail() { return trail;}

    public void schedule(Constraint c) {
        propagationQueue.add(c);
    }

    public void fixPoint() {
        try {
            while (propagationQueue.size() > 0)
                propagationQueue.remove().process();
        } catch (InconsistencyException e) {
            // empty the queue and unset the scheduled status
            while (propagationQueue.size() > 0)
                propagationQueue.remove().discard();
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
