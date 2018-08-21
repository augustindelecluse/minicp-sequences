package minicp.state;

import minicp.engine.core.MiniCP;
import minicp.engine.core.Solver;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public abstract class StateManagerTest {

    @Parameterized.Parameters
    public static Supplier<StateManager>[] data() {
        return new Supplier[]{
                () -> new Trailer(),
                () -> new Copier(),
        };
    }


    @Parameterized.Parameter
    public Supplier<StateManager> stateFactory;
}