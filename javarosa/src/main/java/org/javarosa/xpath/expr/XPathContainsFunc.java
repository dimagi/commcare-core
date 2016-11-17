package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathContainsFunc extends XPathFuncExpr {
    public static final String NAME = "contains";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathContainsFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathContainsFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toString(evaluatedArgs[0]).contains(FunctionUtils.toString(evaluatedArgs[1]));
    }
}
