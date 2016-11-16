package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathRoundFunc extends XPathFuncExpr {
    private static final String NAME = "round";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathRoundFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathRoundFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return new Double(Math.floor(FunctionUtils.toDouble(evaluatedArgs[0]) + 0.5));
    }
}
