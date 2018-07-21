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

import java.util.Stack;


public class Trailer implements StateManager {

    private long magic = 0;
    private Stack<TrailEntry> trail = new Stack<TrailEntry>();
    private Stack<Integer>    trailLimit = new Stack<Integer>();

    /**
     * Initialize a reversible context
     * The current level is -1
     */
    public Trailer() {}

    public long getMagic() { return magic;}

    public void pushOnTrail(TrailEntry entry) {
        trail.push(entry);
    }

    /**
     *
     * Restore all the entries from the top of the trailStack
     * to the limit (excluded)
     */
    private void restoreToSize(int size) {
        int n = trail.size() - size;
        for (int i = 0; i < n; i++) {
            trail.pop().restore();
        }
    }

    /**
     * @return The current level
     */
    public int getLevel() {
        return trailLimit.size()-1;
    }

    /**
     * Stores the current state
     * such that it can be recovered using restore()
     * Increase the level by 1
     */
    public void save() {
        magic++;
        trailLimit.push(trail.size());
    }


    /**
     *  Restores state as it was at getLevel()-1
     *  Decrease the level by 1
     */
    public void restore() {
        restoreToSize(trailLimit.pop());
        // Increments the magic because we want to trail again
        magic++;
    }

    /**
     *  Restores the state as it was at level 0 (first save)
     *  The level is now -1.
     *  Notice that you'll probably want to save after this operation.
     */
    public void restoreAll() {
        restoreUntil(-1);
        trail.clear();
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

    @Override
    public StateInt makeStateInt(int initValue) {
        return new TrailInt(this, initValue);
    }
    @Override
    public StateBool makeStateBool(boolean initValue) {
        return new TrailBool(this, initValue);
    }
    @Override
    public StateMap makeStateMap() {
        return new TrailMap(this);
    }

}

