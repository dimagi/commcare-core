package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathCountSelectedFunc extends XPathFuncExpr {
    private static final String NAME = "count-selected";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCountSelectedFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCountSelectedFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return countSelected(evaluatedArgs[0]);
    }
}
