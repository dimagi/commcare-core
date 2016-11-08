package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathSelectedFunc extends XPathFuncExpr {
    // default to 'selected' but could be 'is-selected'
    // we could also serialize this if we wanted to really preserve it.
    private static final String NAME = "selected";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSelectedFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSelectedFunc(String name, XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);

        // keep function name from parsing instead of using default
        this.id = name;
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return multiSelected(evaluatedArgs[0], evaluatedArgs[1]);
    }
}
