package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathNotFunc extends XPathFuncExpr {
    public static final String NAME = "not";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathNotFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathNotFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return !FunctionUtils.toBoolean(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will evaluate to true if the argument is false.  Otherwise will return false.\n"
                + "Return: Returns a boolean value (true or false)\n"
                + "Arguments:  The value to be converted\n"
                + "Syntax: not(value_to_convert)\n"
                + "Example:  In some situations its easier to write the display or validation condition for when something shouldn't be shown.  You can then pass this to the not function which will reverse it, allowing it to be used as a display condition.  For example, not(/data/is_pregnant = \"yes\" and /data/has_young_children = \"yes\")";
    }
}
