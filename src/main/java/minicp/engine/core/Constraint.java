package minicp.engine.core;

import minicp.reversible.RevBool;
import minicp.util.Box;

public interface Constraint {

    /**
     * Initialize the constraint when it is posted to the solver.
     *
     */
    void post();

    /**
     * Propagate the constraint
     */
    void propagate();

    /**
     * Called by the solver when the constraint
     * is enqueued in the propagation queue
     * @param scheduled is true when the constraint is enqueued in the propagation queue,
     *                  false when dequeued,
     */
    void setScheduled(boolean scheduled);

    /**
     * @return the last value given to setScheduled
     */
    boolean isScheduled();

    /**
     * Typically called by the Constraint to ask that it should not be scheduled any more.
     * For instance in case it is subsumed
     * @param active
     */
    void setActive(boolean active);

    /**
     * @return the last value passed in argument of setActive
     */
    boolean isActive();


}
