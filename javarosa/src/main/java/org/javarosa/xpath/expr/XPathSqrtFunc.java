package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSqrtFunc extends XPathFuncExpr {
    public static final String NAME = "sqrt";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathSqrtFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSqrtFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.sqrt(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Calculates the square root of a number\n"
                + "Return: A double value that represents the square root of the provided argument.\n"
                + "Arguments: An expression that evaluates to a number\n"
                + "Syntax: sqrt(expression)\n"
                + "Example: sqrt(4) -> 2.0";
    }
}
