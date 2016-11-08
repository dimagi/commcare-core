package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathStartsWithFunc extends XPathFuncExpr {
    private static final String NAME = "starts-with";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathStartsWithFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathStartsWithFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return toString(evaluatedArgs[0]).startsWith(toString(evaluatedArgs[1]));
    }
}
