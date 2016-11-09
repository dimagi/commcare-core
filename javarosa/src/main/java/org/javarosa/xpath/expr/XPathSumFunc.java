package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSumFunc extends XPathFuncExpr {
    private static final String NAME = "sum";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathSumFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSumFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (evaluatedArgs[0] instanceof XPathNodeset) {
            return sum(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            throw new XPathTypeMismatchException("not a nodeset");
        }
    }

    /**
     * sum the values in a nodeset; each element is coerced to a numeric value
     */
    private static Double sum(Object argVals[]) {
        double sum = 0.0;
        for (Object argVal : argVals) {
            sum += toNumeric(argVal);
        }
        return sum;
    }

}
