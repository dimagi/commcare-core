package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathFormatDateFunc extends XPathFuncExpr {
    public static final String NAME = "format-date";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathFormatDateFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFormatDateFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return dateStr(evaluatedArgs[0], evaluatedArgs[1]);
    }

    private static String dateStr(Object od, Object of) {
        Date expandedDate = FunctionUtils.expandDateSafe(od);
        if (expandedDate == null) {
            return "";
        }
        return DateUtils.format(expandedDate, FunctionUtils.toString(of));
    }

}
