package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathSumFunc extends XPathFuncExpr {
    public static final String NAME = "sum";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathSumFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSumFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (evaluatedArgs[0] instanceof XPathNodeset) {
            return sum(((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            throw new XPathTypeMismatchException("not a nodeset");
        }
    }

    /**
     * sum the values in a nodeset; each element is coerced to a numeric value
     */
    private static Double sum(Object argVals[]) {
        double sum = 0.0;
        for (Object argVal : argVals) {
            sum += FunctionUtils.toNumeric(argVal);
        }
        return sum;
    }


    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Sum the items in a group (ex. a question in a repeat group)\n"
                + "Return: Will return the sum of all items.\n"
                + "Arguments:  The group of questions to be summed.\n"
                + "Syntax: sum(question_group_to_be_summed)\n"
                + "Example:  This is useful if you have a repeat and need to add up the values entered for one of the questions. Ex.  sum(/data/my_repeat_group/some_number_question).";
    }
}
