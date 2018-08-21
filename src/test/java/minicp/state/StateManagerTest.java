package minicp.state;

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