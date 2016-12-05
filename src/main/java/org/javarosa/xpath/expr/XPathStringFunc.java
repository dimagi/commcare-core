package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathStringFunc extends XPathFuncExpr {
    public static final String NAME = "string";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathStringFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathStringFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.toString(evaluatedArgs[0]);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior: Will convert a value into an equivalent string.\n"
                + "Return: Returns a string based on the passed in argument.\n"
                + "Arguments:  The value to be converted\n"
                + "Syntax: string(value_to_convert)\n"
                + "Example:  If you need to combine some information into a single string (using concatenate for example), you may need to convert some of those values into a string first.  concat(\"You are \", string(/data/age_question), \" years old\").";
    }
}
