package minicp.engine.core;

import minicp.util.Procedure;

/**
 * used to represent a node and its possible insertions points
 * an insertion point is defined by an integer i
 *   and is supposed to be valid if the node can begin after node i
 *   invalid insertion points are removed using constraints
 *   an insertion var can have an empty domain and still be valid. However, a constraint can be added to throw an
 *      inconsistency whenever an insertion var is empty
 */
public interface InsertionVar {

    /**
     * Returns the solver in which this variable was created.
     *
     * @return the solver in which this variable was created
     */
    Solver getSolver();

    /**
     * @return true when the no insertions points exist anymore
     */
    boolean isBound();

    /**
     * remove an insertion point from the set of possible insertions points
     * @param i predecessor candidate for the beginning of the request
     */
    void removeInsert(int i);

    /**
     * remove all insertion points
     */
    void removeAllInsert();

    /**
     * remove all insertion points except the insertion point specified
     * @param i predecessor for the beginning of the request
     */
    void removeAllInsertBut(int i);

    /**
     * tell if the insertion belongs to the set of insertions points
     * @param i
     * @return
     */
    boolean contains(int i);

    /**
     * id of the node
     * @return id of the node
     */
    int node();

    /**
     * Copies the values of the insertions points into an array.
     * each entry of the array contains a valid predecessor
     *
     * @param dest an array large enough {@code dest.length >= size() && dest[0,...,size-1].length >= 2}
     * @return the size of the domain and {@code dest[0,...,size-1]} contains
     *         the values in the domain in an arbitrary order
     */
    int fillInsertions(int[] dest);

    /**
     * @return number of possible insertions points for the request
     */
    int size();

    /**
     * Asks that the closure is called whenever the domain
     * of this variable is reduced to a single setValue
     *
     * @param f the closure
     */
    void whenInsert(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable is reduced to a singleton.
     * In such a state the variable is bind and we say that a <i>bind</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bind events of this variable.
     */
    void propagateOnInsert(Constraint c);

    /**
     * Asks that the closure is called whenever the domain change
     * of this variable changes
     *
     * @param f the closure
     */
    void whenDomainChange(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the domain
     * of this variable changes.
     * We say that a <i>change</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on change events of this variable.
     */
    void propagateOnDomainChange(Constraint c);

    /**
     * Asks that the closure is called whenever the insertion point is excluded
     *
     * @param f the closure
     */
    void whenExclude(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} is called whenever the insertion point is excluded
     * We say that a <i>exclude</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on change events of this variable.
     */
    void propagateOnExclude(Constraint c);

    /**
     * Asks that the closure is called whenever no inserted point remained for this variable
     * this occurs when the insertion variable is either inserted or excluded
     *
     * @param f the closure
     */
    void whenBind(Procedure f);

    /**
     * Asks that {@link Constraint#propagate()} no inserted point remained for this variable
     * this occurs when the insertion variable is either inserted or excluded
     * In such a state the variable is bind and we say that a <i>bind</i> event occurs.
     *
     * @param c the constraint for which the {@link Constraint#propagate()}
     *          method should be called on bind events of this variable.
     */
    void propagateOnBind(Constraint c);

}
