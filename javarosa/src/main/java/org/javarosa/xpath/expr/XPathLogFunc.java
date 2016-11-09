package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathLogFunc extends XPathFuncExpr {
    private static final String NAME = "log";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathLogFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathLogFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return log(evaluatedArgs[0]);
    }

    /**
     * Implementation of natural logarithm
     *
     * @return Natural log of value
     */
    private static Double log(Object o) {
        double value = FunctionUtils.toDouble(o);
        return Math.log(value);
    }
}
