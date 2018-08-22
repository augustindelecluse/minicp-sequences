package minicp.engine.constraints;

import minicp.engine.core.AbstractConstraint;
import minicp.engine.core.IntVar;

public class Element1DVar extends AbstractConstraint {

    private final IntVar[] array;
    private final IntVar y;
    private final IntVar z;

    private final int[] yValues;

    private IntVar supMin;
    private IntVar supMax;
    private int zMin;
    private int zMax;

    public Element1DVar(IntVar[] array, IntVar y, IntVar z) {
        super(y.getSolver());
        this.array = array;
        this.y = y;
        this.z = z;
        yValues = new int[y.getSize()];
    }

    @Override
    public void post() {
        y.removeBelow(0);
        y.removeAbove(array.length - 1);

        for (IntVar t : array) {
            t.propagateOnBoundChange(this);
        }
        y.propagateOnDomainChange(this);
        z.propagateOnBoundChange(this);

        propagate();
    }

    @Override
    public void propagate() {
        zMin = z.getMin();
        zMax = z.getMax();
        if (y.isBound()) equalityPropagate();
        else {
            filterY();
            if (y.isBound())
                equalityPropagate();
            else {
                z.removeBelow(supMin.getMin());
                z.removeAbove(supMax.getMax());
            }
        }
    }

    private void equalityPropagate() {
        int id = y.getMin();
        IntVar tVar = array[id];
        tVar.removeBelow(zMin);
        tVar.removeAbove(zMax);
        z.removeBelow(tVar.getMin());
        z.removeAbove(tVar.getMax());
    }

    private void filterY() {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        int i = y.fillArray(yValues);
        while (i > 0) {
            i -= 1;
            int id = yValues[i];
            IntVar tVar = array[id];
            int tMin = tVar.getMin();
            int tMax = tVar.getMax();
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
}
