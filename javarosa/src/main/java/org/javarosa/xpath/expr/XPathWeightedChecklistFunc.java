package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathWeightedChecklistFunc extends XPathFuncExpr {
    private static final String NAME = "weighted-checklist";
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathWeightedChecklistFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathWeightedChecklistFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (!(args.length >= 2 && args.length % 2 == 0)) {
            throw new XPathArityException(id, "an even number of arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 4 && evaluatedArgs[2] instanceof XPathNodeset && evaluatedArgs[3] instanceof XPathNodeset) {
            Object[] factors = ((XPathNodeset)evaluatedArgs[2]).toArgList();
            Object[] weights = ((XPathNodeset)evaluatedArgs[3]).toArgList();
            if (factors.length != weights.length) {
                throw new XPathTypeMismatchException("weighted-checklist: nodesets not same length");
            }
            return checklistWeighted(evaluatedArgs[0], evaluatedArgs[1], factors, weights);
        } else {
            return checklistWeighted(evaluatedArgs[0], evaluatedArgs[1], subsetArgList(evaluatedArgs, 2, 2), subsetArgList(evaluatedArgs, 3, 2));
        }
    }

    /**
     * very similar to checklist, only each factor is assigned a real-number 'weight'.
     *
     * the first and second args are again the minimum and maximum, but -1 no longer means
     * 'not applicable'.
     *
     * subsequent arguments come in pairs: first the boolean value, then the floating-point
     * weight for that value
     *
     * the weights of all the 'true' factors are summed, and the function returns whether
     * this sum is between the min and max
     */
    private static Boolean checklistWeighted(Object oMin, Object oMax, Object[] flags, Object[] weights) {
        double min = toNumeric(oMin);
        double max = toNumeric(oMax);

        double sum = 0.;
        for (int i = 0; i < flags.length; i++) {
            boolean flag = toBoolean(flags[i]);
            double weight = toNumeric(weights[i]);

            if (flag)
                sum += weight;
        }

        return sum >= min && sum <= max;
    }
}
