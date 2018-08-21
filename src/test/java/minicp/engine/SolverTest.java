package minicp.engine;


import minicp.engine.core.MiniCP;
import minicp.engine.core.Solver;
import minicp.state.Copier;
import minicp.state.StateManager;
import minicp.state.Trailer;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public abstract class SolverTest {

    @Parameterized.Parameters
    public static Supplier<Solver>[] data() {
        return new Supplier[]{
                () -> new MiniCP(new Trailer()),
                () -> new MiniCP(new Copier()),
        };
    }

    @Parameterized.Parameter
    public Supplier<Solver> solverFactory;
}
