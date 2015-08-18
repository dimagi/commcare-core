package org.commcare.test.utilities;

import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

public class CaseTestUtils {
    public static final String CASE_INSTANCE = "jr://instance/casedb";
    public static final String LEDGER_INSTANCE = "jr://instance/ledgerdb";

    public static boolean xpathEvalAndCompare(EvaluationContext evalContext,
                                              String input,
                                              Object expectedOutput)
            throws XPathSyntaxException {
        XPathExpression expr;
        expr = XPathParseTool.parseXPath(input);
        Object output = XPathFuncExpr.unpack(expr.eval(evalContext));
        return expectedOutput.equals(output);
    }

    public static void xpathEval(EvaluationContext evalContext,
                                 String input)
            throws XPathSyntaxException {
        XPathExpression expr = XPathParseTool.parseXPath(input);
        XPathFuncExpr.unpack(expr.eval(evalContext));
    }
}
