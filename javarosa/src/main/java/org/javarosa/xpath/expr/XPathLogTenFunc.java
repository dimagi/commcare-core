package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathLogTenFunc extends XPathFuncExpr {
    public static final String NAME = "log10";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathLogTenFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathLogTenFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.log10(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Takes the base-10 logarithm of a number\n"
                + "Return: The base-10 logarithm of the argument passed to the function\n"
                + "Arguments: The only argument is the number whose base-10 logarithm you want to take\n"
                + "NOTE: A negative argument will return a blank value\n"
                + "Syntax: log10(number)\n"
                + "Example: log10(2.49)";
    }
}
