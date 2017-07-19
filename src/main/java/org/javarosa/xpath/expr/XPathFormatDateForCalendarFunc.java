package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xform.util.CalendarUtils;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathUnsupportedException;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.util.Date;

public class XPathFormatDateForCalendarFunc extends XPathFuncExpr {
    public static final String NAME = "format-date-for-calendar";
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathFormatDateForCalendarFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathFormatDateForCalendarFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length < 2 || args.length > 3) {
            throw new XPathArityException(name, "2 or 3 arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        String formatString = null;
        if(evaluatedArgs.length > 2 ){
            formatString = FunctionUtils.toString(evaluatedArgs[2]);
        }
        return formatDateForCalendar(evaluatedArgs[0], evaluatedArgs[1], formatString);
    }

    /**
     * Given a date and format, return that date as a string formatted for that calendar
     * Accepted calendars are Ethiopian and Nepali
     *
     * @param dateObject The Object (String, Date, or XPath) to be evaluated into a date
     * @param calendar     The calendar system to use (nepali or ethiopian)
     * @param format     An optional format string as used in format-date()
     */
    private static String formatDateForCalendar(Object dateObject, Object calendar, String format) {

        Date date = FunctionUtils.expandDateSafe(dateObject);
        if (date == null) {
            return "";
        }
        if ("ethiopian".equals(calendar)) {
            return CalendarUtils.ConvertToEthiopian(date, format);
        } else if ("nepali".equals(calendar)) {
            return CalendarUtils.convertToNepaliString(date, format);
        } else {
            throw new XPathUnsupportedException("Unsupported calendar type: " + calendar);
        }
    }

}
