package minicp.engine.constraints.sequence;

import minicp.engine.SolverTest;
import minicp.engine.core.*;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static minicp.cp.Factory.*;
import static org.junit.Assert.*;

public class TransitionTimesTest extends SolverTest {

    Solver cp;
    StateManager sm;
    SequenceVar sequence;
    static int nNodes;
    static int begin;
    static int end;
    static int[][] transitions;
    static int[] serviceTime;

    @BeforeClass
    public static void SetUpClass() {
        nNodes = 4;
        begin = 4;
        end = 5;
        /* layout considered
               b--4-c
               |    |
               3    3
               |    |
        e&f--4-a--4-d

        letters = nodes
        int = distances
         */
        transitions = new int[][] {
                {0, 3, 5, 4, 4, 4},
                {3, 0, 4, 5, 5, 5},
                {5, 4, 0, 3, 9, 9},
                {4, 5, 3, 0, 8, 8},
                {4, 5, 9, 8, 0, 0},
                {4, 5, 9, 8, 0, 0},
        };
        serviceTime = new int[] {5, 5, 5, 5, 0, 0};
    }

    @Before
    public void SetUp() {
        cp = solverFactory.get();
        sm = cp.getStateManager();
        sequence = new SequenceVarImpl(cp, nNodes, begin, end);
    }

    @Test
    public void testOneNodeReachable() {
        IntVar[] time = new IntVar[] {
                makeIntVar(cp, 0, 10),
                makeIntVar(cp, 0, 0),
                makeIntVar(cp, 0, 0),
                makeIntVar(cp, 0, 0),
                makeIntVar(cp, 0, 0),
                makeIntVar(cp, 100, 200),
        };
        // only the node 0 is reachable with those time available
        cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));

        int[][] scheduledInsertion1 = new int[][] {
                {sequence.begin()},
                {},
                {},
                {},
        };
        int[][] possibleInsertions1 = new int[][] {
                {},
                {},
                {},
                {},
        };
        int[] scheduled1 = new int[] {};
        int[] possible1= new int[] {0};
        int[] excluded1 = new int[] {1, 2, 3};

        SequenceVarTest.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
        sm.saveState();
        sequence.schedule(0, sequence.begin());
        cp.fixPoint();

        int[][] scheduledInsertion2 = new int[][] {
                {},
                {},
                {},
                {},
        };
        int[] scheduled2 = new int[] {0};
        int[] possible2= new int[] {};
        assertEquals(4, time[0].min());
        assertEquals(10, time[0].max());
        assertEquals(100, time[5].min()); // end node is not affected by the changes in this case
        assertEquals(200, time[5].max());
        SequenceVarTest.isSequenceValid(sequence, scheduled2, possible2, excluded1, scheduledInsertion2, possibleInsertions1);
        sm.restoreState();
        SequenceVarTest.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
    }

    @Test
    public void testOneNodeUnreachable() {
        IntVar[] time = new IntVar[] {
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 12, 16),
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 4, 7), // node 3 unreachable from the sequence (distance from start: 8)
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 100, 200),
        };
        cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));

        int[][] scheduledInsertion1 = new int[][] {
                {sequence.begin()},
                {sequence.begin()},
                {sequence.begin()},
                {},
        };
        int[][] possibleInsertions1 = new int[][] {
                {  1, 2},
                {0,   2},
                {0,    }, // node 1 cannot be predecessor because of time window violation
                {},
        };
        int[] scheduled1 = new int[] {};
        int[] possible1= new int[] {0, 1, 2};
        int[] excluded1 = new int[] {3};

        SequenceVarTest.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
        sm.saveState();
        sequence.schedule(0, sequence.begin());
        sequence.schedule(2, 0);
        cp.fixPoint();
        // node 1 is now unreachable
        int[][] scheduledInsertion2 = new int[][] {
                {},
                {},
                {},
                {},
        };
        int[][] possibleInsertions2 = new int[][] {
                {},
                {},
                {},
                {},
        };
        int[] scheduled2 = new int[] {0, 2};
        int[] possible2 = new int[] {};
        int[] excluded2 = new int[] {1, 3};

        /* sequence at this point: begin -> 0 -> 2 -> 5
        initial time windows:
        begin:  0..20
        0:      0..20
        2:      0..20
        end:    100..200
         */

        // check for updates in time windows
        assertEquals(0, time[begin].min()); // min departure time remains unchanged
        assertEquals(4, time[0].min());
        assertEquals(14, time[2].min());
        assertEquals(100, time[end].min()); // end node is not affected by the changes in this case

        assertEquals(200, time[end].max());
        assertEquals(20, time[2].max());
        assertEquals(10, time[0].max()); // reduced as node 2 must be reachable from here
        assertEquals(6, time[begin].max()); // max departure time must allow reaching node 0

        // excluded nodes should not have their time window updated
        assertEquals(12, time[1].min());
        assertEquals(16, time[1].max());
        assertEquals(4, time[3].min());
        assertEquals(7, time[3].max());

        SequenceVarTest.isSequenceValid(sequence, scheduled2, possible2, excluded2, scheduledInsertion2, possibleInsertions2);
        sm.restoreState();
        SequenceVarTest.isSequenceValid(sequence, scheduled1, possible1, excluded1, scheduledInsertion1, possibleInsertions1);
    }

    // assign a route that would violate the transition time
    @Test
    public void testUnfeasibleTransitions() {
        IntVar[] time = new IntVar[] {
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 12, 16),
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 4, 7), // node 3 unreachable from the sequence (distance from start: 8)
                makeIntVar(cp, 0, 20),
                makeIntVar(cp, 100, 200),
        };
        sequence.schedule(3, begin);
        try {
            cp.post(new TransitionTimes(sequence, time, transitions, serviceTime));
            fail("should fail");
        } catch (InconsistencyException e) {}
    }

}
