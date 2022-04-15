package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;


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
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
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

        try {
            return Pattern.compile(re).matcher(str).find();
        } catch (PatternSyntaxException e) {
            throw new XPathException("The regular expression '" + re + "' is invalid.");
        }
    }


}
