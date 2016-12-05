package org.commcare.test.utilities;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;

public class CaseTestUtils {
    public static final String CASE_INSTANCE = "jr://instance/casedb";
    public static final String LEDGER_INSTANCE = "jr://instance/ledgerdb";
    public static final String FIXTURE_INSTANCE_PRODUCT = "jr://fixture/commtrack:products";

    public static boolean xpathEvalAndCompare(EvaluationContext evalContext,
                                              String input,
                                              Object expectedOutput)
            throws XPathSyntaxException {
        XPathExpression expr;
        expr = XPathParseTool.parseXPath(input);
        Object output = FunctionUtils.unpack(expr.eval(evalContext));
        return expectedOutput.equals(output);
    }

    public static void xpathEvalAndAssert(EvaluationContext evalContext,
                                          String input,
                                          Object expectedOutput)
            throws XPathSyntaxException {
        XPathExpression expr;
        expr = XPathParseTool.parseXPath(input);
        Object output = FunctionUtils.unpack(expr.eval(evalContext));
        Assert.assertEquals("XPath: " + input, expectedOutput, output);
    }

    public static Object xpathEval(EvaluationContext evalContext,
                                   String input)
            throws XPathSyntaxException {
        XPathExpression expr = XPathParseTool.parseXPath(input);
        return FunctionUtils.unpack(expr.eval(evalContext));
    }
}
