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

public interface StateManager {

    /**
     * Stores the current state
     * such that it can be recovered using restore()
     * Increase the level by 1
     */
    void save();

    /**
     *  Restores state as it was at getLevel()-1
     *  Decrease the level by 1
     */
    void restore();

    /**
     *  Restores the state as it was at level 0 (first save)
     *  The level is now -1.
     *  Notice that you'll probably want to save after this operation.
     */
    void restoreAll();

    /**
     * Creates a Stateful integer (restorable)
     * @param initValue the initial value
     * @return a reference to the integer.
     */
    StateInt makeStateInt(int initValue);
    /**
     * Creates a Stateful boolean (restorable)
     * @param initValue the initial value
     * @return a reference to the boolean.
     */
    StateBool makeStateBool(boolean initValue);
    /**
     * Creates a Stateful map (restorable)
     * @return a reference to the map.
     */
    StateMap makeStateMap();

    /**
     * Higher-order function that preserves the state prior to calling body and restores it after.
     * @param body the first-order function to execute.
     */
    void withNewState(Procedure body);
}

