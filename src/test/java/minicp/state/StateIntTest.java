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


import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public class StateIntTest {

    @Parameters
    public static Object[] data() {
        return new Object[]{
                new Supplier<StateManager>() {
                    @Override
                    public StateManager get() {
                        return new Trailer();
                    }
                }, new Supplier<StateManager>() {
            @Override
            public StateManager get() {
                return new Copier();
            }
        }};
    }


    @Parameter
    public Supplier<StateManager> stateFactory;

    @Test
    public void testExample() {
        StateManager sm = stateFactory.get();

        // Two reversible int's inside the sm
        StateInt a = sm.makeStateInt(5);
        StateInt b = sm.makeStateInt(9);

        a.setValue(7);
        b.setValue(13);

        // Record current state a=7, b=1 and increase the level to 0
        sm.save();
        //assertEquals(0,sm.getLevel());

        a.setValue(10);
        b.setValue(13);
        a.setValue(11);

        // Record current state a=11, b=13 and increase the level to 1
        sm.save();
        //assertEquals(1,sm.getLevel());

        a.setValue(4);
        b.setValue(9);

        // Restore the state recorded at the top level 1: a=11, b=13
        // and remove the state of that level
        sm.restore();

        assertEquals(11,a.getValue());
        assertEquals(13,b.getValue());
        //assertEquals(0,sm.getLevel());

        // Restore the state recorded at the top level 0: a=7, b=13
        // and remove the state of that level
        sm.restore();

        assertEquals(7,a.getValue());
        assertEquals(13,b.getValue());
        //assertEquals(-1,sm.getLevel());

    }


    @Test
    public void testReversibleInt() {
        StateManager sm = stateFactory.get();

        StateInt a = sm.makeStateInt(5);
        StateInt b = sm.makeStateInt(5);
        assertTrue(a.getValue() == 5);
        a.setValue(7);
        b.setValue(13);
        assertTrue(a.getValue() == 7);

        sm.save();

        a.setValue(10);
        assertTrue(a.getValue() == 10);
        a.setValue(11);
        assertTrue(a.getValue() == 11);
        b.setValue(16);
        b.setValue(15);

        sm.restore();
        assertTrue(a.getValue() == 7);
        assertTrue(b.getValue() == 13);

    }

    @Test
    public void testPopAll() {
        StateManager sm = stateFactory.get();

        StateInt a = sm.makeStateInt(5);
        StateInt b = sm.makeStateInt(5);

        sm.save();

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        sm.save();

        a.setValue(5);
        b.setValue(10);

        StateInt c = sm.makeStateInt(5);

        sm.save();

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        sm.restoreAll();
        sm.save();

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());


        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        sm.save();

        a.setValue(8);
        b.setValue(10);

        sm.restore();


        assertEquals(10,a.getValue());
        assertEquals(16,b.getValue());
        assertEquals(5,c.getValue());

        sm.restoreAll();

        assertEquals(5,a.getValue());
        assertEquals(5,b.getValue());
        assertEquals(5,c.getValue());

    }


    @Test
    public void testPopUntill() {
        StateManager sm = stateFactory.get();

        StateInt a = sm.makeStateInt(5);
        StateInt b = sm.makeStateInt(5);

        a.setValue(7);
        b.setValue(13);
        a.setValue(13);

        sm.save(); // level 0

        a.setValue(5);
        b.setValue(10);

        StateInt c = sm.makeStateInt(5);

        sm.save(); // level 1

        a.setValue(8);
        b.setValue(1);
        c.setValue(10);

        sm.save(); // level 2

        a.setValue(10);
        b.setValue(13);
        b.setValue(16);

        sm.save(); // level 3

        a.setValue(8);
        b.setValue(10);

        sm.restoreUntil(0);

        //assertEquals(0,sm.getLevel());

        sm.save(); // level 1

        //assertEquals(1,sm.getLevel());
        assertEquals(5,a.getValue());
        assertEquals(10,b.getValue());
        assertEquals(5,c.getValue());

        a.setValue(8);
        b.setValue(10);
        b.setValue(8);
        b.setValue(10);

        sm.restoreUntil(0);

        //assertEquals(0,sm.getLevel());
        assertEquals(5,a.getValue());
        assertEquals(10,b.getValue());
        assertEquals(5,c.getValue());


    }


    @Test
    public void testPopUntillEasy() {
        StateManager sm = stateFactory.get();

        StateInt a = sm.makeStateInt(5);

        a.setValue(7);
        a.setValue(13);

        sm.save(); // level 0

        a.setValue(6);


        sm.save(); // level 1

        a.setValue(8);

        sm.save(); // level 2

        a.setValue(10);

        sm.save(); // level 3

        a.setValue(8);

        sm.restoreUntil(0);

        //assertEquals(0,sm.getLevel());

        sm.save(); // level 1

        //assertEquals(1,sm.getLevel());
        assertEquals(6,a.getValue());


        a.setValue(8);

        sm.restoreUntil(0);

        //assertEquals(0,sm.getLevel());
        assertEquals(6,a.getValue());



    }
}
