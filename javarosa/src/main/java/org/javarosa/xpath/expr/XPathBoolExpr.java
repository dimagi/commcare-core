package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;

public class XPathBoolExpr extends XPathBinaryOpExpr {
    public static final int AND = 0;
    public static final int OR = 1;

    @SuppressWarnings("unused")
    public XPathBoolExpr() {
    } //for deserialization

    public XPathBoolExpr(int op, XPathExpression a, XPathExpression b) {
        super(op, a, b);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        boolean aval = XPathFuncExpr.toBoolean(a.eval(model, evalContext));

        //short-circuiting
        if ((!aval && op == AND) || (aval && op == OR)) {
            return aval;
        }

        boolean bval = XPathFuncExpr.toBoolean(b.eval(model, evalContext));

        boolean result = false;
        switch (op) {
            case AND:
                result = aval && bval;
                break;
            case OR:
                result = aval || bval;
                break;
        }
        return result;
    }

    @Override
    public String toString() {
        String sOp = null;

        switch (op) {
            case AND:
                sOp = "and";
                break;
            case OR:
                sOp = "or";
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
            case AND:
                opString = " and ";
                break;
            case OR:
                opString = " or ";
                break;
            default:
                return "unknown_operator(" + prettyA + ", " + prettyB + ")";
        }

        return prettyA + opString + prettyB;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) ||
                ((o instanceof XPathBoolExpr) && binOpEquals((XPathBinaryOpExpr)o));
    }
}
