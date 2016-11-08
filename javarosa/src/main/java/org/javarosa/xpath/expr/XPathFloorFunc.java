package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathFloorFunc extends XPathFuncExpr {
    public XPathFloorFunc() {
        id = "";
        // at least 2 arguments
        expectedArgCount = -1;
    }

    public XPathFloorFunc(XPathExpression[] args) throws XPathSyntaxException {
        this();
        this.args = args;
        validateArgCount();
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
    }
}
