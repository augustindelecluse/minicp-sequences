package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

/**
 * Absolute value constraint
 */
public class Absolute extends AbstractConstraint {

    private final IntVar x;
    private final IntVar y;

    /**
     * Creates the absolute value constraint {@code y = |x|}.
     *
     * @param x the input variable such that its absolut value is equal to y
     * @param y the variable that represents the absolute value of x
     */
    public Absolute(IntVar x, IntVar y) {
        super(x.getSolver());
        this.x = x;
        this.y = y;
    }

    public void post() {
        // TODO
        // STUDENT throw new NotImplementedException("Absolute");
        // BEGIN STRIP
        y.removeBelow(0);
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        propagate();
        //we can do more propagation with val remove
        // END STRIP
    }

    @Override
    public void propagate() {
        // y = |x|
        // TODO
        // STUDENT throw new NotImplementedException("Absolute");
        // BEGIN STRIP
        if (x.isBound()) {
            y.assign(Math.abs(x.min()));
            setActive(false);
        } else if (y.isBound()) { // y is bound
            // y = |x|
            if (!x.contains(-y.min())) {
                x.assign(y.min());
            } else if (!x.contains(y.min())) {
                x.assign(-y.min());
            } else {
                // x can be (y or -y)
                // remove everything except y and -y from x
                for (int v = x.min(); v <= x.max(); v++) {
                    if (v != y.min() && v != -y.min()) {
                        x.remove(v);
                    }
                }
            }
            setActive(false);
        } else if (x.min() >= 0) {
            y.removeBelow(x.min());
            y.removeAbove(x.max());
            x.removeBelow(y.min());
            x.removeAbove(y.max());
        } else if (x.max() <= 0) {
            y.removeBelow(-x.max());
            y.removeAbove(-x.min());
            x.removeBelow(-y.max());
            x.removeAbove(-y.min());
        } else {
            int maxAbs = Math.max(x.max(), -x.min());
            y.removeAbove(maxAbs);
            x.removeAbove(y.max());
            x.removeBelow(-y.max());
            while (!x.contains(y.min()) & !x.contains(-y.min())) {
                y.remove(y.min());
            }
        }
        // END STRIP
    }

}
