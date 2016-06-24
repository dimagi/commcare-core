package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathNumNegExpr extends XPathUnaryOpExpr {
    public XPathNumNegExpr() {
        // for deserialization
    }

    public XPathNumNegExpr(XPathExpression a) {
        super(a);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        double aval = XPathFuncExpr.toNumeric(a.eval(model, evalContext)).doubleValue();
        return new Double(-aval);
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
