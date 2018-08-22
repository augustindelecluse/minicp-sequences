package minicp.state;


import minicp.util.Procedure;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class Trailer implements StateManager {

    class Backup extends Stack<StateEntry> {
        Backup() {}
        void restore() {
            for (StateEntry se : this)
                se.restore();
        }
    }

    private Stack<Backup> prior;
    private Backup current;
    private long magic = 0L;

    private List<Procedure> onRestoreListeners;

    public Trailer() {
        prior = new Stack<Backup>();
        current = new Backup();
        onRestoreListeners = new LinkedList<Procedure>();
    }

    private void notifyRestore() {
        for (Procedure l: onRestoreListeners) {
            l.call();
        }
    }

    @Override
    public void onRestore(Procedure listener) {
        onRestoreListeners.add(listener);
    }

    public long getMagic() { return magic;}

    public void pushState(StateEntry entry) {
        current.push(entry);
    }

    @Override
    public int getLevel() {
        return prior.size() - 1;
    }

    @Override
    public void saveState() {
        prior.add(current);
        current = new Backup();
        magic++;
    }


    @Override
    public void restoreState() {
        current.restore();
        current = prior.pop();
        magic++;
        notifyRestore();
    }

    @Override
    public void withNewState(Procedure body) {
        final int level = getLevel();
        saveState();
        body.call();
        restoreStateUntil(level);
    }

    @Override
    public void restoreAllState() {
        while (!prior.isEmpty())
            restoreState();
    }

    @Override
    public void restoreStateUntil(int level) {
        while (getLevel() > level)
            restoreState();
    }


    @Override
    public StateInt makeStateInt(int initValue) {
        return new TrailInt(this,initValue);
    }

    @Override
    public StateBool makeStateBool(boolean initValue) {
        return new TrailBool(this,initValue);
    }

    @Override
    public StateMap makeStateMap() {
        return new TrailMap(this);
    }
}
