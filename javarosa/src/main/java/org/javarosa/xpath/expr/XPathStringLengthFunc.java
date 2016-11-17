package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathStringLengthFunc extends XPathFuncExpr {
    public static final String NAME = "string-length";
    private static final int EXPECTED_ARG_COUNT = 1;

    public XPathStringLengthFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathStringLengthFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        String s = FunctionUtils.toString(evaluatedArgs[0]);
        if (s == null) {
            return new Double(0.0);
        }
        return new Double(s.length());
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  The number of characters in a string.\n"
                + "Return: A number (characters)\n"
                + "Arguments:  The string for which you need the length.\n"
                + "Syntax: string-length(text_value)\n"
                + "Example:  You may have users entering some identifier (numbers and letters) and you'd like to validate that is of a specific length.  Ex. string-length(/data/my_id_question)";
    }
}
