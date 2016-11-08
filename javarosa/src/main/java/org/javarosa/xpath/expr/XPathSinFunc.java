package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSinFunc extends XPathFuncExpr {
    private static final String NAME = "sin";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathSinFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSinFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return sin(evaluatedArgs[0]);
    }

    /**
     * Returns the sine of the argument, expressed in radians.
     */
    private static Double sin(Object o) {
        double value = toDouble(o);
        return Math.sin(value);
    }
}
