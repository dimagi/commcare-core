package org.javarosa.xpath.expr;

import org.apache.regexp.RE;
import org.apache.regexp.RESyntaxException;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;


public class XPathReplaceFunc extends XPathFuncExpr {
    public static final String NAME = "replace";
    private static final int EXPECTED_ARG_COUNT = 3;

    public XPathReplaceFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathReplaceFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return replace(evaluatedArgs[0], evaluatedArgs[1], evaluatedArgs[2]);
    }

    /**
     * Regex-based replacement.
     *
     * @param o1 String to manipulate
     * @param o2 Pattern to search for
     * @param o3 Replacement string. Contrary to the XPath spec, this function does NOT
     *           support backreferences (e.g., replace("abbc", "a(.*)c", "$1") will return "a$1c", not "bb").
     * @return String
     */
    private static String replace(Object o1, Object o2, Object o3) {
        String source = FunctionUtils.toString(o1);
        String regexString = FunctionUtils.toString(o2);
        RE pattern;
        try {
            pattern = new RE(regexString);
        } catch (RESyntaxException e) {
            throw new XPathException("The regular expression '" + regexString + "' is invalid.");
        }
        String replacement = FunctionUtils.toString(o3);
        return pattern.subst(source, replacement);
    }

}
