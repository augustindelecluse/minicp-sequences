package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;

public class Equal extends AbstractConstraint {
    private final IntVar x, y;
    private final int c;

    /**
     * Creates a constraint such
     * that {@code x != y + c}
     *
     * @param x the left member
     * @param y the right memer
     * @param c the offset value on y
     * @see minicp.cp.Factory#Equal(IntVar, IntVar, int)
     */
    public Equal(IntVar x, IntVar y, int c) { // x == y + c
        super(x.getSolver());
        this.x = x;
        this.y = y;
        this.c = c;
    }

    @Override
    public void post() {
        if (y.isBound())
            x.assign(y.min() + c);
        else if (x.isBound())
            y.assign(x.min() - c);
        else {
            x.removeBelow(y.min() + c);
            x.removeAbove(y.max() + c);
            y.removeBelow(x.min() - c);
            y.removeAbove(x.max() - c);
            final int lx = x.min(), ux = x.max();
            for (int k = lx; k <= ux; k++)
                if (!x.contains(k))
                    y.remove(k - c);
            final int ly = y.min(), uy = y.max();
            for (int k = ly; k <= uy; k++)
                if (!y.contains(k))
                    x.remove(k + c);
            x.whenDomainChange(() -> {
                for (int k = x.min(); k <= x.max(); k++)
                    if (!x.contains(k))
                        y.remove(k - c);
            });
            y.whenDomainChange(() -> {
                for (int k = y.min(); k <= y.max(); k++)
                    if (!y.contains(k))
                        x.remove(k + c);
            });
        }
    }
}