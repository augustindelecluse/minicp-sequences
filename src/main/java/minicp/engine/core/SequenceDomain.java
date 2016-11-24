package minicp.engine.core;

public interface SequenceDomain {

    int nbScheduled();

    int nbPossible();

    int nbExcluded();

    boolean isScheduled(int val);

    boolean isPossible(int val);

    boolean isExcluded(int val);

    boolean isBound();

    boolean schedule(int v, SequenceListener l);

    boolean exclude(int v, SequenceListener l);

    int getScheduled(int[] dest);

    int getPossible(int[] dest);

    int getExcluded(int[] dest);

}
