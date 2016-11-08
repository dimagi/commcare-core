package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathTanFunc extends XPathFuncExpr {
    private static final String NAME = "tan";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathTanFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTanFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return tan(evaluatedArgs[0]);
    }

    /**
     * Returns the tangent of the argument, expressed in radians.
     */
    private static Double tan(Object o) {
        double value = toDouble(o);
        return Math.tan(value);
    }
}
