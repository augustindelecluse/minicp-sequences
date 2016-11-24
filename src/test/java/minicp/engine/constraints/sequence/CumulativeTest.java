package minicp.engine.constraints.sequence;

import minicp.engine.SolverTest;
import minicp.engine.core.SequenceVar;
import minicp.engine.core.SequenceVarImpl;
import minicp.engine.core.Solver;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class CumulativeTest extends SolverTest {

    Solver cp;
    StateManager sm;
    SequenceVar sequence;
    static int nNodes;
    static int begin;
    static int end;

    @BeforeClass
    public static void SetUpClass() {
        nNodes = 8;
        begin = 8;
        end = 9;
    }

    @Before
    public void SetUp() {
        cp = solverFactory.get();
        sm = cp.getStateManager();
        sequence = new SequenceVarImpl(cp, nNodes, begin, end);
    }

    @Test
    public void testInitCumulative2() {
        int[] capacity = new int[] {1, 1, -1, -1, 0, 0, 0, 0, 0, 0};
        try {
            cp.post(new Cumulative(sequence, new int[] {0, 1}, new int[] {2, 3}, 2, capacity));
        } catch (InconsistencyException e) {
            fail("should not fail");
        }
    }

    /**
     * test if the existing scheduled insertions points are truly insertable
     */
    /*
    @Test
    public void testPerformInsert() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 3, capacity));
        int[] possible = new int[sequence.nNodes() + 2];
        int size = sequence.fillPossible(possible);
        for (int i = 0; i < size ; ++i) {
            recursivePerformInsert(possible[i], "", p, d);
        }
    }

     */

    private String replaceByRequest(String init, int[] pickup, int[] drop) {
        for (int i = 0 ; i < pickup.length ; ++i)
            init = init.replace(Integer.toString(pickup[i]), String.format("p%d", i)).replace(Integer.toString(drop[i]), String.format("d%d", i));
        return init.replace(Integer.toString(begin), "begin").replace(Integer.toString(end), "end");
    }

    public void recursivePerformInsert(int node, String description, int[] p, int[] d) {
        int[] insertions = new int[sequence.nNodes() + 2];
        int size = sequence.fillScheduledInsertions(node, insertions);
        for (int i = 0; i < size ; ++i) {
            sm.saveState();
            try {
                //if (sequence.toString().equals("8 -> 2 -> 0 -> 4 -> 3 -> 1 -> 7 -> 6 -> 9") && node == 5 && insertions[i] == 1)
                //    System.out.println("hi");
                cp.post(new Schedule(sequence, node, insertions[i]));
                //if (sequence.toString().equals("8 -> 2 -> 4 -> 3 -> 1 -> 7 -> 5 -> 9"))
                //    System.out.println("hi");
                int[] possible = new int[sequence.nPossibleNode()];
                int size2 = sequence.fillPossible(possible);
                for (int j = 0; j < size2 ; ++j) {
                    String newDescription = replaceByRequest(sequence.toString(), p, d);
                    recursivePerformInsert(possible[j], description + "\n" + newDescription, p, d);
                }
            } catch (InconsistencyException e) {
                String nodeStr = node < p.length ? String.format("p%d", node) : String.format("d%d", node-p.length);
                String insertionStr = insertions[i] < p.length ? String.format("p%d", insertions[i]) : String.format("d%d", insertions[i]-p.length);
                fail(String.format("scheduled insertion at a node produced an inconsistency\n%s\ntrying to insert %s after %s\n%s",
                        replaceByRequest(sequence.toString(), p, d), nodeStr, insertionStr, description));
            }
            sm.restoreState();
        }
    }

    @Test
    public void testFeasibleSequence() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Schedule(sequence, 2, sequence.begin()));
        cp.post(new Schedule(sequence, 4, 2));
        cp.post(new Schedule(sequence, 3, 4));
        cp.post(new Schedule(sequence, 1, 3));
        cp.post(new Schedule(sequence, 7, 1));
        cp.post(new Schedule(sequence, 5, 7));
    }

    @Test
    public void testNoInsertForStart() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 3, capacity));
        cp.post(new Schedule(sequence, p[0], sequence.begin()));
        cp.post(new Schedule(sequence, p[2], p[0]));
        cp.post(new Schedule(sequence, d[2], p[2]));
        cp.post(new Schedule(sequence, d[1], d[2]));
        // sequence at this point: begin -> p0   -> p2   -> d2   -> d1   -> end
        // capacity:                0, 0    0, 2    0, 1    1, 0    2, 0    0, 0
        assertTrue(sequence.isInsertion(p[1], p[0]));
        assertTrue(sequence.isInsertion(p[3], p[0]));
        assertTrue(sequence.isInsertion(d[3], p[0]));
    }

    /**
     * test if an activity with no inserted part has some insertions points removed
     */
    @Test
    public void testNotInsertedActivity() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Schedule(sequence, 2, sequence.begin()));
        cp.post(new Schedule(sequence, 0, 2));
        cp.post(new Schedule(sequence, 4, 0));
        cp.post(new Schedule(sequence,  6, 4)); // sequence: begin -> 2 -> 0 -> 4 -> 6 -> end
        int start = 1;
        int end = 5;
        assertTrue(sequence.isInsertion(start, sequence.begin()));
        assertTrue(sequence.isInsertion(start, 2));
        assertFalse(sequence.isInsertion(start, 0));
        assertTrue(sequence.isInsertion(start, 4));
        assertTrue(sequence.isInsertion(start, 6));

        assertTrue(sequence.isInsertion(end, sequence.begin()));
        assertTrue(sequence.isInsertion(end, 2));
        assertFalse(sequence.isInsertion(end, 0));
        assertTrue(sequence.isInsertion(end, 4));
        assertTrue(sequence.isInsertion(end, 6));
    }

    @Test
    public void testUpdateEnd() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Schedule(sequence, 1, sequence.begin()));
        cp.post(new Schedule(sequence, 2, 1));
        cp.post(new Schedule(sequence, 0, 2));
        cp.post(new Schedule(sequence, 4, 0));
        cp.post(new Schedule(sequence, 6, 4)); // sequence: begin -> 1 -> 2 -> 0 -> 4 -> 6 -> end

        int end = 5; // end for start == 1
        assertFalse(sequence.isInsertion(end, sequence.begin())); // cannot schedule end before start
        assertTrue(sequence.isInsertion(end, 1));
        assertTrue(sequence.isInsertion(end, 2));
        assertFalse(sequence.isInsertion(end, 0)); // from this point, the end exceeds the max capacity
        assertFalse(sequence.isInsertion(end, 4));
        assertFalse(sequence.isInsertion(end, 6));
    }

    @Test
    public void testRemoveIntermediate() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Schedule(sequence, 2, sequence.begin()));
        cp.post(new Schedule(sequence, 0, 2));
        cp.post(new Schedule(sequence, 4, 0));
        cp.post(new Schedule(sequence, 3, 4));
        cp.post(new Schedule(sequence, 7, 3));
        cp.post(new Schedule(sequence, 6, 7)); // sequence: begin -> 2 -> 0 -> 4 -> 3 -> 7 -> 6 -> end

        int start = 1;
        int end = 5;
        assertTrue(sequence.isInsertion(start, sequence.begin()));
        assertTrue(sequence.isInsertion(start, 2));  // capacity: 1
        assertFalse(sequence.isInsertion(start, 0)); // capacity: 3
        assertTrue(sequence.isInsertion(start, 4));  // capacity: 1
        assertFalse(sequence.isInsertion(start, 3)); // capacity: 2
        assertTrue(sequence.isInsertion(start, 7));  // capacity: 1
        assertTrue(sequence.isInsertion(start, 6));  // capacity: 0

        assertTrue(sequence.isInsertion(end, sequence.begin()));
        assertTrue(sequence.isInsertion(end, 2));    // capacity: 1
        assertFalse(sequence.isInsertion(end, 0));   // capacity: 3
        assertTrue(sequence.isInsertion(end, 4));    // capacity: 1
        assertFalse(sequence.isInsertion(end, 3));   // capacity: 2
        assertTrue(sequence.isInsertion(end, 7));    // capacity: 1
        assertTrue(sequence.isInsertion(end, 6));    // capacity: 0

        try {
            cp.post(new Schedule(sequence, 1, 3));
            fail();
        } catch (InconsistencyException e) {
            ;
        }
    }

    @Test
    public void testPartiallyInsert() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        cp.post(new Cumulative(sequence, new int[] {0, 1, 2, 3}, new int[] {4, 5, 6, 7}, 3, capacity));
        cp.post(new Schedule(sequence, 2, sequence.begin()));
        cp.post(new Schedule(sequence, 3, 2));
        cp.post(new Schedule(sequence, 4, 2));
        cp.post(new Schedule(sequence, 5, 2));
        cp.post(new Schedule(sequence, 6, 2));
        cp.post(new Schedule(sequence, 7, 3)); // sequence: begin -> 2 -> 6 -> 5 -> 4 -> 3 -> 7 -> end
    }

    @Test
    public void removeDropInsertion() {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        Cumulative Cumulative = new Cumulative(sequence, p, d, 2, capacity);
        cp.post(Cumulative);
        cp.post(new Schedule(sequence, p[0], sequence.begin()));
        cp.post(new Schedule(sequence, p[1], p[0]));
        cp.post(new Schedule(sequence, p[2], p[1]));
        cp.post(new Schedule(sequence, d[0], p[2]));
        // sequence:   begin -> 0 (p0) -> 1 (p1) -> 2 (p2) -> 4 (d0) -> end
        // capacity:   0, 0     0, 1      1, 2      1, 2      1, 0
        // partially inserted
        assertFalse(sequence.isInsertion(d[1], p[2]));
        assertTrue(sequence.isInsertion(d[1], p[1]));

        // not inserted: pickup
        assertTrue(sequence.isInsertion(p[3], sequence.begin()));
        assertTrue(sequence.isInsertion(p[3], p[0]));
        assertTrue(sequence.isInsertion(p[3], p[1]));
        assertTrue(sequence.isInsertion(p[3], p[2]));
        assertTrue(sequence.isInsertion(p[3], d[0]));

        // not inserted: drop
        assertTrue(sequence.isInsertion(d[3], sequence.begin()));
        assertTrue(sequence.isInsertion(d[3], p[0]));
        assertTrue(sequence.isInsertion(d[3], p[1]));
        assertTrue(sequence.isInsertion(d[3], p[2]));
        assertTrue(sequence.isInsertion(d[3], d[0]));
    }


    @Test
    public void removeNotInserted() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        Cumulative Cumulative = new Cumulative(sequence, p, d, 3, capacity);
        cp.post(Cumulative);
        cp.post(new Schedule(sequence, p[0], sequence.begin()));
        cp.post(new Schedule(sequence, p[2], p[0]));
        cp.post(new Schedule(sequence, d[2], p[2]));
        cp.post(new Schedule(sequence, d[0], d[2]));
        // sequence:   begin -> (p0) -> (p2) -> 2 (d2) -> 4 (d0) -> end
        // sequence: capacity:   2       3         2         0
        // not inserted: drop
        assertTrue(sequence.isInsertion(d[1], sequence.begin()));
        assertFalse(sequence.isInsertion(d[1], p[0]));
        assertFalse(sequence.isInsertion(d[1], p[2]));
        assertFalse(sequence.isInsertion(d[1], d[2]));
        assertTrue(sequence.isInsertion(d[1], d[0]));
        // not inserted: pickup
        assertTrue(sequence.isInsertion(p[1], sequence.begin()));
        assertFalse(sequence.isInsertion(p[1], p[0]));
        assertFalse(sequence.isInsertion(p[1], p[2]));
        assertFalse(sequence.isInsertion(p[1], d[2]));
        assertTrue(sequence.isInsertion(p[1], d[0]));
    }

    @Test
    public void SeveralPartiallyInserted() {
        int[] capacity = new int[] {2, 2, 1, 1, -2, -2, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 3, capacity));
        cp.post(new Schedule(sequence, p[2], sequence.begin()));
        cp.post(new Schedule(sequence, p[3], p[2]));
        cp.post(new Schedule(sequence, d[0], p[2]));
        cp.post(new Schedule(sequence, d[1], p[3]));
        cp.post(new Schedule(sequence, d[2], d[1])); // begin -> p2 -> d0 -> p3 -> d1 -> d2 -> end
        cp.post(new Schedule(sequence, p[0], p[2])); // begin -> p2 -> p0 -> d0 -> p3 -> d1 -> d2 -> end
        int a = 0;
    }

    @Test
    public void MultipleDropPartiallyInserted() {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Schedule(sequence, d[0], sequence.begin()));
        cp.post(new Schedule(sequence, d[1], d[0]));
        cp.post(new Schedule(sequence, d[2], d[1]));
        cp.post(new Schedule(sequence, d[3], d[2])); // begin -> d0 -> d1 -> d2 -> d3 -> end
        assertFalse(sequence.isInsertion(p[1], sequence.begin()));
        assertFalse(sequence.isInsertion(p[2], sequence.begin()));
        assertFalse(sequence.isInsertion(p[2], d[0]));
        assertFalse(sequence.isInsertion(p[3], sequence.begin()));
        assertFalse(sequence.isInsertion(p[3], d[0]));
        assertFalse(sequence.isInsertion(p[3], d[1]));
    }

    @Test
    public void MultiplePickupPartiallyInserted() {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Schedule(sequence, p[0], sequence.begin()));
        cp.post(new Schedule(sequence, p[1], p[0]));
        cp.post(new Schedule(sequence, p[2], p[1]));
        cp.post(new Schedule(sequence, p[3], p[2])); // begin -> p0 -> p1 -> p2 -> p3 -> end
        assertFalse(sequence.isInsertion(d[0], p[1]));
        assertFalse(sequence.isInsertion(d[1], p[2]));
        assertFalse(sequence.isInsertion(d[2], p[3]));
        assertTrue(sequence.isInsertion(d[0], p[0]));
        assertTrue(sequence.isInsertion(d[1], p[1]));
        assertTrue(sequence.isInsertion(d[2], p[2]));
        assertTrue(sequence.isInsertion(d[3], p[3]));
    }

    @Test
    public void MultiplePartiallyInserted() {
        int[] capacity = new int[] {1, 1, 1, 1, -1, -1, -1, -1, 0, 0};
        int[] p = new int[] {0, 1, 2, 3};
        int[] d = new int[] {4, 5, 6, 7};
        cp.post(new Cumulative(sequence, p, d, 1, capacity));
        cp.post(new Schedule(sequence, d[0], sequence.begin()));
        cp.post(new Schedule(sequence, p[1], d[0]));
        cp.post(new Schedule(sequence, d[2], p[1]));
        cp.post(new Schedule(sequence, p[3], d[2])); // begin -> d0 -> p1 -> d2 -> p3 -> end

        // insertions for p0
        assertTrue(sequence.isInsertion(p[0], sequence.begin()));
        assertFalse(sequence.isInsertion(p[0], d[0]));
        assertFalse(sequence.isInsertion(p[0], p[1]));
        assertFalse(sequence.isInsertion(p[0], d[2]));
        assertFalse(sequence.isInsertion(p[0], p[3]));

        // insertions for d1
        assertFalse(sequence.isInsertion(d[1], sequence.begin()));
        assertFalse(sequence.isInsertion(d[1], d[0]));
        assertTrue(sequence.isInsertion(d[1], p[1]));
        assertFalse(sequence.isInsertion(d[1], d[2]));
        assertFalse(sequence.isInsertion(d[1], p[3]));

        // insertions for p2
        assertFalse(sequence.isInsertion(p[2], sequence.begin()));
        assertFalse(sequence.isInsertion(p[2], d[0]));
        assertTrue(sequence.isInsertion(p[2], p[1]));
        assertFalse(sequence.isInsertion(p[2], d[2]));
        assertFalse(sequence.isInsertion(p[2], p[3]));

        // insertions for d3
        assertFalse(sequence.isInsertion(d[3], sequence.begin()));
        assertFalse(sequence.isInsertion(d[3], d[0]));
        assertFalse(sequence.isInsertion(d[3], p[1]));
        assertFalse(sequence.isInsertion(d[3], d[2]));
        assertTrue(sequence.isInsertion(d[3], p[3]));
    }

}
