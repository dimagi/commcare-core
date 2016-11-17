package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.DataUtil;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard

/**
 * return the number of choices in a multi-select answer
 * (i.e, space-delimited choice values)
 */
public class XPathCountSelectedFunc extends XPathFuncExpr {
    public static final String NAME = "count-selected";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCountSelectedFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCountSelectedFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        Object evalResult = FunctionUtils.unpack(evaluatedArgs[0]);
        if (!(evalResult instanceof String)) {
            throw new XPathTypeMismatchException("count-selected argument was not a select list");
        }

        String s = (String)evalResult;
        return new Double(DataUtil.splitOnSpaces(s).length);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Counts the number of items selected in a multi-selected.\n"
                + "Return: Returns the number of items selected.\n"
                + "Arguments:  The multi-select question  (or a space-separated list of items).\n"
                + "Syntax: count-selected(my_question)\n"
                + "Example:  You may want to check that at least three items were chosen in a multi-select question.  Ex. count-selected(/data/my_question) >= 3";
    }
}
