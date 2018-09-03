package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;
import minicp.util.exception.NotImplementedException;

public class Element1DVar extends AbstractConstraint {

    private final IntVar[] array;
    private final IntVar y;
    private final IntVar z;

    // STUDENT
    // BEGIN STRIP
    private final int[] yValues;
    private IntVar supMin;
    private IntVar supMax;
    private int zMin;
    private int zMax;
    // END STRIP

    public Element1DVar(IntVar[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.array = array;
        this.y = y;
        this.z = z;

        // STUDENT
        // BEGIN STRIP
        yValues = new int[y.size()];
        // END STRIP
    }

    @Override
    public void post() {
        // STUDENT throw new NotImplementedException();
        // BEGIN STRIP
        y.removeBelow(0);
        y.removeAbove(array.length - 1);

        for (IntVar t : array) {
            t.propagateOnBoundChange(this);
        }
        y.propagateOnDomainChange(this);
        z.propagateOnBoundChange(this);

        propagate();
        // END STRIP
    }

    @Override
    public void propagate() {
        // STUDENT throw new NotImplementedException();
        // BEGIN STRIP
        zMin = z.min();
        zMax = z.max();
        if (y.isBound()) equalityPropagate();
        else {
            filterY();
            if (y.isBound())
                equalityPropagate();
            else {
                z.removeBelow(supMin.min());
                z.removeAbove(supMax.max());
            }
        }
        // END STRIP

    }

    // STUDENT
    // BEGIN STRIP
    private void equalityPropagate() {
        int id = y.min();
        IntVar tVar = array[id];
        tVar.removeBelow(zMin);
        tVar.removeAbove(zMax);
        z.removeBelow(tVar.min());
        z.removeAbove(tVar.max());
    }

    private void filterY() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        int i = y.fillArray(yValues);
        while (i > 0) {
            i -= 1;
            int id = yValues[i];
            IntVar tVar = array[id];
            int tMin = tVar.min();
            int tMax = tVar.max();
            if (tMax < zMin || tMin > zMax) {
                y.remove(id);
            } else {
                if (tMin < min) {
                    min = tMin;
                    supMin = tVar;
                }
                if (tMax > max) {
                    max = tMax;
                    supMax = tVar;
                }
            }
        }
    }
    // END STRIP

}
