package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathIfFunc extends XPathFuncExpr {
    private static final String NAME = "if";
    private static final int EXPECTED_ARG_COUNT = 3;

    public XPathIfFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIfFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length != expectedArgCount) {
            String msg = name + "() function requires "
                    + expectedArgCount + " arguments but "
                    + args.length + " are present.";
            throw new XPathSyntaxException(msg);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (toBoolean(args[0].eval(model, evalContext))) {
            return args[1].eval(model, evalContext);
        } else {
            return args[2].eval(model, evalContext);
        }
    }
}
