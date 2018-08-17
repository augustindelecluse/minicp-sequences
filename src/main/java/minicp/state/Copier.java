package minicp.state;

import minicp.util.Procedure;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class Copier extends AbstractStateManager {

    private ArrayList<Storage> store;

    public Copier() {
        store = new ArrayList<>();
    }

    @Override public void save() {
        super.save();
        for (Storage s : store) {
            pushOnTrail(s.save());
        }
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
