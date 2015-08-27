package org.commcare.cases.test;

import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CaseTemplateTest {
    private EvaluationContext evalCtx;

    @Before
    public void setUp() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();
        String inputTransactions = "/create_cases_with_parents.xml";
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream(inputTransactions), sandbox);
        evalCtx = MockDataUtils.buildContextWithInstance(sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
    }

    @Test
    public void testRefToData() throws Exception {
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'child_node']/index/parent", "parent_node"));
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
    @Test
    public void testNonSpecRefFails() {
        try {
            CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/anything_can_go_here/this_should_crash", "");
            Assert.assertTrue(false);
        } catch (Exception e) {
        }
        try {
            CaseTestUtils.xpathEvalAndCompare(evalCtx, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/anything_can_go_here/@this_should_crash", "");
            Assert.assertTrue(false);
        } catch (Exception e) {
        }
    }
}
