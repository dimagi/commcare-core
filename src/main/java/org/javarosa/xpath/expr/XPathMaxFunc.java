package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathMaxFunc extends XPathFuncExpr {
    public static final String NAME = "max";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathMaxFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathMaxFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 1) {
            throw new XPathArityException(name, "at least one argument", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        if (evaluatedArgs.length == 1 && evaluatedArgs[0] instanceof XPathNodeset) {
            return max(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            return max(evaluatedArgs);
        }
    }

    /**
     * Identify the largest value from the list of provided values.
     */
    private static Object max(Object[] argVals) {
        if(argVals.length < 1){
            return Double.NaN;
        }

        double max = Double.NEGATIVE_INFINITY;
        for (Object argVal : argVals) {
            max = Math.max(max, FunctionUtils.toNumeric(argVal));
        }
        return max;
    }

}
