package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class XPathBoolExpr extends XPathBinaryOpExpr {
    public static final int AND = 0;
    public static final int OR = 1;

    public int op;

    public XPathBoolExpr() {
    } //for deserialization

    public XPathBoolExpr(int op, XPathExpression a, XPathExpression b) {
        super(a, b);
        this.op = op;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        boolean aval = XPathFuncExpr.toBoolean(a.eval(model, evalContext)).booleanValue();

        //short-circuiting
        if ((!aval && op == AND) || (aval && op == OR)) {
            return new Boolean(aval);
        }

        boolean bval = XPathFuncExpr.toBoolean(b.eval(model, evalContext)).booleanValue();

        boolean result = false;
        switch (op) {
            case AND:
                result = aval && bval;
                break;
            case OR:
                result = aval || bval;
                break;
        }
        return new Boolean(result);
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
    public boolean equals(Object o) {
        if (o instanceof XPathBoolExpr) {
            XPathBoolExpr x = (XPathBoolExpr)o;
            return super.equals(o) && op == x.op;
        } else {
            return false;
        }
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        op = ExtUtil.readInt(in);
        super.readExternal(in, pf);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.writeNumeric(out, op);
        super.writeExternal(out);
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

}
