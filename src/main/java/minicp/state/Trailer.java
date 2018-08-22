package minicp.state;

import minicp.util.Procedure;

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

    public Trailer() {
        prior = new Stack<Backup>();
        current = new Backup();
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
    public void save() {
        prior.add(current);
        current = new Backup();
        magic++;
    }

    @Override
    public void restore() {
        prior.pop().restore();
        magic++;
    }

    @Override
    public void withNewState(Procedure body) {
        final int level = getLevel();
        save();
        body.call();
        restoreUntil(level);
    }

    @Override
    public void restoreAll() {
        while (!prior.isEmpty())
            restore();
    }

    @Override
    public void restoreUntil(int level) {
        while (getLevel() > level)
            restore();
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
