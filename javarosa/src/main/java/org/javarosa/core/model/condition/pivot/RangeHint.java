/**
 *
 */
package org.javarosa.core.model.condition.pivot;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IConditionExpr;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.xpath.expr.XPathFuncExpr;

import java.util.Vector;

/**
 * @author ctsims
 */
public abstract class RangeHint<T extends IAnswerData> implements ConstraintHint {

    Double min;
    Double max;

    T minCast;
    T maxCast;

    boolean minInclusive;
    boolean maxInclusive;

    @Override
    public void init(EvaluationContext c, IConditionExpr conditional, FormInstance instance) throws UnpivotableExpressionException {

        Vector<Object> pivots = conditional.pivot(instance, c);

        Vector<CmpPivot> internalPivots = new Vector<>();
        for (Object p : pivots) {
            if (!(p instanceof CmpPivot)) {
                throw new UnpivotableExpressionException();
            }
            internalPivots.addElement((CmpPivot)p);
        }

        if (internalPivots.size() > 1) {
            //For now.
            throw new UnpivotableExpressionException();
        }

        for (CmpPivot pivot : internalPivots) {
            evaluatePivot(pivot, conditional, c, instance);
        }
    }

    public T getMin() {
        return min == null ? null : minCast;
    }

    public boolean isMinInclusive() {
        return minInclusive;
    }

    public T getMax() {
        return max == null ? null : maxCast;
    }

    public boolean isMaxInclusive() {
        return maxInclusive;
    }

    private void evaluatePivot(CmpPivot pivot, IConditionExpr conditional, EvaluationContext c, FormInstance instance) throws UnpivotableExpressionException {
        double unit = unit();
        double val = pivot.getVal();
        double lt = val - unit;
        double gt = val + unit;

        c.isConstraint = true;

        c.candidateValue = castToValue(val);
        boolean eq = XPathFuncExpr.toBoolean(conditional.eval(instance, c));

        c.candidateValue = castToValue(lt);
        boolean ltr = XPathFuncExpr.toBoolean(conditional.eval(instance, c));

        c.candidateValue = castToValue(gt);
        boolean gtr = XPathFuncExpr.toBoolean(conditional.eval(instance, c));
        
        if (ltr && !gtr) {
            max = val;
            maxInclusive = eq;
            maxCast = castToValue(max);
        }

        if (!ltr && gtr) {
            min = val;
            minInclusive = eq;
            minCast = castToValue(min);
        }
    }

    protected abstract T castToValue(double value) throws UnpivotableExpressionException;

    protected abstract double unit();
}
