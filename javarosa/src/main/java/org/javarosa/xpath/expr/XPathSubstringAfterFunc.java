package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSubstringAfterFunc extends XPathFuncExpr {
    private static final String NAME = "substring-after";
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
        String fullString = toString(fullStringAsRaw);
        String subString = toString(substringAsRaw);

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

}
