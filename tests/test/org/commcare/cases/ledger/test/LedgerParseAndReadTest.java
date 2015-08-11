package org.commcare.cases.ledger.test;

import org.commcare.test.utils.TestLedgerInitializer;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.model.xform.DataModelSerializer;
import org.javarosa.xpath.XPathMissingInstanceException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.expr.XPathFuncExpr;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Test ledger parsing, loading, and xpath expressions that make ledger
 * references
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class LedgerParseAndReadTest {
    private static final String LEDGER_INSTANCE = "jr://instance/ledgerdb";
    private MockUserDataSandbox sandbox;
    private EvaluationContext evalContextWithLedger;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();

        MockDataUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/ledger_create_basic.xml"), sandbox);

        loadLedgerIntoSandbox(sandbox);
        evalContextWithLedger =
                MockDataUtils.getInstanceContexts(this.sandbox, "ledger", "jr://instance/ledgerdb");
    }

    private static void loadLedgerIntoSandbox(MockUserDataSandbox sandbox) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataModelSerializer s = new DataModelSerializer(bos, new TestLedgerInitializer(sandbox));

            s.serialize(new ExternalDataInstance(LEDGER_INSTANCE, "ledger"), null);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Test
    public void queryExistingLedgerPath() {
        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='edible_stock']/entry[@id='beans']",
                8.0));
    }

    @Test
    public void queryMissingLedgerPath() {
        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']",
                ""));
        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='amphibious_stock']",
                ""));
        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='cleaning_stock']/entry[@id='bleach']",
                ""));

        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']/section[@section-id='edible_stock']/entry[@id='beans']",
                ""));
        Assert.assertTrue(xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='market_basket']/section[@section-id='amphibious_stock']/entry[@id='beans']",
                ""));
    }

    @Test(expected = XPathMissingInstanceException.class)
    public void ledgerQueriesWithoutLedgerInstance() {
        EvaluationContext emptyEvalContext = new EvaluationContext(null);
        xpathEval(emptyEvalContext, "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']", "");
    }

    @Test
    public void ledgerQueriesWithoutLedgerData() {
        MockUserDataSandbox emptySandbox = MockDataUtils.getStaticStorage();

        loadLedgerIntoSandbox(emptySandbox);
        evalContextWithLedger =
                MockDataUtils.getInstanceContexts(emptySandbox, "ledger", "jr://instance/ledgerdb");
        boolean result = xpathEval(evalContextWithLedger,
                "instance('ledger')/ledgerdb/ledger[@entity-id='H_mart']",
                "");
        System.out.print(result);
    }

    private static boolean xpathEval(EvaluationContext evalContext,
                                     String input,
                                     Object expectedOutput) {
        XPathExpression expr;
        try {
            expr = XPathParseTool.parseXPath(input);
        } catch (XPathSyntaxException e) {
            e.printStackTrace();
            return false;
        }
        Object output;
        try {
            output = XPathFuncExpr.unpack(expr.eval(evalContext));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return expectedOutput.equals(output);
    }
}
