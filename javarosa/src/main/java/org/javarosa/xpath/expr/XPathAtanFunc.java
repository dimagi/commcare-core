package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAtanFunc extends XPathFuncExpr {
    public static final String NAME = "atan";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathAtanFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAtanFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.atan(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Finds the arctan of a number.\n"
                + "Return: The arctan of the argument passed to the function\n"
                + "Arguments: One number\n"
                + "Syntax: atan(number)";
    }
}
