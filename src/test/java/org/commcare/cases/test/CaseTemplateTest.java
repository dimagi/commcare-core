package org.commcare.cases.test;

import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test CaseDB template checking code.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseTemplateTest {
    private EvaluationContext evalCtx;

    @Before
    public void setUp() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        String inputTransactions = "/create_cases_with_parents.xml";
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream(inputTransactions), sandbox);
        evalCtx = MockDataUtils.buildContextWithInstance(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
    }

    @Test
    public void testRefToData() throws Exception {
        //Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'child_node']/index/parent", "parent_node"));

        CaseTestUtils.xpathEvalAndCompare(evalCtx, "count(instance('casedb')/casedb/case)", "1");
    }

    /**
     * Ensure silent failure of reference that follows casedb instance template spec but doesn't point to existing data
     */
    @Test
    public void testWellTemplatedRefToMissingData() throws Exception {
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/parent", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/anything_can_go_here", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/anything_can_go_here", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/anything_can_go_here/@case_type", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/anything_can_go_here/@relationship", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/attachment/anything_can_go_here", ""));
    }

    /**
     * Ensure reference that doesn't follows casedb instance template spec fails
     */
    @Test(expected = XPathTypeMismatchException.class)
    public void testNonSpecRefFails() throws Exception {
        CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/anything_can_go_here/this_should_crash", "");
    }

    /**
     * Ensure attribute reference that doesn't follows casedb instance template spec fails
     */
    @Test(expected = XPathTypeMismatchException.class)
    public void testNonSpecAttrRefFails() throws Exception {
        CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/anything_can_go_here/@this_should_crash", "");
    }
}
