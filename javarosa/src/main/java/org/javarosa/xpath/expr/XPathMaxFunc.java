package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathMaxFunc extends XPathFuncExpr {
    public static final String NAME = "max";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathMaxFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathMaxFunc(XPathExpression[] args) throws XPathSyntaxException {
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
        if (evaluatedArgs.length == 1 && evaluatedArgs[0] instanceof XPathNodeset) {
            return max(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            return max(evaluatedArgs);
        }
    }

    /**
     * Identify the largest value from the list of provided values.
     */
    private static Object max(Object[] argVals) {
        double max = Double.MIN_VALUE;
        for (Object argVal : argVals) {
            max = Math.max(max, FunctionUtils.toNumeric(argVal));
        }
        return max;
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Return the maximum value of the passed in values.  These can either be a reference to a group of questions or a direct set of values.\n"
                + "Return: Number that is the maximum.\n"
                + "Arguments:  There are two potential ways this function will work\n"
                + "\tSingle argument that is the group of questions in which to find the maximum\n"
                + "\tMultiple arguments (an unlimited number) in which to find the maximum.\n"
                + "Syntax: max(question_group) or max(value_1, value_2, value_3, ...)\n"
                + "Example:  You can use this when you want to find the maximum number entered in a repeat group.  Ex. max(/data/repeat_group/my_number_question).  Or when you have multiple questions.  Ex. max(/data/question_1, /data/question_2, /data/question_3, /data/question_4).";
    }
}
