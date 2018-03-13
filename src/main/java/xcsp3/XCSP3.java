package xcsp3;

import minicp.engine.constraints.*;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;

import static minicp.cp.Heuristics.*;
import static minicp.cp.Factory.*;

import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.Box;
import minicp.util.InconsistencyException;
import org.w3c.dom.Document;
import org.xcsp.checker.SolutionChecker;
import org.xcsp.common.Condition;
import org.xcsp.common.Types;
import org.xcsp.common.Utilities;
import org.xcsp.common.predicates.XNodeParent;
import org.xcsp.parser.XCallbacks;
import org.xcsp.parser.XCallbacks2;
import org.xcsp.parser.XParser;
import org.xcsp.parser.entries.XVariables;
import org.xcsp.parser.entries.XVariables.*;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Array;
import java.security.InvalidParameterException;
import java.util.*;

public class XCSP3 implements XCallbacks2 {

    private Implem implem = new Implem(this);

    private String fileName;
    public final Map<XVarInteger, IntVar> mapVar = new HashMap<>();
    public final List<XVarInteger> xVars = new LinkedList<>();
    public final List<IntVar> minicpVars = new LinkedList<>();
    public final Solver minicp = new Solver();

    public Optional<IntVar> objectiveMinimize = Optional.empty();


    @Override
    public Implem implem() {
        return implem;
    }

    public XCSP3(String fileName) throws Exception {
        this.fileName = fileName;
        implem.currParameters.clear();

        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_UNARY_PRIMITIVES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_BINARY_PRIMITIVES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_TERNARY_PRIMITIVES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_NVALUES_CASES, new Object());
        implem.currParameters.put(XCallbacksParameters.CONVERT_INTENSION_TO_EXTENSION_ARITY_LIMIT, 1000); // included
        implem.currParameters.put(XCallbacksParameters.CONVERT_INTENSION_TO_EXTENSION_ARITY_LIMIT, 1000000);

        //Document doc = Utilities.loadDocument(fileName);
        //XParser parser = new XParser(doc);
        //parser.vEntries.stream().forEach(e -> System.out.println(e));
        //parser.cEntries.stream().forEach(e -> System.out.println(e));
        //parser.oEntries.stream().forEach(e -> System.out.println(e));

