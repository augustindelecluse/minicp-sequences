/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2017. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.state;

import minicp.util.Procedure;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;


public abstract class AbstractStateManager implements StateManager {

    private Stack<StateEntry> stateEntries = new Stack<StateEntry>();
    private Stack<Integer> limits = new Stack<Integer>();

    private List<Procedure> restoreListeners  = new LinkedList<>();
    private List<Procedure> saveListeners  = new LinkedList<>();

    protected void notifyRestore() {
        for (Procedure p: restoreListeners) {
            p.call();
        }
    }

    protected void notifySave() {
        for (Procedure p: saveListeners) {
            p.call();
        }
    }


    public void onRestore(Procedure listener) {
        restoreListeners.add(listener);
    }

    public void onSave(Procedure listener) {
        saveListeners.add(listener);
    }

    /**
     * Initialize a reversible context
     * The current level is -1
     */
    public AbstractStateManager() {
        stateEntries.ensureCapacity(1000000);
        limits.ensureCapacity(1000000);
    }


    public void pushState(StateEntry entry) {
        stateEntries.push(entry);
    }

    /**
     *
     * Restore all the entries from the top of the stateEntries stack
     * to the limit (excluded)
     */
    private void restoreToSize(int size) {
        int n = stateEntries.size() - size;
        for (int i = 0; i < n; i++) {
            stateEntries.pop().restore();
        }
    }

    /**
     * @return The current level
     */
    public int getLevel() {
        return limits.size()-1;
    }

    /**
     * Stores the current state
     * such that it can be recovered using restore()
     * Increase the level by 1
     */
    public void save() {
        limits.push(stateEntries.size());
    }


    /**
     *  Restores state as it was at getLevel()-1
     *  Decrease the level by 1
     */
    public void restore() {
        restoreToSize(limits.pop());
        notifyRestore();
    }

    /**
     *  Restores the state as it was at level 0 (first save)
     *  The level is now -1.
     *  Notice that you'll probably want to save after this operation.
     */
    public void restoreAll() {
        restoreUntil(-1);
        stateEntries.clear();
    }

    /**
     *  Restores the state as it was at level
     *  @param level
     */
    public void restoreUntil(int level) {
        while (getLevel() > level) {
            restore();
        }
    }

    public void withNewState(Procedure body) {
        int level = getLevel();
        save();
        body.call();
        restoreUntil(level);
    }

    public abstract StateInt makeStateInt(int initValue);
    public abstract StateBool makeStateBool(boolean initValue);
    public abstract StateMap makeStateMap();

}

