package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSubstrFunc extends XPathFuncExpr {
    private static final String NAME = "substr";
    // 2 or 3 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathSubstrFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSubstrFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() {
        if (!(args.length == 2 || args.length == 3)) {
            throw new XPathArityException(id, "two or three arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return substring(evaluatedArgs[0], evaluatedArgs[1], args.length == 3 ? evaluatedArgs[2] : null);
    }
}
