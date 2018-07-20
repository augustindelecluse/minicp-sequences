package minicp.state;

import minicp.util.Procedure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class Copier implements StateManager {
    private ArrayList<Storage> _store;
    Stack<ArrayList> _priorStates;
    public Copier() {
        _store = new ArrayList<>();
        _priorStates = new Stack<ArrayList>();
    }

    @Override
    public int getLevel() {
        return _priorStates.size();
    }

    @Override
    public void save() {
        ArrayList<Storage> copy = new ArrayList<Storage>(_store.size());
        for(Storage s : _store)
            copy.add(s.saveTo());
        _priorStates.push(copy);
    }

    @Override
    public void restore() {
        ArrayList<Storage> last = _priorStates.pop();
        Iterator<Storage> i = last.iterator();
        for(Storage s : _store)
            s.restoreFrom(i.next());
    }

    @Override
    public void restoreAll() {
        while(!_priorStates.empty())
            restore();
    }

    @Override
    public void restoreUntil(int level) {
        while (_priorStates.size() != level)
            restore();
    }

    @Override
    public StateInt makeStateInt(int initValue) {
        CopyInt s = new CopyInt(this,initValue);
        _store.add(s);
        return s;
    }

    @Override
    public StateBool makeStateBool(boolean initValue) {
        CopyBool s = new CopyBool(this,initValue);
        _store.add(s);
        return s;
    }

    @Override
    public StateMap makeStateMap() {
        CopyMap s = new CopyMap<>(this);
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
