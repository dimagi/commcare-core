package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathFormatDateWithTimezoneFunc extends XPathFuncExpr {
    public static final String NAME = "format-date-with-tz";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathFormatDateWithTimezoneFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFormatDateWithTimezoneFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
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