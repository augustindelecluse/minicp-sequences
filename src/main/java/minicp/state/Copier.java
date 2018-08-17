package minicp.state;

import java.util.ArrayList;

public class Copier extends AbstractStateManager {

    private ArrayList<Storage> store;

    public Copier() {
        store = new ArrayList<>();
    }

    @Override public void save() {
        super.save();
        for (Storage s : store) {
            pushState(s.save());
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
