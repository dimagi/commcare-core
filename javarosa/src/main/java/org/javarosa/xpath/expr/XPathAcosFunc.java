package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathAcosFunc extends XPathFuncExpr {
    private static final String NAME = "acos";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathAcosFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathAcosFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return acos(evaluatedArgs[0]);
    }

    /**
     * Returns the arc cosine of the argument, expressed in radians.
     */
    private static Double acos(Object o) {
        double value = toDouble(o);
        return Math.acos(value);
    }

}
