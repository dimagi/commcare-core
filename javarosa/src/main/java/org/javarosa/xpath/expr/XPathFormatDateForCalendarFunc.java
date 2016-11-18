package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xform.util.CalendarUtils;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathFormatDateForCalendarFunc extends XPathFuncExpr {
    public static final String NAME = "format-date-for-calendar";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathFormatDateForCalendarFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFormatDateForCalendarFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return formatDateForCalendar(evaluatedArgs[0], evaluatedArgs[1]);
    }

    /**
     * Given a date and format, return that date as a string formatted for that calendar
     * Accepted calendars are Ethiopian and Nepali
     *
     * @param dateObject The Object (String, Date, or XPath) to be evaluated into a date
     * @param format     The calendar format (nepali or ethiopian)
     */
    private static String formatDateForCalendar(Object dateObject, Object format) {

        Date date = FunctionUtils.expandDateSafe(dateObject);
        if (date == null) {
            return "";
        }
        if ("ethiopian".equals(format)) {
            return CalendarUtils.ConvertToEthiopian(date);
        } else if ("nepali".equals(format)) {
            return CalendarUtils.convertToNepaliString(date);
        } else {
            throw new XPathUnsupportedException("Unsupported calendar type: " + format);
        }
    }
}
