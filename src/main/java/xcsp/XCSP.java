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

import minicp.cp.Factory;
import minicp.engine.constraints.*;
import minicp.engine.core.BoolVar;
import minicp.engine.core.IntVar;
import minicp.engine.core.Solver;
import minicp.search.DFSearch;
import minicp.search.SearchStatistics;
import minicp.util.exception.InconsistencyException;
import minicp.util.exception.NotImplementedException;
import org.xcsp.checker.SolutionChecker;
import org.xcsp.common.Condition;
import org.xcsp.common.Constants;
import org.xcsp.common.Types;
import org.xcsp.common.predicates.XNodeParent;
import org.xcsp.parser.XCallbacks2;
import org.xcsp.parser.entries.XVariables.XVarInteger;
import org.xcsp.parser.entries.XVariables.XVarSymbolic;

import java.io.ByteArrayInputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static minicp.cp.BranchingScheme.and;
import static minicp.cp.BranchingScheme.firstFail;
import static minicp.cp.Factory.*;

public class XCSP implements XCallbacks2 {

    private Implem implem = new Implem(this);

    private String fileName;
    private final Map<XVarInteger, IntVar> mapVar = new HashMap<>();
    private final List<XVarInteger> xVars = new LinkedList<>();
    private final List<IntVar> minicpVars = new LinkedList<>();


    private final Set<IntVar> decisionVars = new LinkedHashSet<>();

    private final Solver minicp = makeSolver();

    private Optional<IntVar> objectiveMinimize = Optional.empty();
    private Optional<IntVar> realObjective = Optional.empty();

    private boolean hasFailed;


    @Override
    public Implem implem() {
        return implem;
    }

