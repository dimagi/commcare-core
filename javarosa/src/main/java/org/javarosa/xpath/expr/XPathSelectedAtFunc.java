package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSelectedAtFunc extends XPathFuncExpr {
    private static final String NAME = "selected-at";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSelectedAtFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSelectedAtFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return selectedAt(evaluatedArgs[0], evaluatedArgs[1]);
    }
}
