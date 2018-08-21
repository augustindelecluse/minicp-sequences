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

import minicp.engine.SolverTest;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static minicp.cp.Factory.*;
import static org.junit.Assert.*;


public class IntVarTest extends SolverTest {

    public boolean propagateCalled = false;

    @Test
    public void testIntVar() {
        Solver cp = solverFactory.get();

        IntVar x = makeIntVar(cp, 10);
        IntVar y = makeIntVar(cp, 10);

        cp.getStateManager().save();


        try {

            assertFalse(x.isBound());
            x.remove(5);
            assertEquals(9, x.getSize());
            x.assign(7);
            assertEquals(1, x.getSize());
            assertTrue(x.isBound());
            assertEquals(7, x.getMin());
            assertEquals(7, x.getMax());

        } catch (InconsistencyException e) {
            fail("should not fail here");
        }

        try {
            x.assign(8);
            fail("should have failed");
        } catch (InconsistencyException expectedException) {
        }


        cp.getStateManager().restore();
        cp.getStateManager().save();

        assertFalse(x.isBound());
        assertEquals(10, x.getSize());

        for (int i = 0; i < 10; i++) {
            assertTrue(x.contains(i));
        }
        assertFalse(x.contains(-1));

    }

    @Test
    public void onDomainChangeOnBind() {
        propagateCalled = false;
        Solver cp = solverFactory.get();

        IntVar x = makeIntVar(cp, 10);
        IntVar y = makeIntVar(cp, 10);

        Constraint cons = new AbstractConstraint(cp) {
            @Override
            public void post() {
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
    public void arbitraryRangeDomains() {

        try {

            Solver cp = solverFactory.get();

            IntVar x = makeIntVar(cp, -10, 10);

            cp.getStateManager().save();


            try {

                assertFalse(x.isBound());
                x.remove(-9);
                x.remove(-10);


                assertEquals(19, x.getSize());
                x.assign(-4);
                assertEquals(1, x.getSize());
                assertTrue(x.isBound());
                assertEquals(-4, x.getMin());

            } catch (InconsistencyException e) {
                fail("should not fail here");
            }

            try {
                x.assign(8);
                fail("should have failed");
            } catch (InconsistencyException expectedException) {
            }


            cp.getStateManager().restore();

            assertEquals(21, x.getSize());

            for (int i = -10; i < 10; i++) {
                assertTrue(x.contains(i));
            }
            assertFalse(x.contains(-11));


        } catch (NotImplementedException e) {
            e.print();
        }
    }


    @Test
    public void arbitrarySetDomains() {

        try {

            Solver cp = solverFactory.get();

            Set<Integer> dom = new HashSet<>(Arrays.asList(-7, -10, 6, 9, 10, 12));

            IntVar x = makeIntVar(cp, dom);

            cp.getStateManager().save();

            try {

                for (int i = -15; i < 15; i++) {
                    if (dom.contains(i))
                        assertTrue(x.contains(i));
                    else assertFalse(x.contains(i));
                }

                x.assign(-7);
            } catch (InconsistencyException e) {
                fail("should not fail here");
            }

            try {
                x.assign(-10);
                fail("should have failed");
            } catch (InconsistencyException expectedException) {
            }


            cp.getStateManager().restore();

            for (int i = -15; i < 15; i++) {
                if (dom.contains(i)) assertTrue(x.contains(i));
                else assertFalse(x.contains(i));
            }
            assertEquals(6, x.getSize());


        } catch (NotImplementedException e) {
            e.print();
        }
    }


    @Test
    public void onBoundChange() {

        Solver cp = solverFactory.get();

        IntVar x = makeIntVar(cp, 10);
        IntVar y = makeIntVar(cp, 10);

        Constraint cons = new AbstractConstraint(cp) {

            @Override
            public void post() {
                x.whenBind(() -> propagateCalled = true);
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


    @Test
    public void removeAbove() {

        try {

            Solver cp = solverFactory.get();

            IntVar x = makeIntVar(cp, 10);

            Constraint cons = new AbstractConstraint(cp) {
                @Override
                public void post() {
                    x.propagateOnBoundChange(this);
                }

                @Override
                public void propagate() {
                    propagateCalled = true;
                }
            };

            try {
                cp.post(cons);
                x.remove(8);
                cp.fixPoint();
                assertFalse(propagateCalled);
                x.removeAbove(8);
                assertEquals(7, x.getMax());
                cp.fixPoint();
                assertTrue(propagateCalled);

            } catch (InconsistencyException inconsistency) {
                fail("should not fail");
            }

        } catch (NotImplementedException e) {
            e.print();
        }
    }

    @Test
    public void removeBelow() {

        try {

            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, 10);

            Constraint cons = new AbstractConstraint(cp) {
                @Override
                public void post() {
                    x.propagateOnBoundChange(this);
                }

                @Override
                public void propagate() {
                    propagateCalled = true;
                }
            };

            try {
                cp.post(cons);
                x.remove(3);
                cp.fixPoint();
                assertFalse(propagateCalled);
                x.removeBelow(3);
                assertEquals(4, x.getMin());
                cp.fixPoint();
                assertTrue(propagateCalled);
                propagateCalled = false;

                x.removeBelow(5);
                assertEquals(5, x.getMin());
                cp.fixPoint();
                assertTrue(propagateCalled);
                propagateCalled = false;


            } catch (InconsistencyException inconsistency) {
                fail("should not fail");
            }

        } catch (NotImplementedException e) {
            e.print();
        }
    }


    @Test
    public void fillArray() {

        try {
            Solver cp = solverFactory.get();

            IntVar x = plus(mul(minus(makeIntVar(cp, 5)), 3), 5); // D(x)= {-7,-4,-1,2,5}
            int[] values = new int[10];
            int s = x.fillArray(values);
            HashSet<Integer> dom = new HashSet<Integer>();
            for (int i = 0; i < s; i++) {
                dom.add(values[i]);
            }
            HashSet<Integer> expectedDom = new HashSet<Integer>();
            Collections.addAll(expectedDom, -7, -4, -1, 2, 5);
            assertEquals(expectedDom, dom);

        } catch (NotImplementedException e) {
            e.print();
        }
    }


}
