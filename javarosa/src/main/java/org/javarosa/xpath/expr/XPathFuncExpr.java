package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.IFunctionHandler;
import org.javarosa.core.model.condition.pivot.UnpivotableExpressionException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapListPoly;
import org.javarosa.core.util.externalizable.PrototypeFactory;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Base class for xpath function expressions.
 * Dispatches to runtime function overrides when they exist.
 */
public abstract class XPathFuncExpr extends XPathExpression {
    protected String name;
    public XPathExpression[] args;
    protected Object[] evaluatedArgs;
    protected int expectedArgCount;
    private boolean evaluateArgsFirst;

    @SuppressWarnings("unused")
    public XPathFuncExpr() {
        // for deserialization
    }

    public XPathFuncExpr(String name, XPathExpression[] args,
                         int expectedArgCount, boolean evaluateArgsFirst)
            throws XPathSyntaxException {
        this.name = name;
        this.args = args;
        this.expectedArgCount = expectedArgCount;
        this.evaluateArgsFirst = evaluateArgsFirst;

        validateArgCount();
    }

    @Override
    public final Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        IFunctionHandler handler = evalContext.getFunctionHandlers().get(name);
        if (handler != null) {
            return XPathCustomRuntimeFunc.evalCustomFunction(handler, evaluatedArgs, evalContext);
        } else {
            return evalBody(model, evalContext);
        }
    }

    private void evaluateArguments(DataInstance model, EvaluationContext evalContext) {
        if (evaluatedArgs == null) {
            evaluatedArgs = new Object[args.length];
        }
        if (evaluateArgsFirst) {
            for (int i = 0; i < args.length; i++) {
                evaluatedArgs[i] = args[i].eval(model, evalContext);
            }
        }
    }

    protected abstract Object evalBody(DataInstance model, EvaluationContext evalContext);

    //protected abstract String docs();

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        sb.append("{func-expr:");
        sb.append(name);
        sb.append(",{");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].toString());
            if (i < args.length - 1)
                sb.append(",");
        }
        sb.append("}}");

        return sb.toString();
    }

    @Override
    public String toPrettyString() {
        StringBuffer sb = new StringBuffer();
        sb.append(name).append("(");
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].toPrettyString());
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof XPathFuncExpr) {
            XPathFuncExpr x = (XPathFuncExpr)o;

            //Shortcuts for very easily comprable values
            //We also only return "True" for methods we expect to return the same thing. This is not good
            //practice in Java, since o.equals(o) will return false. We should evaluate that differently.
            //Dec 8, 2011 - Added "uuid", since we should never assume one uuid equals another
            //May 6, 2013 - Added "random", since two calls asking for a random
            if (!name.equals(x.name) || args.length != x.args.length || name.equals("uuid") || name.equals("random")) {
                return false;
            }

            return ExtUtil.arrayEquals(args, x.args, false);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int argsHash = 0;
        for (XPathExpression arg : args) {
            argsHash ^= arg.hashCode();
        }
        return name.hashCode() ^ argsHash;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf)
            throws IOException, DeserializationException {
        expectedArgCount = ExtUtil.readInt(in);
        evaluateArgsFirst = ExtUtil.readBool(in);

        Vector v = (Vector)ExtUtil.read(in, new ExtWrapListPoly(), pf);
        args = new XPathExpression[v.size()];
        for (int i = 0; i < args.length; i++) {
            args[i] = (XPathExpression)v.elementAt(i);
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, expectedArgCount);
        ExtUtil.write(out, evaluateArgsFirst);

        Vector<XPathExpression> v = new Vector<>();
        for (XPathExpression arg : args) {
            v.addElement(arg);
        }
        ExtUtil.write(out, new ExtWrapListPoly(v));
    }

    @Override
    public Object pivot(DataInstance model, EvaluationContext evalContext, Vector<Object> pivots, Object sentinal) throws UnpivotableExpressionException {
        //for now we'll assume that all that functions do is return the composition of their components
        Object[] argVals = new Object[args.length];

        //Identify whether this function is an identity: IE: can reflect back the pivot sentinal with no modification
        String[] identities = new String[]{"string-length"};
        boolean id = false;
        for (String identity : identities) {
            if (identity.equals(id)) {
                id = true;
            }
        }

        //get each argument's pivot
        for (int i = 0; i < args.length; i++) {
            argVals[i] = args[i].pivot(model, evalContext, pivots, sentinal);
        }

        boolean pivoted = false;
        //evaluate the pivots
        for (Object argVal : argVals) {
            if (argVal == null) {
                //one of our arguments contained pivots,
                pivoted = true;
            } else if (sentinal.equals(argVal)) {
                //one of our arguments is the sentinal, return the sentinal if possible
                if (id) {
                    return sentinal;
                } else {
                    //This function modifies the sentinal in a way that makes it impossible to capture
                    //the pivot.
                    throw new UnpivotableExpressionException();
                }
            }
        }

        if (pivoted) {
            if (id) {
                return null;
            } else {
                //This function modifies the sentinal in a way that makes it impossible to capture
                //the pivot.
                throw new UnpivotableExpressionException();
            }
        }

        //TODO: Inner eval here with eval'd args to improve speed
        return eval(model, evalContext);
    }

    protected void validateArgCount() throws XPathSyntaxException {
        if (expectedArgCount != args.length) {
            throw new XPathArityException(name, expectedArgCount, args.length);
        }
    }

    public String getDocs() {
        return "docs...";
    }
}
