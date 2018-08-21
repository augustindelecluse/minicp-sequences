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

package minicp.engine.constraints;

import minicp.engine.SolverTest;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import minicp.util.NotImplementedExceptionAssume;
import org.junit.Test;

import static minicp.cp.Factory.*;
import static org.junit.Assert.*;

public class AbsoluteTest extends SolverTest {


    @Test
    public void simpleTest0() {

        try {
            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, -5, 5);
            IntVar y = makeIntVar(cp, -10, 10);

            cp.post(new Absolute(x, y));

            assertEquals(0, y.getMin());
            assertEquals(5, y.getMax());
            assertEquals(11, x.getSize());

            x.removeAbove(-2);
            cp.fixPoint();

            assertEquals(2, y.getMin());

            x.removeBelow(-4);
            cp.fixPoint();

            assertEquals(4, y.getMax());

        } catch (InconsistencyException e) {
            fail("should not fail");
        }  catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void simpleTest1() {
        try {
            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, -5, 5);
            IntVar y = makeIntVar(cp, -10, 10);
            notEqual(x, 0);
            notEqual(x, 5);
            notEqual(x, -5);

            cp.post(new Absolute(x, y));


            assertEquals(1, y.getMin());
            assertEquals(4, y.getMax());

        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


    @Test
    public void simpleTest2() {
        try {
            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, -5, 0);
            IntVar y = makeIntVar(cp, 4, 4);

            cp.post(new Absolute(x, y));

            assertTrue(x.isBound());
            assertTrue(y.isBound());
            assertEquals(-4, x.getMax());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void simpleTest3() {
        try {
            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, 7, 7);
            IntVar y = makeIntVar(cp, -1000, 12);

            cp.post(new Absolute(x, y));

            assertTrue(x.isBound());
            assertTrue(y.isBound());
            assertEquals(7, y.getMax());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }

    @Test
    public void simpleTest4() {
        try {
            Solver cp = solverFactory.get();
            IntVar x = makeIntVar(cp, -5, 10);
            IntVar y = makeIntVar(cp, -6, 7);

            cp.post(new Absolute(x, y));

            assertEquals(7, x.getMax());
            assertEquals(-5, x.getMin());

            notEqual(y, 0);
            cp.fixPoint();

            x.removeAbove(4);
            cp.fixPoint();

            assertEquals(5, y.getMax());

            x.removeAbove(-2);
            cp.fixPoint();

            assertEquals(2, y.getMin());

            y.removeBelow(5);
            cp.fixPoint();

            assertTrue(x.isBound());
            assertTrue(y.isBound());


        } catch (InconsistencyException e) {
            fail("should not fail");
        } catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }
    }


}