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

import org.junit.Test;

import static minicp.cp.Factory.makeIntVar;
import static minicp.cp.Factory.makeSolver;
import static org.junit.Assert.*;


public class DomainTest {

    private static class MyDomainListener implements DomainListener {

        int nBind = 0;
        int nChange = 0;
        int nRemoveBelow = 0;
        int nRemoveAbove = 0;
        @Override
        public void empty() {
        }
        @Override
        public void bind() {
            nBind++;
        }

        @Override
        public void change() {
            nChange++;
        }

        @Override
        public void removeBelow() {
            nRemoveBelow++;
        }

        @Override
        public void removeAbove() {
            nRemoveAbove++;
        }
    };



    @Test
    public void testDomain1() {
        Solver cp  = makeSolver();
        MyDomainListener dlistener = new MyDomainListener();
        IntDomain dom = new SparseSetDomain(cp,5,10);

        dom.removeAbove(8,dlistener);

        assertEquals(1, dlistener.nChange);
        assertEquals(0, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(0, dlistener.nRemoveBelow);

        dom.remove(6,dlistener);

        assertEquals(2, dlistener.nChange);
        assertEquals(0, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(0, dlistener.nRemoveBelow);

        dom.remove(5,dlistener);

        assertEquals(3, dlistener.nChange);
        assertEquals(0, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(1, dlistener.nRemoveBelow);

        dom.remove(7,dlistener);

        assertEquals(4, dlistener.nChange);
        assertEquals(1, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(2, dlistener.nRemoveBelow);

    }

    @Test
    public void testDomain2() {
        Solver cp  = makeSolver();
        MyDomainListener dlistener = new MyDomainListener();
        IntDomain dom = new SparseSetDomain(cp,5,10);

        dom.removeAllBut(7,dlistener);

        assertEquals(1, dlistener.nChange);
        assertEquals(1, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(1, dlistener.nRemoveBelow);

    }

    @Test
    public void testDomain3() {
        Solver cp  = makeSolver();
        MyDomainListener dlistener = new MyDomainListener();
        IntDomain dom = new SparseSetDomain(cp,5,10);

        dom.removeAbove(5,dlistener);

        assertEquals(1, dlistener.nChange);
        assertEquals(1, dlistener.nBind);
        assertEquals(1, dlistener.nRemoveAbove);
        assertEquals(0, dlistener.nRemoveBelow);

    }


}
