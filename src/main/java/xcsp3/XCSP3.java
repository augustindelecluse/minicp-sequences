package xcsp3;

import minicp.cp.Factory;
import minicp.engine.constraints.*;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.IntVarViewOpposite;
import minicp.engine.core.Solver;

import static minicp.cp.Heuristics.*;
import static minicp.cp.Factory.*;
import static minicp.search.Selector.branch;
import static minicp.search.Selector.selectMin;
import static org.xcsp.parser.XCallbacks.XCallbacksParameters.RECOGNIZING_BEFORE_CONVERTING;

import minicp.search.Choice;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.Box;
import minicp.util.InconsistencyException;
import minicp.util.NotImplementedException;
import org.w3c.dom.Document;
import org.xcsp.checker.SolutionChecker;
import org.xcsp.common.Condition;
import org.xcsp.common.Constants;
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
    private final Map<XVarInteger, IntVar> mapVar = new HashMap<>();
    private final List<XVarInteger> xVars = new LinkedList<>();
    private final List<IntVar> minicpVars = new LinkedList<>();


    private final Set<IntVar> decisionVars = new HashSet<>();

    public final Solver minicp = new Solver();

    private Optional<IntVar> objectiveMinimize = Optional.empty();
    private boolean hasFailed;


    @Override
    public Implem implem() {
        return implem;
    }

    public XCSP3(String fileName) throws Exception {
        this.fileName = fileName;
        hasFailed = false;

        implem.currParameters.clear();

        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_UNARY_PRIMITIVES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_BINARY_PRIMITIVES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZE_TERNARY_PRIMITIVES, new Object());
        //implem.currParameters.put(XCallbacksParameters.RECOGNIZE_NVALUES_CASES, new Object());
        implem.currParameters.put(XCallbacksParameters.RECOGNIZING_BEFORE_CONVERTING, Boolean.TRUE);
        implem.currParameters.put(XCallbacksParameters.CONVERT_INTENSION_TO_EXTENSION_ARITY_LIMIT, Integer.MAX_VALUE); // included
        implem.currParameters.put(XCallbacksParameters.CONVERT_INTENSION_TO_EXTENSION_SPACE_LIMIT, Long.MAX_VALUE); // included

        loadInstance(fileName);
    }

    public List<String> getViolatedCtrs(String solution) throws Exception {
        return new SolutionChecker(false, fileName, new ByteArrayInputStream(solution.getBytes())).violatedCtrs;
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
        Set<Integer> vals = new LinkedHashSet<>();
        for (int v : values) vals.add(v);
        IntVar x_ = makeIntVar(minicp, vals);
        mapVar.put(x, x_);
        minicpVars.add(x_);
        xVars.add(x);
    }

    private IntVar[] trVars(Object vars) {
        return Arrays.stream((XVarInteger[]) vars).map(mapVar::get).toArray(IntVar[]::new);
    }

    private IntVar[][] trVars2D(Object vars) {
        return Arrays.stream((XVarInteger[][]) vars).map(this::trVars).toArray(IntVar[][]::new);
    }

    @Override
    public void buildCtrExtension(String id, XVarInteger x, int[] values, boolean positive, Set<Types.TypeFlag> flags) {
        if(hasFailed)
            return;
        int[][] table = new int[values.length][1];
        for(int i = 0; i < values.length; i++)
            table[i][0] = values[i];
        buildCtrExtension(id, new XVarInteger[]{x}, table, positive, flags);
    }

    @Override
    public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive, Set<Types.TypeFlag> flags) {


        if(hasFailed)
            return;



        /*if (flags.contains(Types.TypeFlag.UNCLEAN_TUPLES)) {
            // You have possibly to clean tuples here, in order to remove invalid tuples.
            // A tuple is invalid if it contains a value $a$ for a variable $x$, not present in $dom(x)$
            // Note that most of the time, tuples are already cleaned by the parser
        }*/

        try {

            if (!positive) {
                minicp.post(new NegTableCT(trVars(list), tuples));
            }
            else {
                if (flags.contains(Types.TypeFlag.STARRED_TUPLES)) {
                    minicp.fixPoint();
                    minicp.post(new ShortTableCT(trVars(list), tuples, Constants.STAR_INT));
                } else {
                    minicp.fixPoint();
                    minicp.post(new TableCT(trVars(list), tuples));
                }
            }

        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    private void relConstraintVal(IntVar x, Types.TypeConditionOperatorRel operator, int k) {
        if(hasFailed)
            return;

        try {
            switch (operator) {
                case EQ:
                    x.assign(k);
                    break;
                case GE:
                    x.removeBelow(k);
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
            System.out.println("failed rel");
            hasFailed = true;
        }
    }

    private void relConstraintVar(IntVar x, Types.TypeConditionOperatorRel operator, IntVar y) {
        if(hasFailed)
            return;

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
            hasFailed = true;
        }
    }


    private void _buildCrtWithCondition(String id, IntVar expr, Condition operator) {
        if(hasFailed)
            return;

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

                        break;
                    default:
                        throw new InvalidParameterException("unknown condition");
                }
                expr.getSolver().fixPoint();
            } catch (InconsistencyException e) {
                hasFailed = true;
            }
        }
    }


    @Override
    public void buildCtrElement(String id, int[] list, int startIndex, XVarInteger index, Types.TypeRank rank, XVarInteger value) {
        if(hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        IntVar z =  mapVar.get(value);
        try {
            minicp.post(new Element1D(list, idx, z));
            decisionVars.add(idx);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, int value) {
        if(hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        decisionVars.add(idx);
        IntVar z =  makeIntVar(minicp,value,value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, XVarInteger value) {
        if(hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index),startIndex);
        decisionVars.add(idx);
        IntVar z =  mapVar.get(value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeConditionOperatorRel op, int k) {
        if(hasFailed)
            return;
        relConstraintVal(mapVar.get(x), op,k);
    }


    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeUnaryArithmeticOperator aop, XVarInteger y) {
        if(hasFailed)
            return;
        try {
            IntVar r = unaryArithmeticOperatorConstraint(mapVar.get(y),aop);
            relConstraintVar(mapVar.get(x),Types.TypeConditionOperatorRel.EQ,r);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, int k) {
        if(hasFailed)
            return;

        try {
            IntVar r = arithmeticOperatorConstraintVal(mapVar.get(x), aop, p);
            relConstraintVal(r,op,k);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, XVarInteger y) {
        if(hasFailed)
            return;
        try {
            IntVar r = arithmeticOperatorConstraintVal(mapVar.get(x), aop, p);
            relConstraintVar(r,op,mapVar.get(y));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x_, Types.TypeArithmeticOperator aop, XVarInteger y_, Types.TypeConditionOperatorRel op, int k) {
        if(hasFailed)
            return;

        IntVar x = mapVar.get(x_);
        IntVar y = mapVar.get(y_);

        try {
            IntVar r = arithmeticOperatorConstraintVar(x, aop, y);
            relConstraintVal(r, op, k);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, XVarInteger y, Types.TypeConditionOperatorRel op, XVarInteger z) {
        if(hasFailed)
            return;

        try {
            IntVar r = arithmeticOperatorConstraintVar(mapVar.get(x),aop,mapVar.get(y));
            relConstraintVar(r,op,mapVar.get(z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    private IntVar unaryArithmeticOperatorConstraint(IntVar x,Types.TypeUnaryArithmeticOperator aop) throws InconsistencyException {
        switch (aop) {
            case NEG:
                return Factory.minus(x);
            case ABS:
                return Factory.abs(x);
            case NOT:
                //TODO student: you may want to implement it with a new type of view.
                throw new IllegalArgumentException("not implemented");
            case SQR:
            default:
                // Not needed
                throw new IllegalArgumentException("not implemented");
        }
    }

    private IntVar arithmeticOperatorConstraintVal(IntVar x,Types.TypeArithmeticOperator aop, int p) throws InconsistencyException {
        switch (aop) {
            case ADD:
                return Factory.plus(x, p);
            case DIST:
                return Factory.abs(Factory.minus(x, p));
            case SUB:
                return Factory.minus(x, p);
            case MUL:
                return Factory.mul(x, p);
            case DIV:
                // Not needed
                throw new IllegalArgumentException("Division between vars is not implemented");
            case MOD:
                // Not needed
                throw new IllegalArgumentException("Modulo between vars is not implemented");
            case POW:
                // Not needed
                throw new IllegalArgumentException("Pow between vars is not implemented");
            default:
                throw new IllegalArgumentException("Unknown TypeArithmeticOperator");
        }
    }

    private IntVar arithmeticOperatorConstraintVar(IntVar x,Types.TypeArithmeticOperator aop,IntVar y) throws InconsistencyException {
        switch (aop) {
            case ADD:
                return sum(x, y);
            case DIST:
                return Factory.abs(sum(x, minus(y)));
            case SUB:
                return sum(x, minus(y));
            case DIV:
                // Not needed
                throw new IllegalArgumentException("Division between vars is not implemented");
            case MUL:
                // Not needed
                throw new IllegalArgumentException("Multiplication between vars is not implemented");
            case MOD:
                // Not needed
                throw new IllegalArgumentException("Modulo between vars is not implemented");
            case POW:
                // Not needed
                throw new IllegalArgumentException("Pow between vars is not implemented");
            default:
                throw new IllegalArgumentException("Unknown TypeArithmeticOperator");
        }
    }

    @Override
    public void buildCtrSum(String id, XVarInteger[] list, Condition condition) {
        if(hasFailed)
            return;

        try {
            IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
            _buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrSum(String id, XVarInteger[] list, int[] coeffs, Condition condition) {
        if(hasFailed)
            return;

        try {
            IntVar [] wx = new IntVar[list.length];
            for (int i = 0; i < list.length; i++) {
                wx[i] = mul(mapVar.get(list[i]),coeffs[i]);
            }
            IntVar s = sum(wx);
            _buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    @Override
    public void buildCtrAllDifferent(String id, XVarInteger[] list) {

        if(hasFailed)
            return;
        // Constraints
        try {
            IntVar [] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
            minicp.post(allDifferent(xs));
            for (IntVar x: xs) {
                decisionVars.add(x);
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMinimize(String id, XVarInteger x) {
        if(hasFailed)
            return;

        objectiveMinimize = Optional.of(mapVar.get(x));
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list) {
        if (hasFailed)
            return;
        try {
            if (type == Types.TypeObjective.MAXIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                objectiveMinimize = Optional.of(maximum(xs));
            } else if (type == Types.TypeObjective.MINIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                objectiveMinimize = Optional.of(minimum(xs));
            } else if (type == Types.TypeObjective.SUM) {
                IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
                objectiveMinimize = Optional.of(s);
            } else {
                throw new NotImplementedException();
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list, int[] coeffs) {
        if(hasFailed)
            return;

        IntVar [] wx = new IntVar[list.length];
        for (int i = 0; i < list.length; i++) {
            wx[i] = mul(mapVar.get(list[i]),coeffs[i]);
        }
        try {
            IntVar s = sum(wx);
            objectiveMinimize = Optional.of(s);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMaximize(String id, XVarInteger x) {
        if(hasFailed)
            return;

        objectiveMinimize = Optional.of(minus(mapVar.get(x)));
    }

    @Override
    public void buildObjToMaximize(String id, Types.TypeObjective type, XVarInteger[] list) {
        if(hasFailed)
            return;

        try {
            if (type == Types.TypeObjective.MAXIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                objectiveMinimize = Optional.of(minus(maximum(xs)));
            } else if (type == Types.TypeObjective.MINIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                objectiveMinimize = Optional.of(minus(minimum(xs)));
            } else if (type == Types.TypeObjective.SUM) {
                IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
                objectiveMinimize = Optional.of(minus(s));
            } else {
                throw new NotImplementedException();
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMaximize(String id, Types.TypeObjective type, XVarInteger[] list, int[] coeffs) {
        if(hasFailed)
            return;

        IntVar [] wx = new IntVar[list.length];
        for (int i = 0; i < list.length; i++) {
            wx[i] = mul(mapVar.get(list[i]),coeffs[i]);
        }
        try {
            IntVar s = sum(wx);
            objectiveMinimize = Optional.of(minus(s));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrIntension(String id, XVarInteger[] scope, XNodeParent<XVarInteger> tree) {
        if(hasFailed)
            return;
        throw new NotImplementedException();
    }

    @Override
    public void buildCtrIntension(String id, XVarSymbolic[] scope, XNodeParent<XVarSymbolic> syntaxTreeRoot) {
        if(hasFailed)
            return;
        //Not needed
        throw new NotImplementedException();
    }

    class EntryComparator implements Comparator<Map.Entry<XVarInteger, IntVar>> {
        @Override
        public int compare(Map.Entry<XVarInteger, IntVar> o1, Map.Entry<XVarInteger, IntVar> o2) {
            return o1.getKey().id.compareTo(o2.getKey().id);
        }
    }

    public Choice ff(IntVar ... x) {
        Random r = new java.util.Random(0);

        return selectMin(x,
                xi -> xi.getSize() > 1,
                xi -> xi.getSize(),
                xi -> {
                    int v = xi.getMin(); //r.nextBoolean() ? xi.getMax(): xi.getMin();
                    return branch(
                            () -> {
                                equal(xi,v);
                            },
                            () -> {
                                notEqual(xi,v);
                            }
                    );
                }
        );
    }


    /**
     *
     * @param nSolution maximum number of solution (not taken into account if satisfiability problem, only first feasible one)
     * @param timeOut
     * @return
     */
    public String solve(int nSolution, int timeOut) {

        IntVar[] vars = mapVar.entrySet().stream().sorted(new EntryComparator()).map(i -> i.getValue()).toArray(size -> new IntVar[size]);
        DFSearch search;
        if (decisionVars.isEmpty()) {
            System.out.println("here");
            search = makeDfs(minicp, firstFail(vars));

        } else {
            search = makeDfs(minicp, ff(vars));
            //search = makeDfs(minicp, and(ff(decisionVars.toArray(new IntVar[0])), ff(vars)));
        }

        if (objectiveMinimize.isPresent()) {
            try {
                minicp.post(new Minimize(objectiveMinimize.get(), search));
            } catch (InconsistencyException e) {
                hasFailed = true;
            }
        } else {
            nSolution = 1;
        }

        if (hasFailed) {
            System.out.println("Model is inconsistent at first fixpoint");
            return "";
        }



        Box<String> lastSolution = new Box<String>("");
        search.onSolution(() -> {
            System.out.println("solfound"+(objectiveMinimize.isPresent() ? objectiveMinimize.get(): "-"));
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
        Long t0 = System.currentTimeMillis();

        final int nSols = nSolution;

        SearchStatistics stats = search.start(limit -> (System.currentTimeMillis()-t0 >= timeOut*1000 || limit.nSolutions >= nSols));

        System.out.println(stats);
        //System.out.println(stats);
        return lastSolution.get();
    }


    public static void main(String[] args) {
        try {
            XCSP3 xcsp3 = new XCSP3("/Users/pschaus/Documents/IdeaProjects/minicp-solution/data/xcsp3/hard/GraphColoring-myciel7.xml");
            String solution = xcsp3.solve(100000,1000);
            List<String> violatedCtrs = xcsp3.getViolatedCtrs(solution);
            System.out.println(violatedCtrs);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
