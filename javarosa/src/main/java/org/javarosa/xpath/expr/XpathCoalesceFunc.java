package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XpathCoalesceFunc extends XPathFuncExpr {
    public XpathCoalesceFunc() {
        id = "coalesce";
        // at least 2 arguments
        expectedArgCount = -1;
    }

    public XpathCoalesceFunc(XPathExpression[] args) throws XPathSyntaxException {
        this();
        this.args = args;
        validateArgCount();
    }

    @Override
    public Object evalRaw(DataInstance model, EvaluationContext evalContext) {
        //Not sure if unpacking here is quiiite right, but it seems right
        for (int i = 0; i < args.length - 1; i++) {
            Object evaluatedArg = XPathFuncExpr.unpack(args[i].eval(model, evalContext));
            if (!isNull(evaluatedArg)) {
                return evaluatedArg;
            }
        }
        return args[args.length - 1].eval(model, evalContext);
    }
}
