package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathFalseFunc extends XPathFuncExpr {
    public static final String NAME = "false";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathFalseFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFalseFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Boolean.FALSE;
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return:  Returns the boolean value False\n"
                + "Arguments: None\n"
                + "Usage: false()\n"
                + "Example Usage: You may want to use false() when you have some advanced logic for a display or validation condition.  You could also use them if you want to calculate false for a hidden value.  For example, if(/data/question1 = \"yes\" and /data/question2 > 30, true(), false())";
    }
}
