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

import minicp.util.NotImplementedException;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.*;

public class CopierTest {


    @Test
    public void test() {
        Copier copier = new Copier();

        StateInt a = copier.makeStateInt(5);
        StateInt b = copier.makeStateInt(5);

        assertEquals(2,copier.storeSize());

        copier.save();

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        copier.save();

        a.setValue(5);
        b.setValue(10);

        assertEquals(2,copier.storeSize());

        StateInt c = copier.makeStateInt(5);

        assertEquals(3,copier.storeSize());

        copier.save();

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        assertEquals(3,copier.storeSize());

        copier.restoreAll();
        copier.save();

        assertEquals(2,copier.storeSize());

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());


        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        copier.save();

        a.setValue(8);
        b.setValue(10);

        copier.restore();

        assertEquals(2,copier.storeSize());


        assertEquals(10,a.getValue());
        assertEquals(16,b.getValue());
        assertEquals(5,c.getValue());

        copier.restoreAll();

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());

        assertEquals(2,copier.storeSize());

    }
}