package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSubstrFunc extends XPathFuncExpr {
    public static final String NAME = "substr";
    // 2 or 3 arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathSubstrFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSubstrFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() {
        if (!(args.length == 2 || args.length == 3)) {
            throw new XPathArityException(name, "two or three arguments", args.length);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return substring(evaluatedArgs[0], evaluatedArgs[1], args.length == 3 ? evaluatedArgs[2] : null);
    }

    /**
     * Implementation decisions:
     * -Returns the empty string if o1.equals("")
     * -Returns the empty string for any inputs that would
     * cause an IndexOutOfBoundsException on call to Java's substring method,
     * after start and end have been adjusted
     */
    private static String substring(Object o1, Object o2, Object o3) {
        String s = FunctionUtils.toString(o1);

        if (s.length() == 0) {
            return "";
        }

        int start = FunctionUtils.toInt(o2).intValue();

        int len = s.length();

        int end = (o3 != null ? FunctionUtils.toInt(o3).intValue() : len);
        if (start < 0) {
            start = len + start;
        }
        if (end < 0) {
            end = len + end;
        }
        start = Math.min(Math.max(0, start), end);
        end = Math.min(Math.max(0, end), end);

        return ((start <= end && end <= len) ? s.substring(start, end) : "");
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  A substring function.  Finds a specific part of the string (based on a start position and end position).\n"
                + "Return: The substring identified.\n"
                + "Arguments:  Three arguments\n"
                + "\tThe text value in which to find the sub string\n"
                + "\tThe start position in the string.  This is inclusive (so will include that character). The characters are numbered starting at 0.\n"
                + "\tThe end position in the string.  This is exclusive (so will not include that character). The characters are numbered starting at 0.\n"
                + "Syntax: substr(text_value, start_position, end_position)\n"
                + "Example:  For example, you would like to grab just the year from the string \"2012-09-21\". You can use substr(/data/string_date, 0, 4)";

    }
}
