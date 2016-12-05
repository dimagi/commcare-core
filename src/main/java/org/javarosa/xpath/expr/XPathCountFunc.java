package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathCountFunc extends XPathFuncExpr {
    public static final String NAME = "count";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathCountFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathCountFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (evaluatedArgs[0] instanceof XPathNodeset) {
            return new Double(((XPathNodeset)evaluatedArgs[0]).size());
        } else {
            throw new XPathTypeMismatchException("not a nodeset");
        }
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Counts the number of items in a group (ex. a repeat group)\n"
                + "Return: Will return a number of items in the group.\n"
                + "Arguments:  The group of items to be counted.\n"
                + "Syntax: count(/data/group)\n"
                + "Example:  This is useful if you have repeats in your form that allow the end user to choose the number of items.  You may want to calculate how many items were chosen and store them in a hidden value. Ex. count(data/repeat_group)";
    }
}
