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

package minicp.util;

/**
 * Created by ldm on 1/9/17.
 */

public class Box<T> {
    private T _value;

    public Box(T v) {
        _value = v;
    }

    public T get() {
        return _value;
    }

    public void set(T v) {
        _value = v;
    }

    public String toString() {
        return _value.toString();
    }
}
