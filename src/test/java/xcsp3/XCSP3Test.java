package xcsp3;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class XCSP3Test {
    @Test
    public void testAll() {
        String[] instances = new String[]{
                //"data/xcsp3/easy/Queens-0008-m1.xml",
                //"data/xcsp3/easy/testExtension1.xml",
                //"data/xcsp3/easy/testExtension2.xml",
                //"data/xcsp3/easy/testExtension3.xml",
                //"data/xcsp3/easy/testObjective1.xml",
                //"data/xcsp3/easy/testPrimitive.xml",
                //"data/xcsp3/easy/MagicSquare-3-sum.xml",
                //"data/xcsp3/easy/CostasArray-10.xml",
                "data/xcsp3/easy/CoveringArray-elt-3-04-2-08.xml"
        };

        for (String inst: instances) {
            System.out.println(inst);
            try {
                XCSP3 xcsp3 = new XCSP3(inst);
                String solution = xcsp3.solve();
                List<String> violatedCtrs = xcsp3.getViolatedCtrs(solution);
                assertTrue(violatedCtrs.isEmpty());
            } catch (Exception e) {
                fail("enable to read or solve "+inst);
                e.printStackTrace();
            }

        }

    }
}
