package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathStartsWithFunc extends XPathFuncExpr {
    public static final String NAME = "starts-with";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathStartsWithFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathStartsWithFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        return FunctionUtils.toString(evaluatedArgs[0]).startsWith(FunctionUtils.toString(evaluatedArgs[1]));
    }

}
