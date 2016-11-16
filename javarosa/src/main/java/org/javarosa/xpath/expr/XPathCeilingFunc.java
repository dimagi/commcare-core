package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathCeilingFunc extends XPathFuncExpr {
    private static final String NAME = "ceiling";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCeilingFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCeilingFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return new Double(Math.ceil(FunctionUtils.toDouble(evaluatedArgs[0])));
    }
}
