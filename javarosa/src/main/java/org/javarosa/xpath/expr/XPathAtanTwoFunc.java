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
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return atan2(evaluatedArgs[0], evaluatedArgs[1]);
    }

    private static Double atan2(Object o1, Object o2) {
        double value1 = toDouble(o1);
        double value2 = toDouble(o2);
        return Math.atan2(value1, value2);
    }
}
