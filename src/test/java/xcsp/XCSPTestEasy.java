package xcsp;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class XCSPTestEasy extends XCSPTestHelper {
    public XCSPTestEasy(String path) {
        super(path);
    }

    @Parameterized.Parameters
    public static Object[] data() {
        return dataFromFolder("data/xcsp3/easy-first-solution");
    }
}
