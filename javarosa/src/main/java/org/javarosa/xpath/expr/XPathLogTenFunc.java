package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathLogTenFunc extends XPathFuncExpr {
    private static final String NAME = "log10";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathLogTenFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathLogTenFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return log10(evaluatedArgs[0]);
    }

    /**
     * Implementation of logarithm with base ten
     *
     * @return Base ten log of value
     */
    private static Double log10(Object o) {
        double value = FunctionUtils.toDouble(o);
        return Math.log10(value);
    }
}
