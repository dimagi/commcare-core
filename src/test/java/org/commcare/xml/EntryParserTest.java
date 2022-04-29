package org.commcare.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.common.collect.Multimap;

import org.commcare.suite.model.Entry;
import org.commcare.suite.model.FormEntry;
import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteRequestEntry;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.test_utils.ReflectionUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Tests for {@link EntryParser} when parsing {@code <remote-request>} elements.
 */
public class EntryParserTest {

    @Test
    public void testParseRemoteRequest() throws IOException, UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException, NoSuchFieldException, IllegalAccessException {
        String xml = "<remote-request>\n"
                + "  <post url=\"https://www.fake.com/claim_patient/\">\n"
                + "    <data key=\"case_id\" ref=\"instance('session')/session/case_id\"/>\n"
                + "  </post>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</remote-request>";
        PostRequest post = getRemoteRequestPost(xml);
        List<QueryData> params = (List<QueryData>) ReflectionUtils.getField(post, "params");
        assertEquals(1, params.size());
        assertEquals("case_id", params.get(0).getKey());

        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        Multimap<String, String> evaluatedParams = post.getEvaluatedParams(evalContext);
        assertEquals(Arrays.asList("bang"), evaluatedParams.get("case_id"));
    }

    private PostRequest getRemoteRequestPost(String xml)
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException {
        EntryParser parser = ParserTestUtils.buildParser(xml, EntryParser::buildRemoteSyncParser);
        RemoteRequestEntry entry = (RemoteRequestEntry)parser.parse();
        return entry.getPostRequest();
    }
}
