package org.commcare.cases.ledger.test;

import org.commcare.cases.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Test ledger parsing, loading, and referencing ledgers. No case data is
 * present in these tests.
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerParseAndQueryTest {
    private EvaluationContext evalContextWithLedger;

    @Before
    public void setUp() {
        MockUserDataSandbox sandbox = MockDataUtils.getStaticStorage();

        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox);

        evalContextWithLedger =
                MockDataUtils.buildContextWithInstance(sandbox, "ledger", CaseTestUtils.LEDGER_INSTANCE);
    }

    @Test
    public void queryExistingLedgerPath() throws XPathSyntaxException {
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='edible_stock']/entry[@id='beans']",
                8.0));
    }

    @Test
    public void queryMissingLedgerPath() throws XPathSyntaxException {
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']",
                ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='amphibious_stock']",
                ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='cleaning_stock']/entry[@id='bleach']",
                ""));

        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']/section[@section-id='edible_stock']/entry[@id='beans']",
                ""));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='amphibious_stock']/entry[@id='beans']",
                ""));
    }

    @Test(expected = XPathMissingInstanceException.class)
    public void ledgerQueriesWithNoLedgerInstance() throws XPathSyntaxException {
        EvaluationContext emptyEvalContext = new EvaluationContext(null);
        CaseTestUtils.xpathEval(emptyEvalContext, "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']");
    }
}
