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

import java.util.ArrayList;

public class StateStack<E> {

    StateInt size;
    ArrayList<E> stack;

    public StateStack(StateManager sm) {
        size = sm.makeStateInt(0);
        stack = new ArrayList<E>();
    }

    public void push(E elem) {
        stack.add(size.value(), elem);
        size.increment();
    }

    public int size() {
        return size.value();
    }

    public E get(int index) {
        return stack.get(index);
    }
}
