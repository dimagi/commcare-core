package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathFalseFunc extends XPathFuncExpr {
    private static final String NAME = "false";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathFalseFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFalseFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        return Boolean.FALSE;
    }
}
