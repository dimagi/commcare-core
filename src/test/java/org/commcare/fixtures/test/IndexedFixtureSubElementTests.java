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
 * Tests for indexed fixtures around non-indexed parts of their data
 *
 * @author Clayton Sims (csims@dimagi.com)
 */
public class IndexedFixtureSubElementTests {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void queryLookupsOfSubelementsInIndexedFixtures() throws XPathSyntaxException,
            UnfullfilledRequirementsException,
            XmlPullParserException, IOException, InvalidStructureException {

        ParseUtils.parseIntoSandbox(getClass().getResourceAsStream("/fixture_index/indexed-fixture-sub-elements.xml"), sandbox);

        // make sure the fixture is stored in the indexed fixture storage
        assertEquals(4, sandbox.getIndexedFixtureStorage("commtrack:products").getNumRecords());

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[product_data/unique_value = 'three']/name", "Depo-Provera");

        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = '31ab899368d38c2d0207fe80c00fa96c']/product_data/unique_value", "three");

        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = '31ab899368d38c2d0207fe80c00fa96c']/product_data/multiple_data[@lang='hin']", "dp_hin");

        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[product_data/only_present_later='heterogeneous_element']/code", "pd");

        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id='a6d16035b98f6f962a6538bd927cefb3']/product_data/only_present_later", "heterogeneous_element");
    }
}
