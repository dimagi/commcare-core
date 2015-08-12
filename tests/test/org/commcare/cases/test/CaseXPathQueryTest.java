package org.commcare.cases.test;

import org.commcare.cases.CaseTestUtils;
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

    @Test
    public void caseQueryWithNoCaseInstance() throws XPathSyntaxException {
        MockUserDataSandbox emptySandbox = MockDataUtils.getStaticStorage();

        EvaluationContext ec =
                MockDataUtils.getInstanceContexts(emptySandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEval(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name", ""));
    }

    @Test
    public void referenceNonExistentCaseId() {
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_basic.xml"), sandbox);
        EvaluationContext ec =
                MockDataUtils.getInstanceContexts(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);

        // TODO PLM: Should this expression throw an exception because the
        // case_id doesn't exist in the casedb?
        Assert.assertTrue(CaseTestUtils.xpathEval(ec, "instance('casedb')/casedb/case[@case_id = 'no-case']", ""));
    }

    @Test
    public void caseQueryWithBadPath() {
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/case_create_basic.xml"), sandbox);
        EvaluationContext ec =
                MockDataUtils.getInstanceContexts(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEval(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/doesnt_exist", ""));
    }
}
