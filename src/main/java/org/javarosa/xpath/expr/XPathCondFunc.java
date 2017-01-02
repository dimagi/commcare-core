package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class XPathCondFunc extends XPathFuncExpr {
    public static final String NAME = "cond";
    // expects at least 3 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathCondFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCondFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 3) {
            throw new XPathSyntaxException(name + "() function requires at least 3 arguments. " + args.length + " arguments provided.");
        } else if (args.length % 2 != 1) {
            throw new XPathSyntaxException(name + "() function requires an odd number of arguments. " + args.length + " arguments provided.");
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        for (int i = 0; i < args.length - 2; i+=2) {
            if (FunctionUtils.toBoolean(args[i].eval(model, evalContext))) {
                return args[i+1].eval(model, evalContext);
            }
        }

        return args[args.length-1].eval(model, evalContext);
    }

}
