package minicp.examples;

import com.github.guillaumederval.javagrading.Grade;
import com.github.guillaumederval.javagrading.GradeClass;
import com.github.guillaumederval.javagrading.GradingRunner;
import minicp.util.DataPermissionFactory;
import minicp.util.NotImplementedExceptionAssume;
import minicp.util.exception.NotImplementedException;
import minicp.util.io.InputReader;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FilePermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(GradingRunner.class)
@GradeClass(totalValue = 1, defaultCpuTimeout = 5000)
public class QAPCustomBranchingTest  {
    @Test
    @Grade(customPermissions = DataPermissionFactory.class)
    public void simpleTest() {
        /*
        // @Guillaume: This is test is awful
        try {
            InputReader reader = new InputReader("data/qap.txt");

            int n = reader.getInt();
            // Weights
            int[][] w = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    w[i][j] = reader.getInt();
                }
            }
            // Distance
            int[][] d = new int[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    d[i][j] = reader.getInt();
                }
            }

            List<Integer> solutions = QAP.solve(n, w, d, false, stats -> stats.numberOfNodes() > 19400);
            assertEquals((int) solutions.get(solutions.size() - 1), 9552);
        }
        catch (NotImplementedException e) {
            NotImplementedExceptionAssume.fail(e);
        }*/
    }
}
