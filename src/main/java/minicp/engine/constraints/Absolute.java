package minicp.engine.constraints;

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.util.InconsistencyException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Absolute extends Constraint {

    private IntVar x;
    private IntVar y;

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

    @Override
    public void post() throws InconsistencyException {
        y.removeBelow(0);
        x.propagateOnBoundChange(this);
        y.propagateOnBoundChange(this);
        propagate();
        //we can do more propagation with val remove
    }

    @Override
    public void propagate() throws InconsistencyException {
        // y = |x|

        if (x.isBound()) {
            y.assign(Math.abs(x.getMin()));
            deactivate();
        } else if (y.isBound()) { // y is bound
            // y = |x|
            if (!x.contains(-y.getMin())) {
                x.assign(y.getMin());
            } else if (!x.contains(y.getMin())) {
                x.assign(-y.getMin());
            } else {
                // x can be (y or -y)
                // remove everything except y and -y from x
                for (int v = x.getMin(); v <= x.getMax(); v++) {
                    if (v != y.getMin() && v != -y.getMin()) {
                        x.remove(v);
                    }
                }
            }
            deactivate();
        } else if (x.getMin() >= 0) {
            y.removeBelow(x.getMin());
            y.removeAbove(x.getMax());
            x.removeBelow(y.getMin());
            x.removeAbove(y.getMax());
        } else if (x.getMax() <= 0) {
            y.removeBelow(-x.getMax());
            y.removeAbove(-x.getMin());
            x.removeBelow(-y.getMax());
            x.removeAbove(-y.getMin());
        } else {
            int maxAbs = Math.max(x.getMax(), -x.getMin());
            y.removeAbove(maxAbs);
            x.removeAbove(y.getMax());
            x.removeBelow(-y.getMax());
            while (!x.contains(y.getMin()) & !x.contains(-y.getMin())) {
                y.remove(y.getMin());
            }
        }
    }

}
