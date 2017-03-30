package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.CompressingIdGenerator;
import org.javarosa.xpath.XPathException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathIdCompressFunc extends XPathFuncExpr {
    public static final String NAME = "id-compress";
    private static final int EXPECTED_ARG_COUNT = 5;

    public XPathIdCompressFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIdCompressFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext,
                           Object[] evaluatedArgs) {
        int input = FunctionUtils.toInt(evaluatedArgs[0]).intValue();
        String growthDigits = FunctionUtils.toString(evaluatedArgs[1]);
        String leadDigits = FunctionUtils.toString(evaluatedArgs[2]);
        String bodyDigits = FunctionUtils.toString(evaluatedArgs[3]);
        int fixedBodyLength = FunctionUtils.toInt(evaluatedArgs[4]).intValue();

        try {
            return CompressingIdGenerator.generateCompressedIdString(input, growthDigits,
                    leadDigits, bodyDigits, fixedBodyLength);
        } catch(IllegalArgumentException iae) {
            throw new XPathException(iae.getMessage());
        }
    }
}
