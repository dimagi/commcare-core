package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathIfFunc extends XPathFuncExpr {

    public XPathIfFunc() {
        id = "if";
        expectedArgCount = 3;
    }

    public XPathIfFunc(XPathExpression[] args) throws XPathSyntaxException {
        this();
        this.args = args;
        validateArgCount();
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length != expectedArgCount) {
            String msg = id + "() function requires "
                    + expectedArgCount + " arguments but "
                    + args.length + " are present.";
            throw new XPathSyntaxException(msg);
        }
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
