package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathExpFunc extends XPathFuncExpr {
    public static final String NAME = "exp";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathExpFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathExpFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.exp(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Raises Euler's constant to the power of the provided number\n"
                + "Return: A number representing e^x, where e is Euler's number and x is the argument.\n"
                + "Arguments: A number to act as the exponent\n"
                + "Syntax: exp(value_to_convert)\n"
                + "Example: exp(0) -> 1";
    }
}
