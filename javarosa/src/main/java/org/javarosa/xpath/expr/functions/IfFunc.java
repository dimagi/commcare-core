package org.javarosa.xpath.expr.functions;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IfFunc extends XPathFuncExpr {
    expectedArgCount = 3;

    @SuppressWarnings("unused")
    public IfFunc() {
        // for deserialization
    }

    public IfFunc(XPathExpression[] args) {
        id = "if";
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        if (toBoolean(args[0].eval(model, evalContext))) {
            return args[1].eval(model, evalContext);
        } else {
            return args[2].eval(model, evalContext);
        }
    }
}
