package xcsp3;

import minicp.engine.constraints.Minimize;
import minicp.engine.constraints.TableCT;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import static minicp.cp.Factory.*;

import minicp.util.InconsistencyException;
import org.w3c.dom.Document;
import org.xcsp.common.Types;
import org.xcsp.common.Utilities;
import org.xcsp.parser.XCallbacks;
import org.xcsp.parser.XCallbacks2;
import org.xcsp.parser.XParser;
import org.xcsp.parser.entries.XVariables;
import org.xcsp.parser.entries.XVariables.*;

import java.lang.reflect.Array;
import java.util.*;

public class XCSP3 implements XCallbacks2 {

    private Implem implem = new Implem(this);

    private Map<XVarInteger, IntVar> mapVar = new HashMap<XVarInteger, IntVar>();

    private Solver minicp = new Solver();


    @Override
    public Implem implem() {
        return implem;
    }
    public XCSP3(String fileName) throws Exception {
        //Document doc = Utilities.loadDocument(fileName);
        //XParser parser = new XParser(doc);
        //parser.vEntries.stream().forEach(e -> System.out.println(e));
        //parser.cEntries.stream().forEach(e -> System.out.println(e));
        //parser.oEntries.stream().forEach(e -> System.out.println(e));

        loadInstance(fileName);
    }

    @Override
    public void buildVarInteger(XVarInteger x, int minValue, int maxValue) {
        IntVar x_ = makeIntVar(minicp,minValue,maxValue);
        mapVar.put(x,x_);
    }

    @Override
    public void buildVarInteger(XVarInteger x, int[] values) {
        Set<Integer> vals = new LinkedHashSet<Integer>();
        for (int v: values) vals.add(v);
        IntVar x_ = makeIntVar(minicp,vals);
        mapVar.put(x,x_);
    }

    private IntVar trVar(Object x) {
        return mapVar.get((XVarInteger) x);
    }

    private IntVar[] trVars(Object vars) {
        return Arrays.stream((XVarInteger[]) vars).map(x -> mapVar.get(x)).toArray(IntVar[]::new);
    }

    private IntVar[][] trVars2D(Object vars) {
        return Arrays.stream((XVarInteger[][]) vars).map(t -> trVars(t)).toArray(IntVar[][]::new);
    }

    @Override
    public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive, Set<Types.TypeFlag> flags) {
        if (flags.contains(Types.TypeFlag.STARRED_TUPLES.STARRED_TUPLES)) {
            // Can you manage short tables ? i.e., tables with tuples containing symbol * ?
            // If not, throw an exception.
            throw new IllegalArgumentException("short table not supported");

        }
        if (flags.contains(Types.TypeFlag.STARRED_TUPLES.UNCLEAN_TUPLES)) {
            // You have possibly to clean tuples here, in order to remove invalid tuples.
            // A tuple is invalid if it contains a value $a$ for a variable $x$, not present in $dom(x)$
            // Note that most of the time, tuples are already cleaned by the parser
        }
        try {
            System.out.println("table");
            if (!positive) throw new IllegalArgumentException("negative table not supported");
            minicp.post(new TableCT(trVars(list), tuples));
        } catch (InconsistencyException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void buildObjToMinimize(String id, XVarInteger x) {
        //minicp.post(new Minimize(x,dfs));
    }

    public static void main(String[] args) {
        try {
            XCSP3 xcsp3 = new XCSP3("data/xcsp3/testExtension1.xml");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
