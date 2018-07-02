package minicp.engine.core;

import minicp.cp.Factory;
import minicp.reversible.*;
import minicp.util.InconsistencyException;

import java.util.ArrayDeque;
import java.util.Queue;


public class MiniCP implements Solver {

    private Trail trail = new TrailImpl();
    private Queue<ConstraintState> propagationQueue = new ArrayDeque<>();
    private ReversibleStack<ConstraintState> constraints = new ReversibleStack<>(this);
    private ReversibleStack<IntVar> vars = new ReversibleStack<>(this);

    class ConstraintState {

        final Constraint c;
        boolean scheduled;
        RevBool active;

        public ConstraintState(Constraint c) {
            this.c = c;
            active = Factory.makeRevBool(MiniCP.this,true);
            scheduled = false;
        }

        public void propagate() {
            scheduled = false;
            if (active.getValue())
                c.propagate();
        }

        public boolean canSchedule() {
            return !scheduled && active.getValue();
        }

        public void deactivate() {
            active.setValue(false);
        }

    }




    public void schedule(Constraint c) {
        ConstraintState cs = constraints.get(c.getId());
        if (cs.canSchedule()) {
            cs.scheduled = true;
            propagationQueue.add(cs);
        }
    }

    private void clear(ConstraintState cs) {
        cs.scheduled = false;
    }

    public void deactivate(Constraint c) {
        constraints.get(c.getId()).deactivate();
    }

    public int registerVar(IntVar x) {
        vars.push(x);
        return vars.size() - 1;
    }

    public int registerConstraint(Constraint c) {
        constraints.push(new ConstraintState(c));
        return constraints.size() - 1;
    }

    public Trail getTrail() {
        return trail;
    }

    public void fixPoint() {
        try {
            while (propagationQueue.size() > 0)
                propagationQueue.remove().propagate();
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
