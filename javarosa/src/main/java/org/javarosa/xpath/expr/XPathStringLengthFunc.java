package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathStringLengthFunc extends XPathFuncExpr {
    private static final String NAME = "string-length";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathStringLengthFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathStringLengthFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return stringLength(evaluatedArgs[0]);
    }

    private static Double stringLength(Object o) {
        String s = toString(o);
        if (s == null) {
            return new Double(0.0);
        }
        return new Double(s.length());
    }

}
