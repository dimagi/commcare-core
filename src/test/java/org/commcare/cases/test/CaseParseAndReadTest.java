package org.commcare.cases.test;

import static org.commcare.test.utilities.CaseTestUtils.compareCaseDbState;

import org.commcare.cases.instance.CaseInstanceTreeElement;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.test.utilities.TestInstanceInitializer;
import org.commcare.test.utilities.TestProfileConfiguration;
import org.commcare.test.utilities.XmlComparator;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.io.StreamsUtil;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.model.xform.DataModelSerializer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.kxml2.kdom.Document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * Test suite to verify end-to-end parsing of inbound case XML
 * and reading values back from the casedb model
 *
 * @author ctsims
 */
@RunWith(value = Parameterized.class)
public class CaseParseAndReadTest {

    private MockUserDataSandbox sandbox;

    TestProfileConfiguration config;

    @Parameterized.Parameters(name = "{0}")
    public static Collection data() {
        return TestProfileConfiguration.BulkOffOn();
    }

    public CaseParseAndReadTest(TestProfileConfiguration config) {
        this.config = config;
    }

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void testReadCaseDB() throws Exception {
        parseAndCompareCaseDbState("/case_create_basic.xml", "/case_create_basic_output.xml");

        EvaluationContext ec =
                MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name", "case"));
    }

    @Test
    public void testDoubleCreateCaseWithUpdate() throws Exception {
        parseAndCompareCaseDbState("/case_create_overwrite.xml", "/case_create_overwrite_output.xml");
        EvaluationContext ec = MockDataUtils.buildContextWithInstance(this.sandbox, "casedb", CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_name", "case_overwrite"));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_property1", "one"));
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec, "instance('casedb')/casedb/case[@case_id = 'case_one']/case_property2", "property_two"));
    }
    private void parseAndCompareCaseDbState(String inputTransactions,
                                    String caseDbState) throws Exception {
        config.parseIntoSandbox(this.getClass().getResourceAsStream(inputTransactions), sandbox, false);
        compareCaseDbState(sandbox, getClass().getResourceAsStream(caseDbState));
    }
}
