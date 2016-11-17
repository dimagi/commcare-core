package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSubstringAfterFunc extends XPathFuncExpr {
    public static final String NAME = "substring-after";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSubstringAfterFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSubstringAfterFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return substringAfter(evaluatedArgs[0], evaluatedArgs[1]);
    }

    private static String substringAfter(Object fullStringAsRaw, Object substringAsRaw) {
        String fullString = FunctionUtils.toString(fullStringAsRaw);
        String subString = FunctionUtils.toString(substringAsRaw);

        if (fullString.length() == 0) {
            return "";
        }

        int substringIndex = fullString.indexOf(subString);
        if (substringIndex == -1) {
            return fullString;
        } else {
            return fullString.substring(substringIndex + subString.length(), fullString.length());
        }
    }


    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Takes two strings, a base string and a query string and returns the substring of the base string that follows the first occurrence of the query string, or the empty string if the base string does not contain the query string\n"
                + "Return: A substring of the first argument\n"
                + "Arguments: A base string and a query string.\n"
                + "Syntax: substring-after(full_string, substring)\n"
                + "Example: substring-after('hello_there', 'hello_') -> \"there\"";
    }
}
