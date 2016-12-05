package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.XPathNodeset;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathConcatFunc extends XPathFuncExpr {
    public static final String NAME = "concat";
    // zero or more arguments
    private static final int EXPECTED_ARG_COUNT = -1;

    public XPathConcatFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathConcatFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (args.length == 1 && evaluatedArgs[0] instanceof XPathNodeset) {
            return XPathJoinFunc.join("", ((XPathNodeset)evaluatedArgs[0]).toArgList());
        } else {
            return XPathJoinFunc.join("", evaluatedArgs);
        }
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Joins multiple strings together.  The arguments can either reference a question or be a directly type string.\n"
                + "Return: Joined string\n"
                + "Arguments:  Multiple arguments (as many as needed) that represent the strings to be joined in order.\n"
                + "Syntax: concat(text1, text2, text3, ...)\n"
                + "Example:  concat(\"Full name: \", /data/first_name, \" \", /data/last_name)";
    }
}
