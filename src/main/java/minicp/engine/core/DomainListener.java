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


package minicp.engine.core;

public interface DomainListener {

    /**
     * Called whenever the domain becomes empty.
     */
    void empty();

    /**
     * Called whenever the domain becomes a single value.
     */
    void bind();

    /**
     * Called whenever the domain loses a value.
     */
    void change();

    /**
     * Called whenever the maximum value of the domain is lost.
     */
    void changeMin();

    /**
     * Called whenever the minmum value of the domain is lost.
     */
    void changeMax();
}
