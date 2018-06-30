package minicp.engine.core;

public interface Constraint {
    /**
     * API to initialize the internal of a constraint when it is added to the solver.
     */
    void post();

    /**
     * API to propagate the constraint when at least one variable has changed (AC3)
     */
    void propagate();

    /**
     * API to retrieve the identifier of the constraint (an integer between 0 and |cstrs|
     * @return the constraint identifier
     */
    int getId();
}
