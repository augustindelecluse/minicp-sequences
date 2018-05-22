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

import minicp.util.InconsistencyException;
import org.junit.Test;

import static minicp.cp.Factory.makeIntVar;
import static minicp.cp.Factory.plus;
import static org.junit.Assert.*;


public class IntVarViewOffsetTest {

    public boolean propagateCalled = false;

    @Test
    public void testIntVar() {
        Solver cp  = new Solver();

        IntVar x = plus(makeIntVar(cp,-3,4),3); // domain is {0,1,2,3,4,5,6,7}

        assertEquals(0,x.getMin());
        assertEquals(7,x.getMax());
        assertEquals(8,x.getSize());

        cp.getTrail().push();


        try {

            assertFalse(x.isBound());

            x.remove(0);
            assertFalse(x.contains(0));
            x.remove(3);
            assertTrue(x.contains(1));
            assertTrue(x.contains(2));
            assertEquals(6,x.getSize());
            x.removeAbove(6);
            assertEquals(6,x.getMax());
            x.removeBelow(3);
            assertEquals(4,x.getMin());
            x.assign(5);
            assertTrue(x.isBound());
            assertEquals(5,x.getMax());



        } catch (InconsistencyException e) {
            e.printStackTrace();
            fail("should not fail here");
        }

        try {
            x.assign(4);
            fail( "should have failed" );
        } catch (InconsistencyException expectedException) {}

        cp.getTrail().pop();

        assertEquals(8,x.getSize());
        assertFalse(x.contains(-1));

    }



    @Test
    public void onDomainChangeOnBind() {
        propagateCalled = false;
        Solver cp  = new Solver();

        IntVar x = plus(makeIntVar(cp,10),1); // 1..11
        IntVar y = plus(makeIntVar(cp,10),1); // 1..11

        Constraint cons = new Constraint(cp) {

            @Override
            public void post() throws InconsistencyException {
                x.propagateOnBind(() -> propagateCalled = true);
                y.propagateOnDomainChange(() -> propagateCalled = true);
            }
        };

        try {
            cp.post(cons);
            x.remove(9);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.assign(5);
            cp.fixPoint();
            assertTrue(propagateCalled);
            propagateCalled = false;
            y.remove(11);
            cp.fixPoint();
            assertFalse(propagateCalled);
            y.remove(10);
            cp.fixPoint();
            assertTrue(propagateCalled);

        } catch (InconsistencyException inconsistency) {
            fail("should not fail");
        }
    }


    @Test
    public void onBoundChange() {

        Solver cp = new Solver();

        IntVar x = plus(makeIntVar(cp, 10),1);
        IntVar y = plus(makeIntVar(cp, 10),1);

        Constraint cons = new Constraint(cp) {

            @Override
            public void post() throws InconsistencyException {
                x.propagateOnBind(() -> propagateCalled  = true);
                y.propagateOnDomainChange(() -> propagateCalled = true);
            }
        };

        try {
            cp.post(cons);
            x.remove(9);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.remove(10);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.assign(5);
            cp.fixPoint();
            assertTrue(propagateCalled);
            propagateCalled = false;
            assertFalse(y.contains(11));
            y.remove(11);
            cp.fixPoint();
            assertFalse(propagateCalled);
            propagateCalled = false;
            y.remove(3);
            cp.fixPoint();
            assertTrue(propagateCalled);

        } catch (InconsistencyException inconsistency) {
            fail("should not fail");
        }
    }


}
