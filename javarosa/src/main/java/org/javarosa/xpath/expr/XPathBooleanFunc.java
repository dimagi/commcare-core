package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathBooleanFunc extends XPathFuncExpr {
    public static final String NAME = "boolean";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathBooleanFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathBooleanFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toBoolean(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: When passed a number, will return true if the number is not zero.  Otherwise it will return false.   When passed a string, will return true if the string is non-empty.\n"
                + "Return: Returns true or false based on the argument.\n"
                + "Arguments:  The value to be converted\n"
                + "Syntax: boolean(value_to_convert)\n"
                + "Example:  You may have stored a value that is 1 or 0 into a boolean for other logic.  boolean(/data/my_question) or boolean(23)";
    }
}
