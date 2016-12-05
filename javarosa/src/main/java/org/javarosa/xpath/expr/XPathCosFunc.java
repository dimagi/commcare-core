package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathCosFunc extends XPathFuncExpr {
    public static final String NAME = "cos";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCosFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCosFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.cos(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Finds the cos of a number.\n"
                + "Return: The cos of the argument passed to the function\n"
                + "Arguments: One number\n"
                + "Syntax: cos(number)";
    }
}
