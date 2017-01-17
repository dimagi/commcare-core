package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.PropertyUtils;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathUuidFunc extends XPathFuncExpr {
    public static final String NAME = "uuid";
    // 0 or 1 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathUuidFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathUuidFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length > 1) {
            throw new XPathArityException(name, "0 or one arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        //calculated expressions may be recomputed w/o warning! use with caution!!
        if (args.length == 0) {
            return PropertyUtils.genUUID();
        }

        int len = FunctionUtils.toInt(evaluatedArgs[0]).intValue();
        return PropertyUtils.genGUID(len);
    }

}
