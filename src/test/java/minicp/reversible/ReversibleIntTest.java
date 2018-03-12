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


import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ReversibleIntTest {

    {
        Trail trail = new Trail();
        ReversibleInt a = new ReversibleInt(trail,7);
        ReversibleInt b = new ReversibleInt(trail,13);

        trail.push();      // Conceptually: record current state a=7, b=13
        a.setValue(11);
        b.setValue(14);
        trail.push();   // Conceptually: record current state a=11, b=14
        a.setValue(4);
        b.setValue(9);
        trail.pop();
        trail.pop();
    }

    @Test
    public void testExample() {

        Trail trail = new Trail();

        // Two reversible int's inside the trail
        ReversibleInt a = new ReversibleInt(trail,5);
        ReversibleInt b = new ReversibleInt(trail,9);

        a.setValue(7);
        b.setValue(13);

        // Record current state a=7, b=1 and increase the level to 0
        trail.push();
        assertEquals(0,trail.getLevel());

        a.setValue(10);
        b.setValue(13);
        a.setValue(11);

        // Record current state a=11, b=13 and increase the level to 1
        trail.push();
        assertEquals(1,trail.getLevel());

        a.setValue(4);
        b.setValue(9);

        // Restore the state recorded at the top level 1: a=11, b=13
        // and remove the state of that level
        trail.pop();

        assertEquals(11,a.getValue());
        assertEquals(13,b.getValue());
        assertEquals(0,trail.getLevel());

        // Restore the state recorded at the top level 0: a=7, b=13
        // and remove the state of that level
        trail.pop();

        assertEquals(7,a.getValue());
        assertEquals(13,b.getValue());
        assertEquals(-1,trail.getLevel());

    }


    @Test
    public void testReversibleInt() {
        Trail trail = new Trail();
        ReversibleInt a = new ReversibleInt(trail,5);
        ReversibleInt b = new ReversibleInt(trail,5);
        assertTrue(a.getValue() == 5);
        a.setValue(7);
        b.setValue(13);
        assertTrue(a.getValue() == 7);

        trail.push();

        a.setValue(10);
        assertTrue(a.getValue() == 10);
        a.setValue(11);
        assertTrue(a.getValue() == 11);
        b.setValue(16);
        b.setValue(15);

        trail.pop();
        assertTrue(a.getValue() == 7);
        assertTrue(b.getValue() == 13);

    }

    @Test
    public void testPopAll() {

        Trail trail = new Trail();
        ReversibleInt a = new ReversibleInt(trail,5);
        ReversibleInt b = new ReversibleInt(trail,5);

        trail.push();

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        trail.push();

        a.setValue(5);
        b.setValue(10);

        ReversibleInt c = new ReversibleInt(trail,5);

        trail.push();

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        trail.popAll();
        trail.push();

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());


        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        trail.push();

        a.setValue(8);
        b.setValue(10);

        trail.pop();


        assertEquals(10,a.getValue());
        assertEquals(16,b.getValue());
        assertEquals(5,c.getValue());

        trail.popAll();

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());

    }


    @Test
    public void testPopUntill() {

        Trail trail = new Trail();
        ReversibleInt a = new ReversibleInt(trail,5);
        ReversibleInt b = new ReversibleInt(trail,5);

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        trail.push(); // level 0

        a.setValue(5);
        b.setValue(10);

        ReversibleInt c = new ReversibleInt(trail,5);

        trail.push(); // level 1

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        trail.push(); // level 2

        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        trail.push(); // level 3

        a.setValue(8);
        b.setValue(10);

        trail.popUntil(0);

        assertEquals(0,trail.getLevel());

        trail.push(); // level 1

        assertEquals(1,trail.getLevel());
        assertEquals(5,a.getValue());
        assertEquals(10,b.getValue());
        assertEquals(5,c.getValue());

        a.setValue(8);
        b.setValue(10);
        b.setValue(8);
        b.setValue(10);

        trail.popUntil(0);

        assertEquals(0,trail.getLevel());
        assertEquals(5,a.getValue());
        assertEquals(10,b.getValue());
        assertEquals(5,c.getValue());


    }

}
