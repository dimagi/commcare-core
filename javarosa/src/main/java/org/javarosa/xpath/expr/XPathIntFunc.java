package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

//non-standard
public class XPathIntFunc extends XPathFuncExpr {
    public static final String NAME = "int";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathIntFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIntFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toInt(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return: Returns a whole number based on the passed in argument.\n"
                + "Behavior: Will convert a string (ex. \"34.3\") or a decimal value into an integer.  It will round down (ex. 34.8 will be evaluated to 34).\n"
                + "Arguments: The value to be converted\n"
                + "Syntax: int(value_to_convert)\n"
                + "Example: int(45.6) or int(\"45.6\") will return 45.  You can also directly reference another question - int(/data/my_question).";
    }
}
