package minicp.engine.core;

import minicp.state.StateManager;
import minicp.state.StateSequenceSet;

public class SequenceSetDomain implements SequenceDomain {

    private StateSequenceSet domain;

    public SequenceSetDomain(StateManager sm, int n) {
        domain = new StateSequenceSet(sm, n);
    }

    @Override
    public int nbScheduled() {
        return domain.nRequired();
    }

    @Override
    public int nbPossible() {
        return domain.nPossible();
    }

    @Override
    public int nbExcluded() {
        return domain.nExcluded();
    }

    @Override
    public boolean isScheduled(int val) {
        return domain.isRequired(val);
    }

    @Override
    public boolean isPossible(int val) {
        return domain.isPossible(val);
    }

    @Override
    public boolean isExcluded(int val) {
        return domain.isExcluded(val);
    }

    @Override
    public boolean isBound() {
        return domain.nPossible() == 0;
    }

    @Override
    public boolean schedule(int v, SequenceListener l) {
        if (domain.require(v)) {
            l.insert();
            if (domain.nPossible() == 0)
                l.bind();
            return true;
        }
        return false;
    }

    @Override
    public boolean exclude(int v, SequenceListener l) {
        if (domain.exclude(v)) {
            if (domain.nPossible() == 0)
                l.bind();
            return true;
        }
        return false;
    }

    @Override
    public int getScheduled(int[] dest) {
        return domain.getRequired(dest);
    }

    @Override
    public int getPossible(int[] dest) {
        return domain.getPossible(dest);
    }

    @Override
    public int getExcluded(int[] dest) {
        return domain.getExcluded(dest);
    }
}
