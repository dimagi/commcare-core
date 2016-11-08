package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathCountFunc extends XPathFuncExpr {
    private static final String NAME = "count";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCountFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCountFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        evaluateArguments(model, evalContext);

        return count(evaluatedArgs[0]);
    }

    /**
     * count the number of nodes in a nodeset
     */
    public static Double count(Object o) {
        if (o instanceof XPathNodeset) {
            return new Double(((XPathNodeset)o).size());
        } else {
            throw new XPathTypeMismatchException("not a nodeset");
        }
    }

}
