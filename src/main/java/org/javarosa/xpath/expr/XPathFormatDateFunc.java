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

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will change the format of a date for display\n"
                + "Return: Returns a string conversion of the provided date.\n"
                + "Arguments:  the date to be converted, and a string describing how it should be formatted.  The syntax for the display format string is below\n"
                + "\t'%Y' = year\n"
                + "\t'%y' = 2 digit year\n"
                + "\t'%m' = 0-padded month\n"
                + "\t'%n' = numeric month\n"
                + "\t'%b' = short text month (Jan, Feb, etc)\n"
                + "\t'%d' = 0-padded day of month\n"
                + "\t'%e' = day of month\n"
                + "\t'%H' = 0-padded hour (24 hour time)\n"
                + "\t'%h' = hour (24 hour time)\n"
                + "\t'%M' = 0-padded minutes\n"
                + "\t'%S' = 0-padded second\n"
                + "\t'%3' = 0-padded milliseconds\n"
                + "\t'%a' = three letter short text day (Sun, Mon, etc)\n"
                + "\t'short' = the date will be formatted based on the user's current language and phone settings\n"
                + "\t'nepali' = displays the date in the Nepali calendar\n"
                + "Syntax: format-date(date_to_convert, display_format_string)\n"
                + "Example:  When you are displaying a date in the display text, its useful to format it in a manner that is readable for your end users (the default is YYYY-MM-DD).  Some examples\n"
                + "\tformat-date(date(/data/my_date), \"%e/%n/%y\") will return a date that looks like D/M/YY\n"
                + "\tformat-date(date(/data/my_date), \"%a, %e %b %Y\") will return a date that looks like Sun, 7 Apr 2012.\n"
                + "\tformat-date(now(), '%y/%n/%e - %H:%M:%S') will return the current date and time in the format YY/M/D - HH:MM:SS\n"
                + "\tformat-date(now(), '%H:%M:%S') will return the current time in the format HH:MM:SS\n";
    }
}
