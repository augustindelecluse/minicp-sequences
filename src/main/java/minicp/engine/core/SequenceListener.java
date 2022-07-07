package minicp.engine.core;

public interface SequenceListener {

    /**
     * Called whenever no possible insertion remain
     */
    void bind();

    /**
     * Called whenever a possible insertion has been inserted into the sequence
     */
    void insert();

    /**
     * called whenever a possible insertion has been removed from the sequence
     */
    void exclude();
}
