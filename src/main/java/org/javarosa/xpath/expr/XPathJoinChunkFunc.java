package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathJoinChunkFunc extends XPathFuncExpr {
    public static final String NAME = "join-chunked";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathJoinChunkFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathJoinChunkFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 3) {
            throw new XPathArityException(name, "at least three arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        if (args.length == 3 && evaluatedArgs[2] instanceof XPathNodeset) {
            return join(evaluatedArgs[0], evaluatedArgs[1], ((XPathNodeset)evaluatedArgs[2]).toArgList());
        } else {
            return join(evaluatedArgs[0], evaluatedArgs[1], FunctionUtils.subsetArgList(evaluatedArgs, 2));
        }
    }

    /**
     * concatenate an abritrary-length argument list of string values together
     */
    public static String join(Object oSep, Object oChunkSize, Object[] argVals) {
        String sep = FunctionUtils.toString(oSep);
        int chunkSize = FunctionUtils.toInt(oChunkSize).intValue();
        StringBuffer intermediateBuffer = new StringBuffer();
        StringBuffer outputBuffer = new StringBuffer();

        for (int i = 0; i < argVals.length; i++) {
            intermediateBuffer.append(FunctionUtils.toString(argVals[i]));
        }

        for(int i = 0 ; i < intermediateBuffer.length() ; ++i) {
            if( i != 0 && (i % chunkSize ==0)) {
                outputBuffer.append(sep);
            }
            outputBuffer.append(intermediateBuffer.charAt(i));
        }

        return outputBuffer.toString();
    }


}
