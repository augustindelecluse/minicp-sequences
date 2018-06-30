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
    private Vector<IntVar> vars      = new Vector<>(2);
    private Vector<Constraint> cstrs = new Vector<>(2);
    private ConstraintState[] cState;

    private void makeState(Constraint c) {
        if (cState == null) {
            cState = new ConstraintState[cstrs.size()];
            for (int i = 0; i < cstrs.size(); i++)
                cState[i] = new ConstraintState(this, cstrs.get(i));
        }
        if (c.getId() >= cState.length) {
            ConstraintState[] cs = new ConstraintState[cstrs.size()];
            System.arraycopy(cState, 0, cs, 0, cState.length);
            for (int i = cState.length; i < cs.length; i++)
                cs[i] = new ConstraintState(this, cstrs.get(i));
            cState = cs;
        }
    }

    public void schedule(Constraint c) {
        makeState(c);
        int x = c.getId();
        if (cState[x].canSchedule()) {
            cState[x].scheduled = true;
            propagationQueue.add(c);
        }
    }
    private void propagate(Constraint c) {
        makeState(c);
        int x = c.getId();
        cState[x].scheduled = false;
        if (cState[x].isActive())
            c.propagate();
    }
    private void clear(Constraint c) {
        int x = c.getId();
        cState[x].scheduled = false;
    }
    public void deactivate(Constraint c) {
        int x = c.getId();
        cState[x].deactivate();
    }

    public int registerVar(IntVar x) { vars.add(x);return vars.size()-1;}
    public int registerConstraint(Constraint c) { cstrs.add(c);return cstrs.size()-1;}
    public Trail getTrail() { return trail;}

    public void fixPoint() {
        try {
            while (propagationQueue.size() > 0)
                propagate(propagationQueue.remove());
        } catch (InconsistencyException e) {
            // empty the queue and unset the scheduled status
            while (propagationQueue.size() > 0)
                clear(propagationQueue.remove());
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