        loadInstance(fileName);
    }

    public List<String> getViolatedCtrs(String solution) {
        try {
            return new SolutionChecker(false, fileName, new ByteArrayInputStream(solution.getBytes())).violatedCtrs;
        } catch (Exception e) {
            e.printStackTrace();
            return new LinkedList<>();
        }
    }

    @Override
    public void buildVarInteger(XVarInteger x, int minValue, int maxValue) {
        IntVar x_ = makeIntVar(minicp, minValue, maxValue);
        mapVar.put(x, x_);
        minicpVars.add(x_);
        xVars.add(x);
    }

    @Override
    public void buildVarInteger(XVarInteger x, int[] values) {
        Set<Integer> vals = new LinkedHashSet<Integer>();
        for (int v : values) vals.add(v);
        IntVar x_ = makeIntVar(minicp, vals);
        mapVar.put(x, x_);
        minicpVars.add(x_);
        xVars.add(x);
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

    private void relConstraintVal(IntVar x, Types.TypeConditionOperatorRel operator, int k) {
        try {
            switch (operator) {
                case EQ:
                    x.assign( k);
                    break;
                case GE:
                    x.removeBelow( k);
                    break;
                case GT:
                    x.removeBelow(k + 1);
                    break;
                case LE:
                    x.removeAbove(k);
                    break;
                case LT:
                    x.removeAbove(k - 1);
                    break;
                case NE:
                    x.remove(k);
                    break;
                default:
                    throw new InvalidParameterException("unknown condition");
            }
            x.getSolver().fixPoint();
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
    }

    private void relConstraintVar(IntVar x, Types.TypeConditionOperatorRel operator, IntVar y) {
        try {
            switch (operator) {
                case EQ:
                    // TODO: implement equal
                    minicp.post(new LessOrEqual(x, y));
                    minicp.post(new LessOrEqual(y,x));
                    break;
                case GE:
                    minicp.post(new LessOrEqual(y,x));
                    break;
                case GT:
                    minicp.post(new LessOrEqual(minus(y, 1), x));
                    break;
                case LE:
                    minicp.post(new LessOrEqual(x, y));
                    break;
                case LT:
                    minicp.post(new LessOrEqual(minus(x, 1), y));
                    break;
                case NE:
                    minicp.post(notEqual(x,y));
                    break;
                default:
                    throw new InvalidParameterException("unknown condition");
            }
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
    }


    private void _buildCrtWithCondition(String id, IntVar expr, Condition operator) {
        if (operator instanceof Condition.ConditionVal) {
            Condition.ConditionVal op = (Condition.ConditionVal) operator;
            relConstraintVal(expr,op.operator,(int) op.k);
        } else if (operator instanceof Condition.ConditionVar) {
            Condition.ConditionVar op = (Condition.ConditionVar) operator;
            relConstraintVar(expr,op.operator,mapVar.get(op.x));
        } else if (operator instanceof Condition.ConditionIntvl) {
            Condition.ConditionIntvl op = (Condition.ConditionIntvl) operator;
            try {
                switch (op.operator) {
                    case IN:
                        expr.removeAbove((int) op.max);
                        expr.removeBelow((int) op.min);
                        break;
                    case NOTIN:
                        BoolVar le = makeBoolVar(minicp);
                        BoolVar ge = makeBoolVar(minicp);
                        minicp.post(new IsLessOrEqual(le, expr, (int) op.min - 1));
                        minicp.post(new IsLessOrEqual(ge, minus(expr), (int) -op.max - 1));
                        sum(le, ge).removeBelow(1);
                        //minisolIsLessOrEqual(BoolVar b, IntVar x, int c
                        break;
                    default:
                        throw new InvalidParameterException("unknown condition");
                }
                expr.getSolver().fixPoint();
            } catch (InconsistencyException e) {
            }
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int value) {

    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, XVarInteger value) {

    }

    @Override
    public void buildCtrElement(String id, int[] list, int startIndex, XVarInteger index, Types.TypeRank rank, XVarInteger value) {
        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        IntVar z =  mapVar.get(value.id());
        try {
            minicp.post(new Element1D(list, idx, z));
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, int value) {
        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        IntVar z =  makeIntVar(minicp,value,value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(i -> mapVar.get(i)).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, XVarInteger value) {
        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        IntVar z =  mapVar.get(value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(i -> mapVar.get(i)).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeConditionOperatorRel op, int k) {
        System.out.println("here1");
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeConditionOperatorSet op, int[] t) {
        System.out.println("here2");
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeConditionOperatorSet op, int min, int max) {
        System.out.println("here3");
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeUnaryArithmeticOperator aop, XVarInteger y) {
        System.out.println("here3");
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, int k) {
        System.out.println("here5");
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, XVarInteger y) {
        System.out.println("here6");
    }

    public IntVar arithmeticOperatorConstraint(IntVar x,Types.TypeArithmeticOperator aop,IntVar y) {
        IntVar r = null;
        try {
            switch (aop) {
                case ADD:
                    r = sum(x, y);
                    break;
                case DIST:
                    IntVar x_y = sum(x, minus(y));
                    r = makeIntVar(minicp, 0, x_y.getMax());
                    minicp.post(new Absolute(x_y, r));
                    break;
                case DIV:
                    System.out.println("division not implemented");
                    throw new IllegalArgumentException("Division between vars is not implemented");
                case MUL:
                    break;
                case SUB:
                    r = sum(x, minus(y));
                    break;
                case MOD:
                    System.out.println("modulo not implemented");
                    throw new IllegalArgumentException("Modulo between vars is not implemented");
                case POW:
                    throw new IllegalArgumentException("Pow between vars is not implemented");
                default:
                    break;
            }
            return r;
        } catch (InconsistencyException e) {
            System.out.println("inconsistent model");
        }
        return r;
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x_, Types.TypeArithmeticOperator aop, XVarInteger y_, Types.TypeConditionOperatorRel op, int k) {

        IntVar x = mapVar.get(x_);
        IntVar y = mapVar.get(y_);

        IntVar r = arithmeticOperatorConstraint(x, aop, y);
        relConstraintVal(r, op, k);

    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, XVarInteger y, Types.TypeConditionOperatorRel op, XVarInteger z) {
        IntVar r = arithmeticOperatorConstraint(mapVar.get(x),aop,mapVar.get(y));
        relConstraintVar(r,op,mapVar.get(z));
    }


    @Override
    public void buildCtrSum(String id, XVarInteger[] list, Condition condition) {
        try {
            IntVar s = sum(Arrays.stream(list).map(i -> mapVar.get(i)).toArray(IntVar[]::new));
            _buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            System.out.println("model inconsistent");
        }
    }

    @Override
    public void buildCtrAllDifferent(String id, XVarInteger[] list) {
        // Constraints
        try {
            minicp.post(allDifferent(Arrays.stream(list).map(x -> mapVar.get(x)).toArray(IntVar[]::new)));
        } catch (InconsistencyException e) {
            System.out.println("model inconsistent");
        }
    }

    @Override
    public void buildObjToMinimize(String id, XVarInteger x) {
        objectiveMinimize = Optional.of(mapVar.get(x));
        //minicp.post(new Minimize(x,dfs));
    }

    @Override
    public void buildObjToMinimize(String id, XNodeParent<XVarInteger> tree) {
        System.out.println("not implemented1");
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list) {
        if (type != Types.TypeObjective.SUM) {
            System.out.println("not implemented2");

        }
        try {
            IntVar s = sum(Arrays.stream(list).map(i -> mapVar.get(i)).toArray(IntVar[]::new));
            objectiveMinimize = Optional.of(s);
        } catch (InconsistencyException e) {
            System.out.println("model inconsistent");
        }
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list, int[] coeffs) {
        System.out.println("not implemented3");
    }

    @Override
    public void buildCtrIntension(String id, XVarInteger[] scope, XNodeParent<XVarInteger> tree) {

    }

    @Override
    public void buildCtrIntension(String id, XVarSymbolic[] scope, XNodeParent<XVarSymbolic> syntaxTreeRoot) {

    }

    public String solve() {
        DFSearch search = makeDfs(minicp, firstFail(mapVar.values().toArray(new IntVar[0])));

        if (objectiveMinimize.isPresent()) {
            try {
                minicp.post(new Minimize(objectiveMinimize.get(), search));
            } catch (InconsistencyException e) {
                e.printStackTrace();
            }
        }

        //final ArrayList<String> solutions = new ArrayList<>();

        Box<String> lastSolution = new Box<String>("");
        search.onSolution(() -> {

            int i = 0;
            String sol = "<instantiation>\n\t<list>\n\t\t";
            //xcsp3.mapVar.entrySet().stream()
            for (XVarInteger x : xVars) {
                sol += x.id() + " ";
            }
            sol += "\n\t</list>\n\t<values>\n\t\t";
            for (IntVar x : minicpVars) {
                sol += x.getMin() + " ";
            }
            sol += "\n\t</values>\n</instantiation>";
            lastSolution.set(sol);
            //solutions.add(sol);

        });
        SearchStatistics stats = objectiveMinimize.isPresent() ? search.start() : search.start(limit -> limit.nSolutions >= 1);
        System.out.println(stats);
        return lastSolution.get();
    }


    public static void main(String[] args) {
        try {
            //XCSP3 xcsp3 = new XCSP3("data/xcsp3/TravellingSalesman-15-30-13.xml");
            XCSP3 xcsp3 = new XCSP3("data/xcsp3/easy/Queens-0008-m1.xml");
            String solution = xcsp3.solve();
            List<String> violatedCtrs = xcsp3.getViolatedCtrs(solution);
            System.out.println(violatedCtrs);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
