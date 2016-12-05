package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathDoubleFunc extends XPathFuncExpr {
    public static final String NAME = "double";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathDoubleFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDoubleFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toDouble(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will convert a string (ex. \"34.3\") or a integer value into a double.\n"
                + "Return: Returns a double number based on the passed in argument.\n"
                + "Arguments: The value to be converted\n"
                + "Syntax: double(value_to_convert)\n"
                + "Example: double(45) or double(\"45\") will return 45.0. You can also directly reference another question - double(/data/my_question).";
    }
}
