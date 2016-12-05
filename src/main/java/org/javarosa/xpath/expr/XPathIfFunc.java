package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class XPathIfFunc extends XPathFuncExpr {
    public static final String NAME = "if";
    private static final int EXPECTED_ARG_COUNT = 3;

    public XPathIfFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathIfFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    protected void validateArgCount() throws XPathSyntaxException {
        if (args.length != expectedArgCount) {
            String msg = name + "() function requires "
                    + expectedArgCount + " arguments but "
                    + args.length + " are present.";
            throw new XPathSyntaxException(msg);
        }
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        if (FunctionUtils.toBoolean(args[0].eval(model, evalContext))) {
            return args[1].eval(model, evalContext);
        } else {
            return args[2].eval(model, evalContext);
        }
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Behavior:  Can be used to test a condition and return one value if it is true and another if that condition is false.  Behaves like the Excel if function.\n"
                + "Return: Will return either the value of the true or false branch.\n"
                + "Arguments:  The condition, the true value and the false value.\n"
                + "Syntax: if(condition_to_test, value_if_true, value_if_false)\n"
                + "Example:  This function is very useful for complex logic.  Ex. if(/data/mother_is_pregnant = \"yes\" and /data/mother_age > 40, \"High Risk Mother\", \"Normal Mother\"). If you need more complex logic (if a, do this, otherwise if b, do this, otherwise do c), you can nest if statements.  Ex. if(data/mother_is_pregnant = \"yes\", \"Is Pregnant\", if(/data/mother_has_young_children = \"yes\", \"Newborn Child Care\", \"Not Tracked\"))";
    }
}
