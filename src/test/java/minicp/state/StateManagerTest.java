package minicp.state;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

@RunWith(Parameterized.class)
public abstract class StateManagerTest {

    @Parameterized.Parameters
    public static Object[] data() {
        return new Object[]{
                new Supplier<StateManager>() {
                    @Override
                    public StateManager get() {
                        return new Trailer();
                    }
                }, new Supplier<StateManager>() {
            @Override
            public StateManager get() {
                return new Copier();
            }
        }};
    }


    @Parameterized.Parameter
    public Supplier<StateManager> stateFactory;
}