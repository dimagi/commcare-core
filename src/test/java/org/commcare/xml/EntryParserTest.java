package org.commcare.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteRequestEntry;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.test_utils.ReflectionUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

/**
 * Tests for {@link EntryParser} when parsing {@code <remote-request>} elements.
 */
public class EntryParserTest {

    @Test
    public void testParseRemoteRequest() throws IOException, UnfullfilledRequirementsException,
            InvalidStructureException, XmlPullParserException, IllegalAccessException {
        String xml = "<remote-request>\n"
                + "  <post url=\"https://www.fake.com/claim_patient/\">\n"
                + "    <data key=\"case_id\" ref=\"instance('session')/session/case_id\"/>\n"
                + "    <data key=\"case_id_list\""
                + "         nodeset=\"instance('selected-cases')/session-data/value\""
                + "         exclude=\"count(instance('casedb')/casedb/case[@case_id = current()/.]) = 1\""
                + "         ref=\".\"/>"
                + "  </post>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</remote-request>";
        PostRequest post = getRemoteRequestPost(xml);
        List<QueryData> params = (List<QueryData>)ReflectionUtils.getField(post, "params");
        assertEquals(2, params.size());
        assertEquals("case_id", params.get(0).getKey());
        assertEquals("case_id_list", params.get(1).getKey());

        Hashtable<String, DataInstance> instances = TestInstances.getInstances();
        instances.put(TestInstances.CASEDB, TestInstances.buildCaseDb(ImmutableList.of("123", "456", "789")));
        EvaluationContext evalContext = new EvaluationContext(null, instances);

        Multimap<String, String> evaluatedParams = post.getEvaluatedParams(evalContext, false);
        assertEquals(Arrays.asList("bang"), evaluatedParams.get("case_id"));
        assertFalse(evaluatedParams.containsKey("case_id_list"));

        evaluatedParams = post.getEvaluatedParams(evalContext, true);
        assertEquals(Arrays.asList("bang"), evaluatedParams.get("case_id"));
        assertTrue(evaluatedParams.containsKey("case_id_list"));
    }

    @Test
    public void testParseEntry() throws IOException, UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException {
        String xml = "<entry>\n"
                + "  <form>http://openrosa.org/formdesigner/2f9</form>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</entry>";
        EntryParser parser = ParserTestUtils.buildParser(xml, EntryParser::buildEntryParser);
        FormEntry entry = (FormEntry) parser.parse();
        assertEquals(entry.getXFormNamespace(), "http://openrosa.org/formdesigner/2f9");
        assertNull(entry.getPostRequest());
    }

    @Test
    public void testParseEntryWithPost() throws IOException, UnfullfilledRequirementsException,
            InvalidStructureException, XmlPullParserException {
        String xml = "<entry>\n"
                + "  <form>http://openrosa.org/formdesigner/2f9</form>\n"
                + "  <post url=\"https://www.fake.com/claim_patient/\">\n"
                + "    <data key=\"case_id\" ref=\"instance('session')/session/case_id\"/>\n"
                + "  </post>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</entry>";
        EntryParser parser = ParserTestUtils.buildParser(xml, EntryParser::buildEntryParser);
        FormEntry entry = (FormEntry) parser.parse();
        assertEquals(entry.getXFormNamespace(), "http://openrosa.org/formdesigner/2f9");
        assertEquals(entry.getPostRequest().getUrl().toString(), "https://www.fake.com/claim_patient/");
    }

    private PostRequest getRemoteRequestPost(String xml)
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException {
        EntryParser parser = ParserTestUtils.buildParser(xml, EntryParser::buildRemoteSyncParser);
        RemoteRequestEntry entry = (RemoteRequestEntry)parser.parse();
        return entry.getPostRequest();
    }
}
