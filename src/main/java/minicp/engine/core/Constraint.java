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
     * API to process a constraint.
     * This involves resetting any attribute prior to propagation
     * and propagating the constraint (if it makes sense).
     */
    void process();

    /**
     * API to discard a constraint from the scheduled set.
     * This happens when the propagation queue must be cleared after a failure has
     * occurred.
     */
    void discard();
}
