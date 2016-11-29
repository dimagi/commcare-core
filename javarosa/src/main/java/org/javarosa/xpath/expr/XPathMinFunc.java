package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathArityException;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathMinFunc extends XPathFuncExpr {
    public static final String NAME = "min";
    // one or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathMinFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathMinFunc(XPathExpression[] args) throws XPathSyntaxException {
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
            return min(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            return min(evaluatedArgs);
        }
    }

    private static Object min(Object[] argVals) {
        double min = Double.MAX_VALUE;
        for (Object argVal : argVals) {
            min = Math.min(min, FunctionUtils.toNumeric(argVal));
        }
        return min;
    }


    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Return the minimum value of the passed in values.  These can either be a reference to a group of questions or a direct set of values.\n"
                + "Return: Number that is the minimum.\n"
                + "Arguments:  There are two potential ways this function will work\n"
                + "\tSingle argument that is the group of questions in which to find the minimum\n"
                + "\tMultiple arguments (an unlimited number) in which to find the minimum.\n"
                + "Syntax: min(question_group) or min(value_1, value_2, value_3, ...)\n"
                + "Example:  You can use this when you want to find the minimum number entered in a repeat group.  Ex. min(/data/repeat_group/my_number_question).  Or when you have multiple questions.  Ex. min(/data/question_1, /data/question_2, /data/question_3, /data/question_4).";
    }
}
