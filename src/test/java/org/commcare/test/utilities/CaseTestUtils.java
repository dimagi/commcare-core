package org.commcare.test.utilities;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.FunctionUtils;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.kxml2.kdom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

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


    /**
     * Compares the case db state in a sandbox with the given data
     *
     * @param sandbox     Sandbox with case data
     * @param caseDbState expected state for casedb instance
     * @throws IOException
     */
    public static void compareCaseDbState(MockUserDataSandbox sandbox, InputStream caseDbState)
            throws IOException {
        byte[] parsedDb = serializeCaseInstanceFromSandbox(sandbox);
        Document parsed = XmlComparator.getDocumentFromStream(new ByteArrayInputStream(parsedDb));
        Document loaded = XmlComparator.getDocumentFromStream(caseDbState);

        try {
            XmlComparator.isDOMEqual(parsed, loaded);
        } catch (Exception e) {
            System.out.print(new String(parsedDb));

            //NOTE: The DOM's definitely don't match here, so the strings cannot be the same.
            //The reason we are asserting equality is because the delta between the strings is
            //likely to do a good job of contextualizing where the DOM's don't match.
            Assert.assertEquals("CaseDB output did not match expected structure(" + e.getMessage() + ")",
                    new String(dumpStream(caseDbState)), new String(parsedDb));
        }
    }

    private static byte[] serializeCaseInstanceFromSandbox(MockUserDataSandbox sandbox) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, new TestInstanceInitializer(sandbox));

            s.serialize(new ExternalDataInstance(CaseTestUtils.CASE_INSTANCE, CaseInstanceTreeElement.MODEL_NAME),
                    null);

            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    private static byte[] dumpStream(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamsUtil.writeFromInputToOutput(is, bos);
        return bos.toByteArray();
    }
}
