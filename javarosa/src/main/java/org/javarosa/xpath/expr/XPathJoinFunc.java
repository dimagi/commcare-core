package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathJoinFunc extends XPathFuncExpr {
    public static final String NAME = "join";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathJoinFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathJoinFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 1) {
            throw new XPathArityException(name, "at least one argument", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 2 && evaluatedArgs[1] instanceof XPathNodeset) {
            return join(evaluatedArgs[0], ((XPathNodeset)evaluatedArgs[1]).toArgList());
        } else {
            return join(evaluatedArgs[0], FunctionUtils.subsetArgList(evaluatedArgs, 1));
        }
    }

    /**
     * concatenate an abritrary-length argument list of string values together
     */
    public static String join(Object oSep, Object[] argVals) {
        String sep = FunctionUtils.toString(oSep);
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < argVals.length; i++) {
            sb.append(FunctionUtils.toString(argVals[i]));
            if (i < argVals.length - 1)
                sb.append(sep);
        }

        return sb.toString();
    }

}
