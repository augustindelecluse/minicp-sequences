package minicp.engine.constraints.sequence;

import minicp.engine.SolverTest;
import minicp.engine.core.SequenceVar;
import minicp.engine.core.SequenceVarImpl;
import minicp.engine.core.Solver;
import minicp.state.StateManager;
import minicp.util.exception.InconsistencyException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DisjointTest extends SolverTest {

    @Test
    public void testDisjoint1() {
        Solver cp = solverFactory.get();
        StateManager sm = cp.getStateManager();
        int nSequences = 5;
        int nNodes = 10;
        SequenceVar[] sequenceVars = new SequenceVar[nSequences];

        for (int i = 0; i < nSequences ; ++i) {
            sequenceVars[i] = new SequenceVarImpl(cp, nNodes, nNodes+i, nNodes + nSequences + i);
        }
        sm.saveState();
        cp.post(new Disjoint(sequenceVars));
        // no modifications should have occurred at the moment

        sequenceVars[0].schedule(5, sequenceVars[0].begin());
        // node 5 should be excluded from all others sequences
        for (int i = 1; i < nSequences ; ++i) {
            assertFalse(sequenceVars[i].isExcluded(5));
        }

        sequenceVars[1].schedule(2, sequenceVars[1].begin());
        // node 2 should be excluded from all others sequences
        for (int i = 0; i < nSequences ; ++i) {
            if (i != 1)
                assertFalse(sequenceVars[i].isExcluded(2));
        }

        sequenceVars[4].schedule(8, sequenceVars[4].begin());
        // node 2 should be excluded from all others sequences
        for (int i = 0; i < nSequences ; ++i) {
            if (i != 4)
                assertFalse(sequenceVars[i].isExcluded(8));
        }

    }

}