    public XCSP(String fileName) throws Exception {
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

    public boolean isCOP() {
        return objectiveMinimize.isPresent();
    }

    public List<String> getViolatedCtrs(String solution) throws Exception {
        return new SolutionChecker(false, fileName, new ByteArrayInputStream(solution.getBytes())).violatedCtrs;
    }

    @Override
    public void buildVarInteger(XVarInteger x, int minValue, int maxValue) {
        IntVar minicpVar = makeIntVar(minicp, minValue, maxValue);
        mapVar.put(x, minicpVar);
        minicpVars.add(minicpVar);
        xVars.add(x);
    }

    @Override
    public void buildVarInteger(XVarInteger x, int[] values) {
        Set<Integer> vals = new LinkedHashSet<>();
        for (int v : values) vals.add(v);
        IntVar minicpVar = makeIntVar(minicp, vals);
        mapVar.put(x, minicpVar);
        minicpVars.add(minicpVar);
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
        if (hasFailed)
            return;
        int[][] table = new int[values.length][1];
        for (int i = 0; i < values.length; i++)
            table[i][0] = values[i];
        buildCtrExtension(id, new XVarInteger[]{x}, table, positive, flags);
    }

    @Override
    public void buildCtrExtension(String id, XVarInteger[] list, int[][] tuples, boolean positive, Set<Types.TypeFlag> flags) {


        if (hasFailed)
            return;



        /*if (flags.contains(Types.TypeFlag.UNCLEAN_TUPLES)) {
            // You have possibly to clean tuples here, in order to remove invalid tuples.
            // A tuple is invalid if it contains a setValue $a$ for a variable $x$, not present in $dom(x)$
            // Note that most of the time, tuples are already cleaned by the parser
        }*/

        try {

            if (!positive) {
                minicp.post(new NegTableCT(trVars(list), tuples));
            } else {
                if (flags.contains(Types.TypeFlag.STARRED_TUPLES)) {
                    minicp.fixPoint();
                    minicp.post(new ShortTableCT(trVars(list), tuples, Constants.STAR_INT));
                } else {
                    minicp.fixPoint();
                    minicp.post(new TableDecomp(trVars(list), tuples));
                }
            }

        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    private void relConstraintVal(IntVar x, Types.TypeConditionOperatorRel operator, int k) {
        if (hasFailed)
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
        if (hasFailed)
            return;

        try {
            switch (operator) {
                case EQ:
                    // TODO: implement equal
                    minicp.post(new LessOrEqual(x, y));
                    minicp.post(new LessOrEqual(y, x));
                    break;
                case GE:
                    minicp.post(new LessOrEqual(y, x));
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
                    minicp.post(notEqual(x, y));
                    break;
                default:
                    throw new InvalidParameterException("unknown condition");
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    private void buildCrtWithCondition(String id, IntVar expr, Condition operator) {
        if (hasFailed)
            return;

        if (operator instanceof Condition.ConditionVal) {
            Condition.ConditionVal op = (Condition.ConditionVal) operator;
            relConstraintVal(expr, op.operator, (int) op.k);
        } else if (operator instanceof Condition.ConditionVar) {
            Condition.ConditionVar op = (Condition.ConditionVar) operator;
            relConstraintVar(expr, op.operator, mapVar.get(op.x));
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
        if (hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index), startIndex);
        IntVar z = mapVar.get(value);
        try {
            minicp.post(new Element1D(list, idx, z));
            decisionVars.add(idx);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, int value) {
        if (hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index), startIndex);
        decisionVars.add(idx);
        IntVar z = makeIntVar(minicp, value, value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrElement(String id, XVarInteger[] list, int startIndex, XVarInteger index, Types.TypeRank rank, XVarInteger value) {
        if (hasFailed)
            return;

        if (rank != Types.TypeRank.ANY)
            throw new IllegalArgumentException("Element constraint only supports ANY as position for the index");
        IntVar idx = minus(mapVar.get(index), startIndex);
        decisionVars.add(idx);
        IntVar z = mapVar.get(value);
        try {
            minicp.post(new Element1DVar(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new), idx, z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeConditionOperatorRel op, int k) {
        if (hasFailed)
            return;
        relConstraintVal(mapVar.get(x), op, k);
    }


    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeUnaryArithmeticOperator aop, XVarInteger y) {
        if (hasFailed)
            return;
        try {
            IntVar r = unaryArithmeticOperatorConstraint(mapVar.get(y), aop);
            relConstraintVar(mapVar.get(x), Types.TypeConditionOperatorRel.EQ, r);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, int k) {
        if (hasFailed)
            return;

        try {
            IntVar r = arithmeticOperatorConstraintVal(mapVar.get(x), aop, p);
            relConstraintVal(r, op, k);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, int p, Types.TypeConditionOperatorRel op, XVarInteger y) {
        if (hasFailed)
            return;
        try {
            IntVar r = arithmeticOperatorConstraintVal(mapVar.get(x), aop, p);
            relConstraintVar(r, op, mapVar.get(y));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, XVarInteger y, Types.TypeConditionOperatorRel op, int k) {
        if (hasFailed)
            return;

        IntVar minicpX = mapVar.get(x);
        IntVar minicpY = mapVar.get(y);

        try {
            IntVar r = arithmeticOperatorConstraintVar(minicpX, aop, minicpY);
            relConstraintVal(r, op, k);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrPrimitive(String id, XVarInteger x, Types.TypeArithmeticOperator aop, XVarInteger y, Types.TypeConditionOperatorRel op, XVarInteger z) {
        if (hasFailed)
            return;

        try {
            IntVar r = arithmeticOperatorConstraintVar(mapVar.get(x), aop, mapVar.get(y));
            relConstraintVar(r, op, mapVar.get(z));
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    private IntVar unaryArithmeticOperatorConstraint(IntVar x, Types.TypeUnaryArithmeticOperator aop) {
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

    private IntVar arithmeticOperatorConstraintVal(IntVar x, Types.TypeArithmeticOperator aop, int p) {
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

    private IntVar arithmeticOperatorConstraintVar(IntVar x, Types.TypeArithmeticOperator aop, IntVar y) {
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
        if (hasFailed)
            return;

        try {
            IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
            buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrSum(String id, XVarInteger[] list, int[] coeffs, Condition condition) {
        if (hasFailed)
            return;

        try {
            IntVar[] wx = new IntVar[list.length];
            for (int i = 0; i < list.length; i++) {
                wx[i] = mul(mapVar.get(list[i]), coeffs[i]);
            }
            IntVar s = sum(wx);
            buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    @Override
    public void buildCtrAllDifferent(String id, XVarInteger[] list) {

        if (hasFailed)
            return;
        // Constraints
        try {
            IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
            minicp.post(allDifferent(xs));
            for (IntVar x : xs) {
                decisionVars.add(x);
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    private void setObj(IntVar obj, boolean minimization) {
        IntVar minobj = minimization ? obj : minus(obj);


        objectiveMinimize = Optional.of(minobj);
        realObjective = Optional.of(obj);
    }

    @Override
    public void buildObjToMinimize(String id, XVarInteger x) {
        if (hasFailed)
            return;

        setObj(mapVar.get(x), true);
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list) {
        if (hasFailed)
            return;
        try {
            if (type == Types.TypeObjective.MAXIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                setObj(maximum(xs), true);
            } else if (type == Types.TypeObjective.MINIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                setObj(minimum(xs), true);
            } else if (type == Types.TypeObjective.SUM) {
                IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
                setObj(s, true);
            } else {
                throw new NotImplementedException();
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMinimize(String id, Types.TypeObjective type, XVarInteger[] list, int[] coeffs) {
        if (hasFailed)
            return;

        IntVar[] wx = new IntVar[list.length];
        for (int i = 0; i < list.length; i++) {
            wx[i] = mul(mapVar.get(list[i]), coeffs[i]);
        }
        try {
            IntVar s = sum(wx);
            setObj(s, true);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMaximize(String id, XVarInteger x) {
        if (hasFailed)
            return;

        setObj(mapVar.get(x), false);
    }

    @Override
    public void buildObjToMaximize(String id, Types.TypeObjective type, XVarInteger[] list) {
        if (hasFailed)
            return;

        try {
            if (type == Types.TypeObjective.MAXIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                setObj(maximum(xs), false);
            } else if (type == Types.TypeObjective.MINIMUM) {
                IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
                setObj(minimum(xs), false);
            } else if (type == Types.TypeObjective.SUM) {
                IntVar s = sum(Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new));
                setObj(s, false);
            } else {
                throw new NotImplementedException();
            }
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildObjToMaximize(String id, Types.TypeObjective type, XVarInteger[] list, int[] coeffs) {
        if (hasFailed)
            return;

        IntVar[] wx = new IntVar[list.length];
        for (int i = 0; i < list.length; i++) {
            wx[i] = mul(mapVar.get(list[i]), coeffs[i]);
        }
        try {
            IntVar s = sum(wx);
            setObj(s, false);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    @Override
    public void buildCtrIntension(String id, XVarInteger[] scope, XNodeParent<XVarInteger> tree) {
        if (hasFailed)
            return;
        throw new NotImplementedException();
    }

    @Override
    public void buildCtrIntension(String id, XVarSymbolic[] scope, XNodeParent<XVarSymbolic> syntaxTreeRoot) {
        if (hasFailed)
            return;
        //Not needed
        throw new NotImplementedException();
    }

    @Override
    public void buildCtrMaximum(String id, XVarInteger[] list, Condition condition) {
        if (hasFailed)
            return;
        // Constraints
        try {
            IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
            IntVar s = Factory.maximum(xs);
            buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }


    @Override
    public void buildCtrMinimum(String id, XVarInteger[] list, Condition condition) {
        if (hasFailed)
            return;
        // Constraints
        try {
            IntVar[] xs = Arrays.stream(list).map(mapVar::get).toArray(IntVar[]::new);
            IntVar s = Factory.minimum(xs);
            buildCrtWithCondition(id, s, condition);
        } catch (InconsistencyException e) {
            hasFailed = true;
        }
    }

    static class EntryComparator implements Comparator<Map.Entry<XVarInteger, IntVar>> {
        @Override
        public int compare(Map.Entry<XVarInteger, IntVar> o1, Map.Entry<XVarInteger, IntVar> o2) {
            return o1.getKey().id.compareTo(o2.getKey().id);
        }
    }

    public String solve(int nSolution, int timeOut) {
        AtomicReference<String> lastSolution = new AtomicReference<>("");
        Long t0 = System.currentTimeMillis();

        solve((solution, value) -> {
            System.out.println("solfound " + (value == Integer.MAX_VALUE ? value : "solution"));
            lastSolution.set(solution);
        }, ss -> {
            int nSols = isCOP() ? nSolution : 1;
            return (System.currentTimeMillis() - t0 >= timeOut * 1000 || ss.numberOfSolutions() >= nSols);
        });

        return lastSolution.get();
    }

    public void buildAnnotationDecision(XVarInteger[] list) {
        decisionVars.clear();
        Arrays.stream(list).map(mapVar::get).forEach(decisionVars::add);
    }

    /**
     * @param onSolution: void onSolution(solution, obj). If not a COP, obj = Integer.MAXVALUE
     * @param shouldStop: boolean shouldStop(stats, isCOP).
     * @return Stats
     */
    public SearchStatistics solve(BiConsumer<String, Integer> onSolution, Function<SearchStatistics, Boolean> shouldStop) {

        IntVar[] vars = mapVar.entrySet().stream().sorted(new EntryComparator()).map(Map.Entry::getValue).toArray(IntVar[]::new);
        DFSearch search;
        if (decisionVars.isEmpty()) {
            search = makeDfs(minicp, firstFail(vars));
        } else {
            search = makeDfs(minicp, and(firstFail(decisionVars.toArray(new IntVar[0])), firstFail(vars)));
        }

        if (objectiveMinimize.isPresent()) {
            try {
                minicp.minimize(objectiveMinimize.get());
            } catch (InconsistencyException e) {
                hasFailed = true;
            }
        }

        if (hasFailed) {
            throw InconsistencyException.INCONSISTENCY;
        }


        search.onSolution(() -> {
            StringBuilder sol = new StringBuilder("<instantiation>\n\t<list>\n\t\t");
            for (XVarInteger x : xVars)
                sol.append(x.id()).append(" ");
            sol.append("\n\t</list>\n\t<values>\n\t\t");
            for (IntVar x : minicpVars)
                sol.append(x.min()).append(" ");
            sol.append("\n\t</values>\n</instantiation>");
            onSolution.accept(sol.toString(), realObjective.map(IntVar::min).orElse(Integer.MAX_VALUE));
        });

        return search.solve(shouldStop::apply);
    }


    public static void main(String[] args) {
        try {
            XCSP xcsp = new XCSP(args[0]);
            String solution = xcsp.solve(Integer.MAX_VALUE, 100);
            List<String> violatedCtrs = xcsp.getViolatedCtrs(solution);
            System.out.println(violatedCtrs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
