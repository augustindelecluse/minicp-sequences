package minicp.state;

import java.util.Stack;

public class Copier extends AbstractStateManager {

    private Stack<Storage> store;

    private StateEntry popStore = new StateEntry() {
        @Override
        public void restore() {
            store.pop();
        }
    };

    public Copier() {
        store = new Stack<>();
    }

    @Override public void save() {
        super.save();
        for (Storage s : store) {
            pushState(s.save());
        }
        notifySave();
    }

    public int storeSize() {
        return store.size();
    };

    @Override
    public StateInt makeStateInt(int initValue) {
        CopyInt s = new CopyInt(initValue);
        store.add(s);
        pushState(popStore);
        return s;
    }

    @Override
    public StateBool makeStateBool(boolean initValue) {
        CopyBool s = new CopyBool(initValue);
        store.add(s);
        pushState(popStore);
        return s;
    }

    @Override
    public StateMap makeStateMap() {
        CopyMap s = new CopyMap<>();
        store.add(s);
        pushState(popStore);
        return s;
    }


}
