package org.javarosa.xpath.expr;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.condition.RequestAbandonedException;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xpath.parser.XPathSyntaxException;

/**
 * Pauses the execution of an expression for a fixed amount of time.
 *
 * Intended to be used to debug and test performance bound expressions without needing
 * to replicate complex situations.
 *
 * If the thread is interrupted, this function throws a RequestAbandonedException, which is the
 * same behavior that would be expected from a cancelled long-running operation elsewhere
 * in xpath
 *
 * @author ctsims
 */
public class XPathSleepFunc extends XPathFuncExpr {
    public static final String NAME = "sleep";
    private static final int EXPECTED_ARG_COUNT = 2;

    public XPathSleepFunc() {
        name = NAME;
        expectedArgCount = EXPECTED_ARG_COUNT;
    }

    public XPathSleepFunc(XPathExpression[] args) throws XPathSyntaxException {
        super(NAME, args, EXPECTED_ARG_COUNT, true);
    }

    @Override
    public Object evalBody(DataInstance model, EvaluationContext evalContext, Object[] evaluatedArgs) {
        int millis =  FunctionUtils.toInt(evaluatedArgs[0]).intValue();

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RequestAbandonedException();
        }
        return evaluatedArgs[1];
    }
}
