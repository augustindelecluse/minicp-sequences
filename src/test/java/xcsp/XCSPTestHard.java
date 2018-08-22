package xcsp;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XCSPTestHard extends XCSPTestHelper {
    public XCSPTestHard(String path) {
        super(path);
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return dataFromFolder("data/xcsp3/hard");
    }
}