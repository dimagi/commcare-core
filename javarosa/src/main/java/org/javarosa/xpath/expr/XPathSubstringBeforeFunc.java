package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSubstringBeforeFunc extends XPathFuncExpr {
    public static final String NAME = "substring-before";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSubstringBeforeFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSubstringBeforeFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return substringBefore(evaluatedArgs[0], evaluatedArgs[1]);
    }

    private static String substringBefore(Object fullStringAsRaw, Object substringAsRaw) {
        String fullString = FunctionUtils.toString(fullStringAsRaw);
        String subString = FunctionUtils.toString(substringAsRaw);

        if (fullString.length() == 0) {
            return "";
        }

        int substringIndex = fullString.indexOf(subString);
        if (substringIndex <= 0) {
            return "";
        } else {
            return fullString.substring(0, substringIndex);
        }
    }


    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Takes two strings, a base string and a query string and returns the substring of the base string that precedes the first occurrence of the query string, or the empty string if the base string does not contain the query string\n"
                + "Return: A substring of the first argument\n"
                + "Arguments: A base string and a query string.\n"
                + "Syntax: substring-before(full_string, substring)\n"
                + "Example: substring-before('hello_there', '_there'). In conjunction with string-length, this can calculate the index of the 1st occurrence of a query string: string-length(substring-before(base_string, query_string))+1";
    }
}
