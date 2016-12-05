package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathLogFunc extends XPathFuncExpr {
    public static final String NAME = "log";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathLogFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathLogFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.log(FunctionUtils.toDouble(evaluatedArgs[0]));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Takes the natural logarithm of a number\n"
                + "Return: The natural logarithm of the argument passed to the function\n"
                + "Arguments: The only argument is the number whose natural logarithm you want to take\n"
                + "NOTE: A negative argument will return a blank value.\n"
                + "Syntax: log(number)\n"
                + "Example: log(2.49)";
    }
}
