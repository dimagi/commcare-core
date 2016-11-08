package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathUpperCaseFunc extends XPathFuncExpr {
    private static final String NAME = "upper-case";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathUpperCaseFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathUpperCaseFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return normalizeCase(evaluatedArgs[0], true);
    }
}
