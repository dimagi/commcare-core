package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathNumberFunc extends XPathFuncExpr {
    public static final String NAME = "number";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathNumberFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathNumberFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toNumeric(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return: Returns a number based on the passed in argument.\n"
                + "Behavior: Will convert a string (ex. \"34.3\") into a number.  Will cause an error if the passed in argument is not a number (ex. \"two\").\n"
                + "Arguments:  The value to be converted\n"
                + "Syntax: number(value_to_convert)\n"
                + "Example:  If your application has a string value that needs to be stored as number.  number(/data/my_string_number) or number(\"453\")";
    }
}
