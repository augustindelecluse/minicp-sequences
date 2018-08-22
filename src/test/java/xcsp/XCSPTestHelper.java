package xcsp;

import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class XCSPTestHelper {
    private String path;

    public XCSPTestHelper(String path) {
        this.path = path;
    }

    @Test
    public void testInstance() throws Exception {
        boolean shouldBeSat = !path.contains("unsat");
        try {
            System.out.println(path);
            XCSP xcsp = new XCSP(path);
            String solution = xcsp.solve(1, 3);

            if (shouldBeSat) {
                List<String> violatedCtrs = xcsp.getViolatedCtrs(solution);
                assertTrue(violatedCtrs.isEmpty());
            } else {
                assertTrue(solution.equals(""));
            }
        } catch (IllegalArgumentException | NotImplementedException e) {
            Assume.assumeNoException(e);
        } catch (InconsistencyException e) {
            assertFalse(shouldBeSat);
        }
    }

    public static Object[] dataFromFolder(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();

        List<Object> out = new LinkedList<>();
        assert listOfFiles != null;
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile()) {
                String name = listOfFile.getAbsolutePath();
                if (name.endsWith(".xml.lzma") || name.endsWith(".xml"))
                    out.add(name);
            }
        }
        return out.toArray();
    }
}
