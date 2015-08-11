package org.commcare.cases.ledger.test;

import org.commcare.cases.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

/**
 * Test interplay between ledgers and cases.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerWithCaseTest {
    private EvaluationContext evalContext;

    @Before
    public void setUp() {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        // load cases that will be referenced by ledgers
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/create_case_for_ledger.xml"), sandbox);
        CaseTestUtils.loadCaseInstanceIntoSandbox(sandbox);

        // load ledger data
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox);
        CaseTestUtils.loadLedgerIntoSandbox(sandbox);

        // create an evaluation context that has ledger and case instances setup
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(CaseTestUtils.LEDGER_INSTANCE, "ledger");
        instanceRefToId.put(CaseTestUtils.CASE_INSTANCE, "casedb");
        evalContext =
                MockDataUtils.buildContextWithInstances(sandbox, instanceRefToId);
    }

    @Test
    public void ledgerQueriesWithLedgerData() {
        // case id 'market_basket' exists, and ledger data has been attached to
        // 'market_basket', but the section 'non-existent-section' is
        // non-existent
        Assert.assertTrue(
                CaseTestUtils.xpathEval(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='non-existent-section']",
                        ""));
    }

    @Test
    public void ledgerQueriesWithoutReferencedLedgerData() {
        // case id 'star_market' exists but no ledger data has been attached to
        // it
        Assert.assertTrue(
                CaseTestUtils.xpathEval(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']",
                        ""));
        Assert.assertTrue(
                CaseTestUtils.xpathEval(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']/section[@section-id='non-existent-section']",
                        ""));
    }

    @Test
    public void ledgerQueriesWithNoLedgerData() {
        // case id 'star_market' exists but no ledger data been loaded at all
        EvaluationContext evalContextWithoutLedgers = createContextWithNoLedgers();
        boolean result;
        result = CaseTestUtils.xpathEval(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']",
                "");
        result = CaseTestUtils.xpathEval(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']/section[@section-id='non-existent-section']",
                "");
        System.out.print(result);
        // TODO PLM: Make this actually fail when 'result' is false
    }

    private EvaluationContext createContextWithNoLedgers() {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        // load cases that will be referenced by ledgers
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/create_case_for_ledger.xml"), sandbox);
        CaseTestUtils.loadCaseInstanceIntoSandbox(sandbox);

        CaseTestUtils.loadLedgerIntoSandbox(sandbox);

        // create an evaluation context that has ledger and case instances setup
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(CaseTestUtils.LEDGER_INSTANCE, "ledger");
        instanceRefToId.put(CaseTestUtils.CASE_INSTANCE, "casedb");
        return MockDataUtils.buildContextWithInstances(sandbox, instanceRefToId);
    }
}
