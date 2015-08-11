package org.commcare.cases.test;

import org.commcare.cases.CaseTestUtils;
import org.commcare.test.utils.XmlComparator;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.kxml2.kdom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Test suite to verify end-to-end parsing of inbound case XML
 * and reading values back from the casedb model
 *
 * @author ctsims
 */
public class CaseParseAndReadTest {

    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test(expected = XPathTypeMismatchException.class)
    public void testCaseQueryWithoutCaseInstance() {
        MockUserDataSandbox emptySandbox = MockDataUtils.getStaticStorage();

        CaseTestUtils.loadCaseInstanceIntoSandbox(emptySandbox);
        EvaluationContext ec = MockDataUtils.getInstanceContexts(emptySandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        try {
            CaseTestUtils.xpathEvalWithException(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name");
        } catch (XPathSyntaxException e) {
            Assert.assertTrue(false);
        }
    }

    @Test
    public void testQueryWithMissingCaseId() {
        CaseTestUtils.loadCaseInstanceIntoSandbox(sandbox);
        EvaluationContext ec = MockDataUtils.getInstanceContexts(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        boolean result = CaseTestUtils.xpathEval(ec, "instance('casedb')/casedb/case[@case_id = 'missing-case']/case_name", "");
        System.out.println(result);
    }

    @Test
    public void testReadCaseDB() throws Exception {
        compareCaseDbState("/case_create_basic.xml", "/case_create_basic_output.xml");
    }

    private void compareCaseDbState(String inputTransactions,
                                    String caseDbState) throws Exception {
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream(inputTransactions), sandbox);

        EvaluationContext ec =
            MockDataUtils.getInstanceContexts(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEval(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name", "case"));

        byte[] parsedDb = CaseTestUtils.loadCaseInstanceIntoSandbox(sandbox);
        Document parsed = XmlComparator.getDocumentFromStream(new ByteArrayInputStream(parsedDb));
        Document loaded = XmlComparator.getDocumentFromStream(this.getClass().getResourceAsStream(caseDbState));

        try {
            XmlComparator.isDOMEqual(parsed, loaded);
        } catch(Exception e) {
            System.out.print(new String(parsedDb));

            //NOTE: The DOM's definitely don't match here, so the strings cannot be the same.
            //The reason we are asserting equality is because the delta between the strings is
            //likely to do a good job of contextualizing where the DOM's don't match.
            Assert.assertEquals("CaseDB output did not match expected structure(" + e.getMessage() + ")", new String(dumpStream(caseDbState)), new String(parsedDb));
        }
    }

    private byte[] dumpStream(String inputResource) throws IOException {
        InputStream is = this.getClass().getResourceAsStream(inputResource);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        StreamsUtil.writeFromInputToOutput(is, bos);

        return bos.toByteArray();
    }
}
