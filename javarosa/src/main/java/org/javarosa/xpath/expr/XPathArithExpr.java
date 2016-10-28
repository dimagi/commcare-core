package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;

public class XPathArithExpr extends XPathBinaryOpExpr {
    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int DIVIDE = 3;
    public static final int MODULO = 4;

    @SuppressWarnings("unused")
    public XPathArithExpr() {
    } //for deserialization

    public XPathArithExpr(int op, XPathExpression a, XPathExpression b) {
        super(op, a, b);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        double aval = XPathFuncExpr.toNumeric(a.eval(model, evalContext));
        double bval = XPathFuncExpr.toNumeric(b.eval(model, evalContext));

        double result = 0;
        switch (op) {
            case ADD:
                result = aval + bval;
                break;
            case SUBTRACT:
                result = aval - bval;
                break;
            case MULTIPLY:
                result = aval * bval;
                break;
            case DIVIDE:
                result = aval / bval;
                break;
            case MODULO:
                result = aval % bval;
                break;
        }
        return new Double(result);
    }

    @Override
    public String toString() {
        String sOp = null;

        switch (op) {
            case ADD:
                sOp = "+";
                break;
            case SUBTRACT:
                sOp = "-";
                break;
            case MULTIPLY:
                sOp = "*";
                break;
            case DIVIDE:
                sOp = "/";
                break;
            case MODULO:
                sOp = "%";
                break;
        }

        return super.toString(sOp);
    }

    @Override
    public String toPrettyString() {
        String prettyA = a.toPrettyString();
        String prettyB = b.toPrettyString();
        String opString;
        switch (op) {
            case ADD:
                opString = " + ";
                break;
            case SUBTRACT:
                opString = " - ";
                break;
            case MULTIPLY:
                opString = " * ";
                break;
            case DIVIDE:
                opString = " div ";
                break;
            case MODULO:
                opString = " mod ";
                break;
            default:
                return "unknown_operator(" + prettyA + ", " + prettyB + ")";
        }

        return prettyA + opString + prettyB;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) ||
                ((o instanceof XPathArithExpr) && binOpEquals((XPathBinaryOpExpr)o));
    }
}
