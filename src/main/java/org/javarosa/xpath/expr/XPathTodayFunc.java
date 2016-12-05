package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathTodayFunc extends XPathFuncExpr {
    public static final String NAME = "today";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathTodayFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTodayFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return DateUtils.roundDate(new Date());
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return:  Returns the current date.\n"
                + "Arguments: None\n"
                + "Usage: today()\n"
                + "Example Usage: You may want to use this when comparing against a user entered date.  For example, you may want to check that entered EDD is in the future (/data/edd > today()).";
    }
}
