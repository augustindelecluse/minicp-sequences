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


import static minicp.cp.Factory.*;
import static org.junit.Assert.*;


public class IntVarViewMulTest {

    public boolean propagateCalled = false;

    @Test
    public void testIntVar() {
        Solver cp  = makeSolver();

        IntVar x = mul(mul(makeIntVar(cp,-3,4),-3),-1); // domain is {-9,-6,-3,0,3,6,9,12}

        assertEquals(-9,x.getMin());
        assertEquals(12,x.getMax());
        assertEquals(8,x.getSize());

        cp.getTrail().push();


        try {

            assertFalse(x.isBound());

            x.remove(-6);
            assertFalse(x.contains(-6));
            x.remove(2);
            assertTrue(x.contains(0));
            assertTrue(x.contains(3));
            assertEquals(7,x.getSize());
            x.removeAbove(7);
            assertEquals(6,x.getMax());
            x.removeBelow(-8);
            assertEquals(-3,x.getMin());
            x.assign(3);
            assertTrue(x.isBound());
            assertEquals(3,x.getMax());



        } catch (InconsistencyException e) {
            e.printStackTrace();
            fail("should not fail here");
        }

        try {
            x.assign(8);
            fail( "should have failed" );
        } catch (InconsistencyException expectedException) {}

        cp.getTrail().pop();

        assertEquals(8,x.getSize());
        assertFalse(x.contains(-1));

    }


    @Test
    public void onDomainChangeOnBind() {
        propagateCalled = false;
        Solver cp  = makeSolver();

        IntVar x = mul(makeIntVar(cp,10),1);
        IntVar y = mul(makeIntVar(cp,10),1);

        Constraint cons = new Constraint() {

            @Override
            public void post()  {
                x.whenBind(() -> propagateCalled = true);
                y.whenDomainChange(() -> propagateCalled = true);
            }
        };

        try {
            cp.post(cons);
            x.remove(8);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.assign(4);
            cp.fixPoint();
            assertTrue(propagateCalled);
            propagateCalled = false;
            y.remove(10);
            cp.fixPoint();
            assertFalse(propagateCalled);
            y.remove(9);
            cp.fixPoint();
            assertTrue(propagateCalled);

        } catch (InconsistencyException inconsistency) {
            fail("should not fail");
        }
    }


    @Test
    public void onBoundChange() {

        Solver cp = makeSolver();

        IntVar x = mul(makeIntVar(cp, 10),1);
        IntVar y = mul(makeIntVar(cp, 10),1);

        Constraint cons = new Constraint() {

            @Override
            public void post()  {
                x.whenBind(() -> propagateCalled  = true);
                y.whenDomainChange(() -> propagateCalled = true);
            }
        };

        try {
            cp.post(cons);
            x.remove(8);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.remove(9);
            cp.fixPoint();
            assertFalse(propagateCalled);
            x.assign(4);
            cp.fixPoint();
            assertTrue(propagateCalled);
            propagateCalled = false;
            assertFalse(y.contains(10));
            y.remove(10);
            cp.fixPoint();
            assertFalse(propagateCalled);
            propagateCalled = false;
            y.remove(2);
            cp.fixPoint();
            assertTrue(propagateCalled);

        } catch (InconsistencyException inconsistency) {
            fail("should not fail");
        }
    }


}
