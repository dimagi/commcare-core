package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAtanFunc extends XPathFuncExpr {
    private static final String NAME = "atan";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathAtanFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAtanFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return atan(evaluatedArgs[0]);
    }

    /**
     * Returns the arc tan of the argument, expressed in radians.
     */
    private static Double atan(Object o) {
        double value = toDouble(o);
        return Math.atan(value);
    }
}
