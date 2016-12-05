package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import me.regexp.RE;
import me.regexp.RESyntaxException;

public class XPathRegexFunc extends XPathFuncExpr {
    public static final String NAME = "regex";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathRegexFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathRegexFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return regex(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * determine if a string matches a regular expression.
     *
     * @param o1 string being matched
     * @param o2 regular expression
     */
    private static Boolean regex(Object o1, Object o2) {
        String str = FunctionUtils.toString(o1);
        String re = FunctionUtils.toString(o2);

        RE regexp;
        try {
            regexp = new RE(re);
        } catch (RESyntaxException e) {
            throw new XPathException("The regular expression '" + str + "' is invalid.");
        }

        boolean result;
        try {
            result = regexp.match(str);
        } catch (java.lang.StackOverflowError e) {
            throw new XPathException("The regular expression '" + str + "' took too long to process.");
        }

        return result;
    }


    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Evaluates a value against a regular expression and returns true if the value matches that regular expression.\n"
                + "Return: true or false\n"
                + "Arguments:  There are two arguments, the value to be validated and the regular expression as a string.\n"
                + "Syntax: regex(value, regular_expression)\n"
                + "Example:  This is useful when doing complex validation against some value.  For example, to validate that a string contains only numbers, you can use regex(/data/my_question, \"[0 - 9] + \").   You can test and develop other regular expressions using http://www.regexr.com/.  Also see the Advanced Validation Conditions page.";
    }
}
