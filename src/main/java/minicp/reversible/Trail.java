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

import java.util.Stack;


public interface Trail {

    /**
     * @return The current level
     */
    public int getLevel();

    /**
     * Stores the current state
     * such that it can be recovered using pop()
     * Increase the level by 1
     */
    public void push();


    /**
     *  Restores state as it was at getLevel()-1
     *  Decrease the level by 1
     */
    public void pop();

    /**
     *  Restores the state as it was at level 0 (first push)
     *  The level is now -1.
     *  Notice that you'll probably want to push after this operation.
     */
    public void popAll();

    /**
     *  Restores the state as it was at level
     *  @param level
     */
    public void popUntil(int level);

    public RevInt makeRevInt(int initValue);

    public RevBool makeRevBool(boolean initValue);

}

