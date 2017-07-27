package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathMinFunc extends XPathFuncExpr {
    public static final String NAME = "min";
    // zero or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathMinFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathMinFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        // Any number of arguments are accepted
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        if (evaluatedArgs.length == 1 && evaluatedArgs[0] instanceof XPathNodeset) {
            return min(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            return min(evaluatedArgs);
        }
    }

    private static Object min(Object[] argVals) {
        if(argVals.length < 1){
            return Double.NaN;
        }

        double min = Double.MAX_VALUE;
        for (Object argVal : argVals) {
            min = Math.min(min, FunctionUtils.toNumeric(argVal));
        }
        return min;
    }


}
