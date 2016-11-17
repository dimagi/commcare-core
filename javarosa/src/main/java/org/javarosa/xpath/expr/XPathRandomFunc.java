package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.core.util.MathUtils;
import org.javarosa.xpath.parser.XPathSyntaxException;

// non-standard
public class XPathRandomFunc extends XPathFuncExpr {
    public static final String NAME = "random";
    private static final int EXPECTED_ARG_COUNT = 0;

    public XPathRandomFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathRandomFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, false);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext) {
        //calculated expressions may be recomputed w/o warning! use with caution!!
        return new Double(MathUtils.getRand().nextDouble());
    }

    @Override
    public String getDocumentation() {
        return getDocHeader()
                + "Return:  Returns a random number between 0.0 (inclusive) and 1.0 (exclusive). For instance: 0.738\n"
                + "Arguments: None\n"
                + "Usage: random()\n"
                + "Example Usage: When you need to generate a random number.  For example, to generate a number between 5 and 23, you can use (random()*(23 - 5)) + 5.  This will be something like 12.43334.  You can convert that to a whole number by using int((random()*(23 - 5)) + 5).  You can also reference questions instead of directly typing numbers.  Ex. int(random()*(/data/high_num - /data/low_num) + /data/low_num).";
    }
}
