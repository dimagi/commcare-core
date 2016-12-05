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

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Raises a number to an exponent, b^n\n"
                + "Return: The value of the the first argument, raised to the power of the second argument\n"
                + "Arguments:\n"
                + "\tThe first argument is a numeric value\n"
                + "\tThe second argument is the numeric exponent to which the first argument should be raised. It can be a negative value.\n"
                + "NOTE: Due to technical restrictions the Exponent can only be an integer (non-decimal) value on Java Phones (Nokia, etc.). Decimal values can be used elsewhere.\n"
                + "Syntax: pow(value, exponent)\n"
                + "Example:  pow(2.5, 2)";
    }
}
