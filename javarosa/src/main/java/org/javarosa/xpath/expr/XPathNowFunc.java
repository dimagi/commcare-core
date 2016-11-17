package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathNowFunc extends XPathFuncExpr {
    public static final String NAME = "now";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathNowFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathNowFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return new Date();
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return:  Returns the current date.and time\n"
                + "Arguments: None\n"
                + "Usage: now()\n"
                + "Example Usage: You may want to use this if you want to calculate the current date and time in a hidden value. When saved to a case, will only save the date portion without the time. If the time portion is important, convert to a number before saving to a case: double(now()).";
    }
}
