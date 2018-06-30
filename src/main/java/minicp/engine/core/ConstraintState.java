package minicp.engine.core;

import minicp.cp.Factory;
import minicp.reversible.RevBool;
import minicp.reversible.StateManager;

public class ConstraintState {
    boolean scheduled;
    private RevBool active;
    public ConstraintState(StateManager sm,Constraint c) {
        active = Factory.makeRevBool(sm,true);
        scheduled = false;
    }
    public boolean isActive() {
        return active.getValue();
    }
    public boolean canSchedule() {
        return !scheduled && active.getValue();
    }
    public void deactivate() {
        active.setValue(false);
    }
}
