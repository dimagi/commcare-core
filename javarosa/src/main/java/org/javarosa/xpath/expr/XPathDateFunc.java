package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathDateFunc extends XPathFuncExpr {
    public static final String NAME = "date";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathDateFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDateFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toDate(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will convert a string or a number value into an equivalent date.  Will throw an error if the format of the string is wrong or an invalid date is passed.\n"
                + "Return: Returns a date\n"
                + "Arguments:  The value to be converted (either a string in the format YYYY-MM-DD or a number).\n"
                + "Syntax: date(value_to_convert)\n"
                + "Example:  If you have stored any date values in a case, they are actually stored as a string in the format YYYY-MM-DD.  You will need to convert them into a date prior to using that for date math or when formatting for display.  (ex. date(/data/case_edd)).\n"
                + "Notes: When working with dates prior to 1970 you should use date(floor(value_to_convert)) in order to avoid an issue where negative numbers are rounded incorrectly.";
    }
}
