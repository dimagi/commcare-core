package org.commcare.cases.test;

import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test xpath expression evaluation that references the case instance
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseXPathQueryTest {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    /*
    @Test
    public void caseQueryWithNoCaseInstance() throws XPathSyntaxException {
        MockUserDataSandbox emptySandbox = MockDataUtils.getStaticStorage();

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(emptySandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name", ""));
    }
    */

    @Test
    public void referenceNonExistentCaseId() throws Exception {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_basic.xml"), sandbox);
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);

        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'no-case']", ""));
    }

    @Test
    public void caseQueryWithBadPath() throws Exception {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_basic.xml"), sandbox);
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/doesnt_exist", ""));
    }
}
