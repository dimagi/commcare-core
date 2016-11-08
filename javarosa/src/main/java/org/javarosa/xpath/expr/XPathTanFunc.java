package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathTanFunc extends XPathFuncExpr {
    public XPathTanFunc() {
        id = "";
        // at least 2 arguments
        expectedArgCount = -1;
    }

    public XPathTanFunc(XPathExpression[] args) throws XPathSyntaxException {
        this();
        this.args = args;
        validateArgCount();
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
    }
}
