package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapTagged;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

public abstract class XPathBinaryOpExpr extends XPathOpExpr {
    public XPathExpression a, b;
    public int op;

    public XPathBinaryOpExpr() {
    } //for deserialization of children

    public XPathBinaryOpExpr(int op, XPathExpression a, XPathExpression b) {
        this.a = a;
        this.b = b;
        this.op = op;
    }

    public String toString(String op) {
        return "{binop-expr:" + op + "," + a.toString() + "," + b.toString() + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathBinaryOpExpr) {
            XPathBinaryOpExpr x = (XPathBinaryOpExpr)o;
            return op == x.op && a.equals(x.a) && b.equals(x.b);
        } else {
            return false;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        op = ExtUtil.readInt(in);
        readExpressions(in, pf);
    }

    protected void readExpressions(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        a = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
        b = (XPathExpression)ExtUtil.read(in, new ExtWrapTagged(), pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, op);
        writeExpressions(out);
    }

    protected void writeExpressions(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapTagged(a));
        ExtUtil.write(out, new ExtWrapTagged(b));
    }

    @Override
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        //Pivot both args
        Object aval = a.pivot(model, evalContext, pivots, sentinal);
        Object bval = b.pivot(model, evalContext, pivots, sentinal);

        //If either is the sentinal, we don't have a good way to represent the resulting expression, so fail
        if (aval == sentinal || bval == sentinal) {
            throw new UnpivotableExpressionException();
        }

        //If either has added a pivot, this expression can't produce any more pivots, so signal that
        if (aval == null || bval == null) {
            return null;
        }

        //Otherwise, return the value
        return this.eval(model, evalContext);
    }
}
