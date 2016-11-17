package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathPowFunc extends XPathFuncExpr {
    public static final String NAME = "pow";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathPowFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathPowFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return power(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * Best faith effort at getting a result for math.pow
     *
     * @param o1 The base number
     * @param o2 The exponent of the number that it is to be raised to
     * @return An approximation of o1 ^ o2. If there is a native power
     * function, it is utilized. It there is not, a recursive exponent is
     * run if (b) is an integer value, and a taylor series approximation is
     * used otherwise.
     */
    private static Double power(Object o1, Object o2) {
        double a = FunctionUtils.toDouble(o1);
        double b = FunctionUtils.toDouble(o2);

        return Math.pow(a, b);
    }
}
