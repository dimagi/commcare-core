package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAtanTwoFunc extends XPathFuncExpr {
    private static final String NAME = "atan2";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathAtanTwoFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAtanTwoFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        double value1 = toDouble(evaluatedArgs[0]);
        double value2 = toDouble(evaluatedArgs[1]);
        return Math.atan2(value1, value2);
    }
}
