package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathTrueFunc extends XPathFuncExpr {
    private static final String NAME = "true";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathTrueFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathTrueFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return Boolean.TRUE;
    }
}
