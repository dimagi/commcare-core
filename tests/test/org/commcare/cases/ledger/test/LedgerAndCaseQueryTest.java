package org.commcare.cases.ledger.test;

import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Hashtable;

/**
 * Test interplay between ledgers and cases.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerAndCaseQueryTest {
    private EvaluationContext evalContext;

    @Before
    public void setUp() throws Exception {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        // load cases that will be referenced by ledgers
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/create_case_for_ledger.xml"), sandbox, true);

        // load ledger data
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox, true);

        // create an evaluation context that has ledger and case instances setup
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(CaseTestUtils.LEDGER_INSTANCE, "ledger");
        instanceRefToId.put(CaseTestUtils.CASE_INSTANCE, "casedb");
        evalContext =
                MockDataUtils.buildContextWithInstances(sandbox, instanceRefToId);
    }

    @Test
    public void ledgerQueriesWithLedgerData() throws XPathSyntaxException {
        // case id 'market_basket' exists, and ledger data has been attached it
        Assert.assertTrue(
                CaseTestUtils.xpathEvalAndCompare(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='edible_stock']/entry[@id='rice']",
                        10.0));
        // Reference valid case but invalid section id
        Assert.assertTrue(
                CaseTestUtils.xpathEvalAndCompare(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='non-existent-section']",
                        ""));
        // case id 'ocean_state_job_lot' doesn't exists, but the ledger data
        // corresponding to it does
        Assert.assertTrue(
                CaseTestUtils.xpathEvalAndCompare(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='ocean_state_job_lot']/section[@section-id='cleaning_stock']/entry[@id='soap']",
                        9.0));
    }

    @Test
    public void ledgerQueriesWithoutReferencedLedgerData() throws XPathSyntaxException {
        // case id 'star_market' exists but no ledger data has been attached to
        // it
        Assert.assertTrue(
                CaseTestUtils.xpathEvalAndCompare(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']", ""));
        Assert.assertTrue(
                CaseTestUtils.xpathEvalAndCompare(evalContext,
                        "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']/section[@section-id='non-existent-section']", ""));
    }

    @Test
    public void fakeLedgerQueriesFailCorrectly() throws XPathSyntaxException {
        // case id 'totally-fake' doesn't exist
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContext,
                "instance('ledger')/ledgerdb/ledger[@entity-id='totally-fake']", ""));
    }

    @Test
    public void ledgerQueriesWithNoLedgerData() throws Exception {
        // case id 'star_market' exists but no ledger data been loaded at all
        EvaluationContext evalContextWithoutLedgers = createContextWithNoLedgers();

        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger[@entity-id='']/section[@section-id='']/entry[@entry-id='']", ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger/section/entry", ""));
    }

    @Test(expected = XPathTypeMismatchException.class)
    public void ledgerQueriesWithBadTemplate() throws Exception {
        // case id 'star_market' exists but no ledger data been loaded at all
        EvaluationContext evalContextWithoutLedgers = createContextWithNoLedgers();
        CaseTestUtils.xpathEval(evalContextWithoutLedgers,
                "instance('ledger')/ledgerdb/ledger[@entity-id='star_market']/not-section[@section-id='']/entry[@entry-id='']");
    }

    private EvaluationContext createContextWithNoLedgers() throws Exception{
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        // load cases that will be referenced by ledgers
        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/create_case_for_ledger.xml"), sandbox, true);

        // create an evaluation context that has ledger and case instances setup
        Hashtable<String, String> instanceRefToId = new Hashtable<>();
        instanceRefToId.put(CaseTestUtils.LEDGER_INSTANCE, "ledger");
        instanceRefToId.put(CaseTestUtils.CASE_INSTANCE, "casedb");
        return MockDataUtils.buildContextWithInstances(sandbox, instanceRefToId);
    }
}
