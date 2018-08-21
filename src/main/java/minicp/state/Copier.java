package minicp.state;

import minicp.util.Procedure;
import java.util.Stack;

public class Copier implements StateManager {

    class Backup extends Stack<StateEntry> {
        private int sz;
        private Stack<Storage> toFix;
        Backup(Stack<Storage> st) {
            sz = st.size();
            toFix = st;
            for (Storage s : st)
                add(s.save());
        }
        void restore() {
            toFix.setSize(sz);
            for(StateEntry se : this)
                se.restore();
        }
    }
    private Stack<Storage> store;
    private Stack<Backup> prior;

    public Copier() {
        store = new Stack<Storage>();
        prior = new Stack<Backup>();
    }
    public int getLevel()  { return prior.size()-1;}
    public int storeSize() { return store.size();}
    public void save()     { prior.add(new Backup(store));}
    public void restore()  { prior.pop().restore();}

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