package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAcosFunc extends XPathFuncExpr {
    public static final String NAME = "acos";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathAcosFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAcosFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return Math.acos(FunctionUtils.toDouble(evaluatedArgs[0]));
    }
}
