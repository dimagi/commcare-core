package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathExpFunc extends XPathFuncExpr {
    private static final String NAME = "exp";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathExpFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathExpFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);
        return exp(evaluatedArgs[0]);
    }

    private static Double exp(Object o) {
        double value = toDouble(o);
        return Math.exp(value);
    }

}
