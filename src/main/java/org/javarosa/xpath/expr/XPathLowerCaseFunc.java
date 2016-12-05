package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathLowerCaseFunc extends XPathFuncExpr {
    public static final String NAME = "lower-case";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathLowerCaseFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathLowerCaseFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        return FunctionUtils.normalizeCase(evaluatedArgs[0], false);
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Transforms all letters in a string to their lowercase equivalents.\n"
                + "Return: Updated string\n"
                + "Arguments: The string you want to transform.\n"
                + "Syntax: lower-case(text)\n"
                + "Example: lower-case(\"i AM a Test\") -> \"i am a test\"";
    }
}
