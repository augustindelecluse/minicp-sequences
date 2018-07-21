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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StateSparseSetTest {



    @Test
    public void testExample() {

        StateManager sm = new Trailer();
        StateSparseSet set = new StateSparseSet(sm,9,0);

        sm.save();

        set.remove(4);
        set.remove(6);

        assertFalse(set.contains(4));
        assertFalse(set.contains(6));

        sm.restore();

        assertTrue(set.contains(4));
        assertTrue(set.contains(6));

    }

    @Test
    public void testReversibleSparseSet() {

        StateManager sm = new Trailer();
        StateSparseSet set = new StateSparseSet(sm,10,0);

        assertTrue(toSet(set.toArray()).equals(toSet(new int[]{0,1,2,3,4,5,6,7,8,9})));

        sm.save();

        set.remove(1);
        set.remove(0);

        assertTrue(set.getMin() == 2);

        set.remove(8);
        set.remove(9);

        assertTrue(toSet(set.toArray()).equals(toSet(new int[]{2,3,4,5,6,7})));
        assertTrue(set.getMax() == 7);

        sm.restore();
        sm.save();

        assertEquals(10, set.getSize());

        for (int i = 0; i < 10; i++) {
            assertTrue(set.contains(i));
        }
        assertFalse(set.contains(10));

        assertTrue(set.getMin() == 0);
        assertTrue(set.getMax() == 9);

        set.removeAllBut(2);

        for (int i = 0; i < 10; i++) {
            if (i != 2) assertFalse(set.contains(i));
        }
        assertTrue(set.contains(2));
        assertTrue(toSet(set.toArray()).equals(toSet(new int[]{2})));


        sm.restore();
        sm.save();

        assertEquals(10, set.getSize());

    }

    private Set<Integer> toSet(int... values) {
        Set<Integer> set = new java.util.HashSet<Integer>();
        for (int v: values) {
            set.add(v);
        }
        return set;
    }
    @Test
    public void testRangeConstructor() {

        try {

            StateManager sm = new Trailer();
            StateSparseSet set = new StateSparseSet(sm,10,0);

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }

            sm.save();

            set.remove(4);
            set.remove(5);
            set.remove(0);
            set.remove(1);

            assertEquals(2,set.getMin());
            assertEquals(9,set.getMax());

            sm.save();

            set.removeAllBut(7);
            assertEquals(7,set.getMin());
            assertEquals(7,set.getMax());


            sm.restore();
            sm.restore();

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }

        } catch (NotImplementedException e) {
            e.print();
        }
    }

    @Test
    public void testRemoveBelow() {

        //try {

            StateManager sm = new Trailer();
            StateSparseSet set = new StateSparseSet(sm,10,0);

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }

            sm.save();



            set.removeBelow(5);



            assertEquals(5,set.getMin());
            assertEquals(9,set.getMax());

            sm.save();

            set.remove(7);
            set.removeBelow(7);

            assertEquals(8,set.getMin());

            sm.restore();
            sm.restore();

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }


    }

    @Test
    public void testRemoveAbove() {

        try {

            StateManager sm = new Trailer();
            StateSparseSet set = new StateSparseSet(sm,10,0);

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }

            sm.save();


            set.remove(1);
            set.remove(2);

            set.removeAbove(7);

            assertEquals(0,set.getMin());
            assertEquals(7,set.getMax());

            sm.save();

            set.removeAbove(2);

            assertEquals(0,set.getMax());

            sm.restore();
            sm.restore();

            for (int i = 0; i < 10; i++) {
                assertTrue(set.contains(i));
            }

        } catch (NotImplementedException e) {
            e.print();
        }
    }
}