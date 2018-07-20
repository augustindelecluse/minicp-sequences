package minicp.engine.core;

import minicp.state.StateBool;

public abstract class AbstractConstraint implements Constraint {
    final protected Solver cp;
    private boolean scheduled = false;
    private final StateBool active;

    public AbstractConstraint(Solver cp) {
        this.cp = cp;
        active = cp.getStateManager().makeStateBool(true);
    }
    public void post() {}
    public void propagate() {}
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }
    public boolean isScheduled() {
        return scheduled;
    }
    public void setActive(boolean active) {
        this.active.setValue(active);
    }
    public boolean isActive() {
        return active.getValue();
    }
}
