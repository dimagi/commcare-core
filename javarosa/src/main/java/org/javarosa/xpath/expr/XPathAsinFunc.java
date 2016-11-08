package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAsinFunc extends XPathFuncExpr {
    private static final String NAME = "asin";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathAsinFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAsinFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return asin(evaluatedArgs[0]);
    }

    /**
     * Returns the arc sine of the argument, expressed in radians.
     */
    private static Double asin(Object o) {
        double value = toDouble(o);
        return Math.asin(value);
    }
}
