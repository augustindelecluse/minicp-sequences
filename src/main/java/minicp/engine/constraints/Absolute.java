package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;

public class Absolute extends AbstractConstraint {

    private final IntVar x;
    private final IntVar y;

    /**
     * Build a constraint y = |x|
     *
     * @param x
     * @param y
     */
    public Absolute(IntVar x, IntVar y) {
        super(x.getSolver());
        this.x = x;
        this.y = y;
    }

    public void post() {
        y.removeBelow(0);
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        propagate();
        //we can do more propagation with val remove
    }

    @Override
    public void propagate() {
        // y = |x|

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
    }

}
