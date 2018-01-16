package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.MathUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathRandomFunc extends XPathFuncExpr implements UncacheableXPathFuncExpr {
    public static final String NAME = "random";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathRandomFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathRandomFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        //calculated expressions may be recomputed w/o warning! use with caution!!
        return new Double(MathUtils.getRand().nextDouble());
    }

    @Override
    protected boolean expressionIsCacheable(Object result) {
        return false;
    }

}
