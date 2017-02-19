package org.commcare.fixtures.test;

import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
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
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/fixture_index/indexed-fixture.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = 'a6d16035b98f6f962a6538bd927cefb3']/name", "CU");

        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[code = 'pd']/name", "CU");

        CaseTestUtils.xpathEvalAndAssert(ec, "count(instance('products')/products/product[non_unique = 'match'])", 3.0);

        // ensure that the entire fixture is stored in the normal storage.
        // This is to ensure if we ever change the indexed data model, we can
        // perform offline data migrations
        assertEquals(1, sandbox.getUserFixtureStorage().getNumRecords());

        // make sure the fixture is stored in the indexed fixture storage
        assertEquals(4, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());
    }

    @Test(expected = InvalidStructureException.class)
    public void errorOnSchemaAfterFixtureTest() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/fixture_index/schema-after-indexed-fixture.xml"), sandbox, true);
    }

    @Test(expected = InvalidStructureException.class)
    public void errorOnMaliciousSchemaTest() throws XPathSyntaxException, UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/fixture_index/malicious-indexed-fixture.xml"), sandbox, true);
    }
}
