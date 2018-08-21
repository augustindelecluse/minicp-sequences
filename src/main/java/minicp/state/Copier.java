package minicp.state;

import minicp.util.Procedure;
import java.util.Stack;

public class Copier implements StateManager {

    private Stack<Storage> store;
    private Stack<Stack<StateEntry>> prior;
    private Stack<StateEntry> backup;

    public Copier() {
        store = new Stack<Storage>();
        prior = new Stack<Stack<StateEntry>>();
        backup = new Stack<StateEntry>();
    }
    public int getLevel()  { return prior.size()-1;}
    public int storeSize() { return store.size();}

    public void save() {
        for (Storage s : store)
            backup.add(s.save());
        prior.add(backup);
        backup = prepare();
    }
    private Stack<StateEntry> prepare() {
        Stack<StateEntry> backup = new Stack<>();
        final int curSize = store.size();
        backup.add(new StateEntry() {
            public void restore() { store.setSize(curSize);}
        });
        return backup;
    }
    public void restore() {
        Stack<StateEntry> toRestore = prior.pop();
        for(StateEntry se : toRestore)
            se.restore();
        backup = prepare();
    }

    public void withNewState(Procedure body) {
        final int level = getLevel();
        save();
        body.call();
        restoreUntil(level);
    }

    /**
     *  Restores the state as it was at level 0 (first save)
     *  The level is now -1.
     *  Notice that you'll probably want to save after this operation.
     */
    public void restoreAll() {
        while (!prior.isEmpty())
            restore();
    }

    public void restoreUntil(int level) {
        while (getLevel() > level)
            restore();
    }

    public StateInt makeStateInt(int initValue) {
        CopyInt s = new CopyInt(initValue);
        store.add(s);
        return s;
    }

    public StateBool makeStateBool(boolean initValue) {
        CopyBool s = new CopyBool(initValue);
        store.add(s);
        return s;
    }

    public StateMap makeStateMap() {
        CopyMap s = new CopyMap<>();
        store.add(s);
        return s;
    }
}
