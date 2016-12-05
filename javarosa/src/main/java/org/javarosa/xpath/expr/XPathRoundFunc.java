package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathRoundFunc extends XPathFuncExpr {
    public static final String NAME = "round";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathRoundFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathRoundFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return new Double(Math.floor(FunctionUtils.toDouble(evaluatedArgs[0]) + 0.5));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Rounds a number to the nearest integer\n"
                + "Return: The argument passed to the function, rounded to the nearest integer.\n"
                + "\tNOTE: Rounding negative numbers can be counter-intuitive. round(1.5) returns 2, while round(-1.5) returns -1.\n"
                + "Arguments: The only argument is the number you want to round\n"
                + "Syntax: round(number)\n"
                + "Example: round(2.49)";
    }
}
