package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathEndsWithFunc extends XPathFuncExpr {
    private static final String NAME = "ends-with";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathEndsWithFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathEndsWithFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return toString(evaluatedArgs[0]).endsWith(toString(evaluatedArgs[1]));
    }
}
