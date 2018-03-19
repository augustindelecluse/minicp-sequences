package xcsp3;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public abstract class XCSP3TestHelper {
    private String path;

    public XCSP3TestHelper(String path) {
        this.path = path;
    }

    @Test
    public void testInstance() throws Exception {
        try {
            System.out.println(path);
            XCSP3 xcsp3 = new XCSP3(path);
            String solution = xcsp3.solve();

            boolean shouldBeSat = !path.contains("unsat");
            if(shouldBeSat) {
                List<String> violatedCtrs = xcsp3.getViolatedCtrs(solution);
                assertTrue(violatedCtrs.isEmpty());
            }
            else {
                assertTrue(solution.equals(""));
            }
        }
        catch (IllegalArgumentException e) {
            Assume.assumeNoException(e);
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
