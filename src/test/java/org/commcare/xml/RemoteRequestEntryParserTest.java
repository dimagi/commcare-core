package org.commcare.xml;

import static org.junit.Assert.assertTrue;

import org.commcare.suite.model.PostRequest;
import org.commcare.suite.model.RemoteRequestEntry;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.javarosa.xpath.expr.XPathExpression;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Hashtable;

public class RemoteRequestEntryParserTest {

    @Test
    public void testParse() throws IOException, UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException, NoSuchFieldException, IllegalAccessException {
        String query = "<remote-request>\n"
                + "  <post url=\"https://www.fake.com/claim_patient/\">\n"
                + "    <data key=\"case_id\" ref=\"instance('session')/session/data/case_id\"/>\n"
                + "  </post>\n"
                + "  <command id=\"search\">\n"
                + "    <display>\n"
                + "      <text>Search</text>\n"
                + "    </display>\n"
                + "  </command>\n"
                + "</remote-request>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(query.getBytes("UTF-8"));
        KXmlParser parser = ElementParser.instantiateParser(inputStream);
        EntryParser entryParser = EntryParser.buildRemoteSyncParser(parser);
        RemoteRequestEntry entry = (RemoteRequestEntry)entryParser.parse();
        PostRequest post = entry.getPostRequest();
        Field paramsField = PostRequest.class.getDeclaredField("params");
        paramsField.setAccessible(true);
        Hashtable<String, XPathExpression> params = (Hashtable<String, XPathExpression>)paramsField.get(post);
        assertTrue(params.containsKey("case_id"));
    }
}
