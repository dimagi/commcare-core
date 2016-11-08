package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathLazyNodeset;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathPositionFunc extends XPathFuncExpr {
    private static final String NAME = "position";
    // 0 or 1 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathPositionFunc() {
        id = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathPositionFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length > 1) {
            throw new XPathArityException(id, "0 or one arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 1) {
            XPathNodeset expr = (XPathNodeset)evaluatedArgs[0];
            try {
                return position(expr.getRefAt(0));
            } catch (ArrayIndexOutOfBoundsException e) {
                if (expr instanceof XPathLazyNodeset) {
                    throw new XPathTypeMismatchException("Unable to evaluate `position` on " + ((XPathLazyNodeset)expr).getUnexpandedRefString() + ", which is empty.");
                } else {
                    throw new XPathTypeMismatchException("Unable to evaluate `position` on empty reference in the context of " + evalContext.getContextRef());
                }
            }
        } else if (evalContext.getContextPosition() != -1) {
            return new Double(evalContext.getContextPosition());
        } else {
            return position(evalContext.getContextRef());
        }
    }

    private static Double position(TreeReference refAt) {
        return new Double(refAt.getMultLast());
    }

}
