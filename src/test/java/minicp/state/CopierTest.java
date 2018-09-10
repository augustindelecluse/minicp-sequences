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
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package minicp.state;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CopierTest {


    @Test
    public void test() {
        Copier copier = new Copier();

        StateInt a = copier.makeStateInt(5);
        StateInt b = copier.makeStateInt(5);

        assertEquals(2, copier.storeSize());

        copier.saveState();

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        copier.saveState();

        a.setValue(5);
        b.setValue(10);

        assertEquals(2, copier.storeSize());

        StateInt c = copier.makeStateInt(5);

        assertEquals(3, copier.storeSize());

        copier.saveState();

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        assertEquals(3, copier.storeSize());

        copier.restoreAllState();
        copier.saveState();

        assertEquals(2, copier.storeSize());

        assertEquals(5, a.value());
        assertEquals(5, b.value());
        assertEquals(5, c.value());


        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        copier.saveState();

        a.setValue(8);
        b.setValue(10);

        copier.restoreState();

        assertEquals(2, copier.storeSize());


        assertEquals(10, a.value());
        assertEquals(16, b.value());
        assertEquals(5, c.value());

        copier.restoreAllState();

        assertEquals(5, a.value());
        assertEquals(5, b.value());
        assertEquals(5, c.value());

        assertEquals(2, copier.storeSize());

    }
}
