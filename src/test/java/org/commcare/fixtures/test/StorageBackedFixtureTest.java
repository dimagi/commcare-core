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

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class StorageBackedFixtureTest {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void queryFlatLookup() throws XPathSyntaxException, UnfullfilledRequirementsException, XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/flat-fixture.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('products')/products/product[@id = 'a6d16035b98f6f962a6538bd927cefb3']/name", "CU");
    }
}
