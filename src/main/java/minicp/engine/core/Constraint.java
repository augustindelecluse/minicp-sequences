package minicp.engine.core;

public interface Constraint {
    /**
     * API to initialize the internal of a constraint when it is added to the solver.
     */
    default void post() {};

    /**
     * API to propagate the constraint when at least one variable has changed (AC3)
     */
    default void propagate() {};

}
