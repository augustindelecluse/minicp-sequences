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
            x.removeBelow(y.min());
            x.removeAbove(y.max());
            y.removeBelow(x.min());
            y.removeAbove(x.max());
            final int lx = x.min(), ux = x.max();
            for (int k = lx; k <= ux; k++)
                if (!x.contains(k))
                    y.remove(k);
            final int ly = y.min(), uy = y.max();
            for (int k = ly; k <= uy; k++)
                if (!y.contains(k))
                    x.remove(k);
            x.whenDomainChange(() -> {
                if (x.isBound()) y.assign(x.min());
                for (int k = y.min(); k <= y.max(); k++)
                    if (!x.contains(k))
                        y.remove(k);
            });
            y.whenDomainChange(() -> {
                if (y.isBound()) x.assign(x.min());
                for (int k = x.min(); k <= x.max(); k++)
                    if (!y.contains(k))
                        x.remove(k);
            });
        }
    }
}
