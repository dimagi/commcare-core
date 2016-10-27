package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.pivot.CmpPivot;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.data.DecimalData;
import org.javarosa.core.model.data.UncastData;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;

import java.util.Vector;

public class XPathCmpExpr extends XPathBinaryOpExpr {
    public static final int LT = 0;
    public static final int GT = 1;
    public static final int LTE = 2;
    public static final int GTE = 3;

    @SuppressWarnings("unused")
    public XPathCmpExpr() {
    } //for deserialization

    public XPathCmpExpr(int op, XPathExpression a, XPathExpression b) {
        super(op, a, b);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        Object aval = a.eval(model, evalContext);
        Object bval = b.eval(model, evalContext);
        boolean result = false;

        //xpath spec says comparisons only defined for numbers (not defined for strings)
        aval = XPathFuncExpr.toNumeric(aval);
        bval = XPathFuncExpr.toNumeric(bval);

        double fa = (Double)aval;
        double fb = (Double)bval;

        switch (op) {
            case LT:
                result = fa < fb;
                break;
            case GT:
                result = fa > fb;
                break;
            case LTE:
                result = fa <= fb;
                break;
            case GTE:
                result = fa >= fb;
                break;
        }

        return result;
    }

    @Override
    public String toString() {
        String sOp = null;

        switch (op) {
            case LT:
                sOp = "<";
                break;
            case GT:
                sOp = ">";
                break;
            case LTE:
                sOp = "<=";
                break;
            case GTE:
                sOp = ">=";
                break;
        }

        return super.toString(sOp);
    }

    @Override
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        Object aval = a.pivot(model, evalContext, pivots, sentinal);
        Object bval = b.pivot(model, evalContext, pivots, sentinal);
        if (bval instanceof XPathNodeset) {
            bval = ((XPathNodeset)bval).unpack();
        }

        if (handled(aval, bval, sentinal, pivots) || handled(bval, aval, sentinal, pivots)) {
            return null;
        }

        return this.eval(model, evalContext);
    }

    private boolean handled(Object a, Object b, Object sentinal, Vector<Object> pivots) throws UnpivotableExpressionException {
        if (sentinal == a) {
            if (b == null) {
                //Can't pivot on an expression which is derived from pivoted expressions
                throw new UnpivotableExpressionException();
            } else if (sentinal == b) {
                //WTF?
                throw new UnpivotableExpressionException();
            } else {
                Double val = null;
                //either of
                if (b instanceof Double) {
                    val = (Double)b;
                } else {
                    //These are probably the
                    if (b instanceof Integer) {
                        val = ((Integer)b).doubleValue();
                    } else if (b instanceof Long) {
                        val = ((Long)b).doubleValue();
                    } else if (b instanceof Float) {
                        val = ((Float)b).doubleValue();
                    } else if (b instanceof Short) {
                        val = new Double((Short)b);
                    } else if (b instanceof Byte) {
                        val = new Double((Byte)b);
                    } else {
                        if (b instanceof String) {
                            try {
                                //TODO: Too expensive?
                                val = (Double)new DecimalData().cast(new UncastData((String)b)).getValue();
                            } catch (Exception e) {
                                throw new UnpivotableExpressionException("Unrecognized numeric data in cmp expression: " + b);
                            }
                        } else {
                            throw new UnpivotableExpressionException("Unrecognized numeric data in cmp expression: " + b);
                        }
                    }
                }


                pivots.addElement(new CmpPivot(val, op));
                return true;
            }
        }
        return false;
    }

    @Override
    public String toPrettyString() {
        String prettyA = a.toPrettyString();
        String prettyB = b.toPrettyString();
        String opString;
        switch (op) {
            case LT:
                opString = " < ";
                break;
            case GT:
                opString = " > ";
                break;
            case LTE:
                opString = " <= ";
                break;
            case GTE:
                opString = " >= ";
                break;
            default:
                return "unknown_operator(" + prettyA + ", " + prettyB + ")";
        }

        return prettyA + opString + prettyB;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) ||
                ((o instanceof XPathCmpExpr) && binOpEquals((XPathBinaryOpExpr)o));
    }
}
