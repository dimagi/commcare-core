package org.commcare.fixtures.test;

import org.commcare.cases.query.QueryContext;
import org.commcare.cases.query.ScopeLimitedReferenceRequestCache;
import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathParseTool;
import org.javarosa.xpath.analysis.TreeReferenceAccumulatingAnalyzer;
import org.javarosa.xpath.expr.XPathExpression;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class IndexedFixtureTests {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void queryIndexedLookup() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/indexed-fixture.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = 'a6d16035b98f6f962a6538bd927cefb3']/name", "CU");

        // ensure that the entire fixture is stored in the normal storage.
        // This is to ensure if we ever change the indexed data model, we can
        // perform offline data migrations
        assertEquals(1, sandbox.getUserFixtureStorage().getNumRecords());

        // make sure the fixture is stored in the indexed fixture storage
        assertEquals(4, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());
    }

    @Test
    public void entriesShouldGetDeleted() throws UnfullfilledRequirementsException, XmlPullParserException,
            IOException, InvalidStructureException, XPathSyntaxException {
        // Parse a fixture and check if all fixture ops are workign as expected
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/indexed-fixture.xml"), sandbox);
        assertEquals(4, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());
        EvaluationContext ec = MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = 'f895be4959f9a8a66f57c340aac461b4']/name", "Collier");

        // Update the fixture with the new fixture with one less entry
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/indexed-fixture-delete.xml"), sandbox);
        assertEquals(3, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());

        // validate that the deleted entry is no longer there
        ec = MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = 'f895be4959f9a8a66f57c340aac461b4']/name", "");

        // Update the fixture with the new fixture with no entries and validate that the records got deleted.
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/indexed-fixture-empty.xml"), sandbox);
        assertEquals(0, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());
    }

    @Test
    public void queryLargeBodyLookup() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/large_body.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "testfixture", "jr://fixture/testfixture");

        CaseTestUtils.xpathEvalAndAssert(ec, "count(instance('testfixture')/test/entry[@type = 'a'][value = 1])", 40.0);
    }


    @Test(expected = InvalidStructureException.class)
    public void errorOnSchemaAfterFixtureTest() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/schema-after-indexed-fixture.xml"), sandbox, true);
    }

    @Test(expected = InvalidStructureException.class)
    public void errorOnMaliciousSchemaTest() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/malicious-indexed-fixture.xml"), sandbox, true);
    }

    @Test
    public void testPartialQueryLoads() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/partial_lookup_load_body.xml"), sandbox);

        doPartialLookupTest();
    }

    @Test
    public void testPartialQueryLoadsInBulkMode() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed_fixture/partial_lookup_load_body_large_scope.xml"), sandbox);
        doPartialLookupTest();
    }

    private void doPartialLookupTest() throws XPathSyntaxException, UnfullfilledRequirementsException{
        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "testfixture", "jr://fixture/testfixture");

        EvaluationContext ecForTest = ec.spawnWithCleanLifecycle();

        QueryContext context = ecForTest.getCurrentQueryContext();
        ScopeLimitedReferenceRequestCache cache = context.getQueryCache(ScopeLimitedReferenceRequestCache.class);

        String exprString = "instance('testfixture')/test/entry[@filter_attribute = 'pass'][true() and filter_one = 'pass']/name";
        XPathExpression expr = XPathParseTool.parseXPath(exprString);

        cache.addTreeReferencesToLimitedScope(new TreeReferenceAccumulatingAnalyzer(ecForTest).accumulate(expr));

        CaseTestUtils.xpathEvalAndAssert(ecForTest, exprString, "succeed");

    }

    @Test
    public void queryInSetLookup() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/indexed-fixture-with-keys.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "sort(join(' ', instance('products')/products/product[selected('a6d16035b98f6f962a6538bd927cefb3 31ab899368d38c2d0207fe80c00fb8c1', @id)]/name))", "CU DIU");
    }
}
