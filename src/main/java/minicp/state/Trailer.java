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

public class Trailer extends AbstractStateManager {

    private long magic = 0;

    /**
     * Initialize a reversible context
     * The current level is -1
     */
    public Trailer() {
        super();
    }

    public long getMagic() {
        return magic;
    }

    /**
     * Stores the current state
     * such that it can be recovered using restore()
     * Increase the level by 1
     */
    @Override
    public void save() {
        super.save();
        magic++;
        notifySave();
    }

    /**
     * Restores state as it was at getLevel()-1
     * Decrease the level by 1
     */
    @Override
    public void restore() {
        magic++;
        super.restore();
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

