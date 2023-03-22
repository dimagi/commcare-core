package org.commcare.cases.util.test;

import static org.commcare.test.utilities.CaseTestUtils.compareCaseDbState;

import org.commcare.core.parse.CaseInstanceXmlTransactionParserFactory;
import org.commcare.core.parse.ParseUtils;
import org.commcare.data.xml.TransactionParserFactory;
import org.commcare.test.utilities.CaseTestUtils;
import org.commcare.util.mocks.MockDataUtils;
import org.commcare.util.mocks.MockUserDataSandbox;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.parser.XPathSyntaxException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class BulkCaseInstanceXmlParserTests {

    private MockUserDataSandbox sandbox;

    @Before
    public void setUp() {
        sandbox = MockDataUtils.getStaticStorage();
    }

    @Test
    public void testValidCaseInstanceXml()
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException, XPathSyntaxException {
        parseXml("case_instance_parse/case_instance_valid.xml");
        compareCaseDbState(sandbox, getClass().getClassLoader().getResourceAsStream("case_instance_parse/case_instance_output.xml"));
        EvaluationContext ec = MockDataUtils.buildContextWithInstance(this.sandbox, "casedb",
                CaseTestUtils.CASE_INSTANCE);
        Assert.assertTrue(CaseTestUtils.xpathEvalAndCompare(ec,
                "instance('casedb')/casedb/case[@case_id = 'f6dff792-2599-4fd3-9e86-c11ef61f0302']/case_name", "tapid papid"));
    }

    private void parseXml(String resourceFilePath)
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException {
        InputStream dataSteam = getClass().getClassLoader().getResourceAsStream(resourceFilePath);
        TransactionParserFactory factory = new CaseInstanceXmlTransactionParserFactory(sandbox, null);
        ParseUtils.parseIntoSandbox(dataSteam, factory, false, false);
        dataSteam.close();
    }
}
