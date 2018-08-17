package minicp.state;

import minicp.util.Procedure;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class Copier implements StateManager {
    private ArrayList<Storage> _store;
    Stack<ArrayList<StateEntry>> _priorStates;

    public Copier() {
        _store = new ArrayList<>();
        _priorStates = new Stack<ArrayList<StateEntry>>();
    }

    @Override public void save() {
        ArrayList<StateEntry> copy = new ArrayList<StateEntry>(_store.size());
        for(Storage s : _store)
            copy.add(s.save());
        _priorStates.push(copy);
    }

    @Override public void restore() {
        ArrayList<StateEntry> last = _priorStates.pop();
        for (StateEntry state: last) {
            state.restore();
        }
    }

    @Override public void restoreAll() {
        while(!_priorStates.empty())
            restore();
    }

    @Override public void restoreUntil(int level) {
        while (_priorStates.size()-1 > level)
            restore();
    }

    @Override
    public StateInt makeStateInt(int initValue) {
        CopyInt s = new CopyInt(initValue);
        _store.add(s);
        // I don't understand this, you add it everywhere, why and why not booleans, why not only on the last one ?
        //_priorStates.peek().add(s.save());
        return s;
    }

    @Override
    public StateBool makeStateBool(boolean initValue) {
        CopyBool s = new CopyBool(initValue);
        _store.add(s);
        return s;
    }

    @Override
    public StateMap makeStateMap() {
        CopyMap s = new CopyMap<>();
        _store.add(s);
        return s;
    }

    @Override
    public void withNewState(Procedure body) {
        save();
        body.call();
        restore();
    }
}
