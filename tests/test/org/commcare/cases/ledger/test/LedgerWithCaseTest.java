package org.commcare.cases.ledger.test;

import org.commcare.cases.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test ledger parsing, loading, and xpath expressions that make ledger
 * references
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerWithCaseTest {
    private EvaluationContext evalContextWithLedger;

    @Before
    public void setUp() {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox);

        CaseTestUtils.loadLedgerIntoSandbox(sandbox);
        evalContextWithLedger =
                MockDataUtils.getInstanceContexts(sandbox, "ledger", CaseTestUtils.LEDGER_INSTANCE);
    }

    @Test
    public void queryExistingLedgerPath() {
        Assert.assertTrue(CaseTestUtils.xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='edible_stock']/entry[@id='beans']",
                8.0));
    }

    @Test
    public void ledgerQueriesWithoutLedgerData() {
        MockUserDataSandbox emptySandbox = MockDataUtils.getStaticStorage();

        CaseTestUtils.loadLedgerIntoSandbox(emptySandbox);
        evalContextWithLedger =
                MockDataUtils.getInstanceContexts(emptySandbox, "ledger", CaseTestUtils.LEDGER_INSTANCE);
        boolean result = CaseTestUtils.xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']",
                "");
        System.out.print(result);
    }
}
