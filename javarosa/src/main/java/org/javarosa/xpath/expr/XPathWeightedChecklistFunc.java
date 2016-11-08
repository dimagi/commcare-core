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
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

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
}
