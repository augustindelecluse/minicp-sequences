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

import java.util.ArrayList;

public class ReversibleStack<E> {

    RevInt size;
    ArrayList<E> stack;

    public ReversibleStack(StateManager sm) {
        size = sm.getTrail().makeRevInt(0);
        stack = new ArrayList<E>();
    }

    public void push(E elem) {
        stack.add(size.getValue(),elem);
        size.increment();
    }

    public int size() { return size.getValue(); }

    public E get(int index) { return stack.get(index); }
}
