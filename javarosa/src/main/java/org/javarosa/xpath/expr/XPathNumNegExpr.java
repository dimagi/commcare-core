package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;

public class XPathNumNegExpr extends XPathUnaryOpExpr {
    @SuppressWarnings("unused")
    public XPathNumNegExpr() {
        // for deserialization
    }

    public XPathNumNegExpr(XPathExpression a) {
        super(a);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        double aval = XPathFuncExpr.toNumeric(a.eval(model, evalContext));
        return -aval;
    }

    @Override
    public String toString() {
        return "{unop-expr:num-neg," + a.toString() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathNumNegExpr) {
            return super.equals(o);
        } else {
            return false;
        }
    }

    @Override
    public String toPrettyString() {
        return "-" + a.toPrettyString();
    }
}
