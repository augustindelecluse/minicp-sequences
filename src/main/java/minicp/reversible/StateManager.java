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

package minicp.reversible;

import minicp.util.Procedure;


public interface StateManager {

    /**
     * @return The current level
     */
    public int getLevel();

    /**
     * Stores the current state
     * such that it can be recovered using restore()
     * Increase the level by 1
     */
    public void save();


    /**
     *  Restores state as it was at getLevel()-1
     *  Decrease the level by 1
     */
    public void restore();

    /**
     *  Restores the state as it was at level 0 (first save)
     *  The level is now -1.
     *  Notice that you'll probably want to save after this operation.
     */
    public void restoreAll();

    /**
     *  Restores the state as it was at level
     *  @param level
     */
    public void restoreUntil(int level);

    public StateInt makeStateInt(int initValue);

    public StateBool makeStateBool(boolean initValue);

    public StateMap makeStateMap();

    default void withNewState(Procedure body) {
        int level = getLevel();
        save();
        body.call();
        restoreUntil(level);
    }

}

