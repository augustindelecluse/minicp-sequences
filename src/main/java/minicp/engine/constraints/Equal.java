package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;

public class Equal extends AbstractConstraint {
    private final IntVar x, y;

    /**
     * Creates a constraint such
     * that {@code x = y + c}
     *
     * @param x the left member
     * @param y the right memer
     * @see minicp.cp.Factory#equal(IntVar, IntVar)
     */
    public Equal(IntVar x, IntVar y) { // x == y
        super(x.getSolver());
        this.x = x;
        this.y = y;
    }

    @Override
    public void post() {
        if (y.isBound())
            x.assign(y.min());
        else if (x.isBound())
            y.assign(x.min());
        else {
            boundsIntersect();
            for (int k = x.min(); k <= x.max(); k++)
                if (!x.contains(k))
                    y.remove(k);
            for (int k = y.min(); k <= y.max(); k++)
                if (!y.contains(k))
                    x.remove(k);
            x.whenDomainChange(() -> {
                boundsIntersect();
                if (x.isBound())
                    y.assign(x.min());
                else
                    for (int k = x.min(); k <= x.max(); k++)
                        if (!x.contains(k))
                            y.remove(k);
            });
            y.whenDomainChange(() -> {
                boundsIntersect();
                if (y.isBound())
                    x.assign(x.min());
                else
                    for (int k = y.min(); k <= y.max(); k++)
                        if (!y.contains(k))
                            x.remove(k);
            });
        }
    }
    private void boundsIntersect() {
        int newMin = Math.max(x.min(),y.min());
        int newMax = Math.min(x.max(),y.max());
        x.removeBelow(newMin);
        x.removeAbove(newMax);
        y.removeBelow(newMin);
        y.removeAbove(newMax);
    }
}
