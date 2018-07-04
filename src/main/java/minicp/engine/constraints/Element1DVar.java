package minicp.engine.constraints;

import minicp.engine.core.Constraint;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;

public class Element1DVar implements Constraint {

    private final Solver cp;
    private final IntVar[] T;
    private final IntVar y;
    private final IntVar z;

    private final int [] yValues;

    IntVar supMin;
    IntVar supMax;
    int zMin;
    int zMax;

    public Element1DVar(IntVar[] T, IntVar y, IntVar z) {
        this.cp = y.getSolver();
        this.T = T;
        this.y = y;
        this.z = z;
        yValues = new int[y.getSize()];
    }

    @Override
    public void post()  {
        y.removeBelow(0);
        y.removeAbove(T.length - 1);

        for (IntVar t: T) {
            t.propagateOnBoundChange(this);
        }
        y.propagateOnDomainChange(this);
        z.propagateOnBoundChange(this);

        propagate();
    }

    @Override
    public void propagate()  {
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

    private void equalityPropagate()  {
        int id = y.getMin();
        IntVar tVar = T[id];
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
            IntVar tVar = T[id];
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
