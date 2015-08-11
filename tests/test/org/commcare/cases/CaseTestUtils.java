package org.commcare.cases;

import org.commcare.test.utils.TestInstanceInitializer;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CaseTestUtils {
    public static final String CASE_INSTANCE = "jr://instance/casedb";
    public static final String LEDGER_INSTANCE = "jr://instance/ledgerdb";

    public static byte[] loadCaseInstanceIntoSandbox(MockUserDataSandbox sandbox) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, new TestInstanceInitializer(sandbox));

            s.serialize(new ExternalDataInstance(CASE_INSTANCE, "instance"), null);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public static boolean xpathEval(EvaluationContext evalContext,
                                    String input,
                                    Object expectedOutput) {
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(input);
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            return false;
        }
        Object output;
        try {
            output = XPathFuncExpr.unpack(expr.eval(evalContext));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return expectedOutput.equals(output);
    }

    public static void xpathEvalWithException(EvaluationContext evalContext,
                                              String input)
            throws XPathSyntaxException {
        XPathExpression expr = XPathParseTool.parseXPath(input);
        XPathFuncExpr.unpack(expr.eval(evalContext));
    }
}
