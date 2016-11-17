package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathTrueFunc extends XPathFuncExpr {
    public static final String NAME = "true";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathTrueFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTrueFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Boolean.TRUE;
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return:  Returns the boolean value True\n"
                + "Arguments: None\n"
                + "Syntax: true()\n"
                + "Example: You may want to use true() when you have some advanced logic for a display or validation condition.  You could also use them if you want to calculate true for a hidden value.  For example, if(/data/question1 = \"yes\" and /data/question2 > 30, true(), false())";
    }
}
