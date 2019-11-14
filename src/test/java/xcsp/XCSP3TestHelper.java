/*
 * mini-cp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License  v3
 * as published by the Free Software Foundation.
 *
 * mini-cp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY.
 * See the GNU Lesser General Public License  for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with mini-cp. If not, see http://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 * Copyright (c)  2018. by Laurent Michel, Pierre Schaus, Pascal Van Hentenryck
 */

package xcsp;

import com.github.guillaumederval.javagrading.Grade;
import com.github.guillaumederval.javagrading.GradeClass;
import minicp.util.DataPermissionFactory;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import org.junit.Assume;
import org.junit.Test;

import java.io.File;
import java.io.FilePermission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

//@GradeClass(totalValue=10.0, defaultCpuTimeout = 20000)
public abstract class XCSP3TestHelper {
    private String path;

    public XCSP3TestHelper(String path) {
        this.path = path;
    }

    @Test
    //@Grade(customPermissions = DataPermissionFactory.class)
    public void testInstance() throws Exception {
        boolean shouldBeSat = !path.contains("unsat");
        try {
            System.out.println(path);
            XCSP xcsp3 = new XCSP(path);
            String solution = xcsp3.solve(1, 5);

            if (shouldBeSat) {
                List<String> violatedCtrs = xcsp3.getViolatedCtrs(solution);
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
