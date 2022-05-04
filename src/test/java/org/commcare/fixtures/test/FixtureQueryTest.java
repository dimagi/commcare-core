package org.commcare.fixtures.test;

import org.commcare.core.parse.ParseUtils;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.XPathTypeMismatchException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Test XPath expressions for fixtures
 *
 * @author Clayton Sims (csims@dimagi.com)
 */
public class FixtureQueryTest {
    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void queryNonHomogenousLookups() throws XPathSyntaxException, UnfullfilledRequirementsException, XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/fixture_create.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "commtrack:products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "count(instance('commtrack:products')/products/product[@heterogenous_attribute = 'present'])", 2.0);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('commtrack:products')/products/@last_sync", "2018-07-27T12:54:11.987997+00:00");
    }

    @Test(expected = XPathTypeMismatchException.class)
    public void queryMissingLookups() throws XPathSyntaxException, UnfullfilledRequirementsException, XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/fixture_create.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "commtrack:products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('commtrack:products')/products/product[@heterogenous_attribute = 'present']/missing_reference", "");
    }

    @Test(expected = XPathTypeMismatchException.class)
    public void queryMissingPredicate() throws XPathSyntaxException, UnfullfilledRequirementsException, XmlPullParserException, IOException, InvalidStructureException {
        ParseUtils.parseIntoSandbox(this.getClass().getResourceAsStream("/fixture_create.xml"), sandbox);

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(sandbox, "commtrack:products", CaseTestUtils.FIXTURE_INSTANCE_PRODUCT);
        CaseTestUtils.xpathEvalAndAssert(ec, "instance('commtrack:products')/products/product[@heterogenous_attribute = 'present'][missing_reference='test']", "");
    }
}
