package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathTodayFunc extends XPathFuncExpr {
    private static final String NAME = "today";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathTodayFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTodayFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return DateUtils.roundDate(new Date());
    }
}
