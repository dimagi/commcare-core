package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathDependFunc extends XPathFuncExpr {
    private static final String NAME = "depend";
    // at least one argument
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathDependFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathDependFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 1) {
            throw new XPathArityException(id, "at least one argument", args.length);
        }
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return evaluatedArgs[0];
    }
}
