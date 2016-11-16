package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathChecklistFunc extends XPathFuncExpr {
    public static final String NAME = "checklist";
    // two or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathChecklistFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathChecklistFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 2) {
            throw new XPathArityException(name, "two or more arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 3 && evaluatedArgs[2] instanceof XPathNodeset) {
            return checklist(evaluatedArgs[0], evaluatedArgs[1], ((XPathNodeset)evaluatedArgs[2]).toArgList());
        } else {
            return checklist(evaluatedArgs[0], evaluatedArgs[1], FunctionUtils.subsetArgList(evaluatedArgs, 2));
        }
    }

    /**
     * perform a 'checklist' computation, enabling expressions like 'if there are at least 3 risk
     * factors active'
     *
     * @param oMin    a numeric value expressing the minimum number of factors required.
     *                if -1, no minimum is applicable
     * @param oMax    a numeric value expressing the maximum number of allowed factors.
     *                if -1, no maximum is applicable
     * @param factors individual factors that are coerced to boolean values
     * @return true if the count of 'true' factors is between the applicable minimum and maximum,
     * inclusive
     */
    private static Boolean checklist(Object oMin, Object oMax, Object[] factors) {
        int min = FunctionUtils.toNumeric(oMin).intValue();
        int max = FunctionUtils.toNumeric(oMax).intValue();

        int count = 0;
        for (Object factor : factors) {
            if (FunctionUtils.toBoolean(factor)) {
                count++;
            }
        }

        return (min < 0 || count >= min) && (max < 0 || count <= max);
    }

}
