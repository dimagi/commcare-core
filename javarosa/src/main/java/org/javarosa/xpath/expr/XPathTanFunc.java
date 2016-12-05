package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathTanFunc extends XPathFuncExpr {
    public static final String NAME = "tan";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathTanFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTanFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.tan(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Finds the tan of a number.\n"
                + "Return: The tan of the argument passed to the function\n"
                + "Arguments: One number\n"
                + "Syntax: tan(number)";
    }
}
