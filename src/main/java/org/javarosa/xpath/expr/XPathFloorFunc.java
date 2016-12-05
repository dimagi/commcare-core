package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathFloorFunc extends XPathFuncExpr {
    public static final String NAME = "floor";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathFloorFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFloorFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return new Double(Math.floor(FunctionUtils.toDouble(evaluatedArgs[0])));
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Finds the largest integer that is less than or equal to a number\n"
                + "Return: The largest integer that is less than or equal to the given number\n"
                + "Arguments: The only argument is the number whose floor you want\n"
                + "Syntax: floor(number)\n"
                + "Example: floor(2.49)";
    }
}
