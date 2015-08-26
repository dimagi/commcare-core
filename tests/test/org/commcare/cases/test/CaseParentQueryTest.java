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
public class CaseParentQueryTest {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void testReadCaseDB() throws Exception {
        String inputTransactions = "/create_cases_with_parents.xml";
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream(inputTransactions), sandbox);
        EvaluationContext ec =
            MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        boolean result = CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/parent", "");
        System.out.print(result);
        // Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'parent_node']/index/parent", ""));
        // Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'child_node']/index/parent", "parent_node"));
    }
}
