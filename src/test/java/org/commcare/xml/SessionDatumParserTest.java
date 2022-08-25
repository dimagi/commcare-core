package org.commcare.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.Text;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;
import org.javarosa.xpath.expr.XPathExpression;

import java.io.IOException;
import java.util.List;
import java.util.Hashtable;

/**
 * Low level tests for session parsing
 */
public class SessionDatumParserTest {

    @Test
    public void testSessionDatumParser()
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException {
        String query = "<query url=\"https://www.fake.com/patient_search/\" storage-instance=\"patients\">"
                + "<data key=\"device_id\" ref=\"instance('session')/session/context/deviceid\"/>"
                + "</query>";
        SessionDatumParser parser = ParserTestUtils.buildParser(query, SessionDatumParser.class);
        RemoteQueryDatum datum = (RemoteQueryDatum) parser.parse();
        Hashtable<String, XPathExpression> hiddenQueryValues = datum.getHiddenQueryValues();

        assertEquals(1, hiddenQueryValues.size());
        assertTrue(hiddenQueryValues.containsKey("device_id"));
    }

    @Test
    public void testSessionDatumParser__withTitle()
            throws UnfullfilledRequirementsException, InvalidStructureException, XmlPullParserException,
            IOException {
        String query = "<query url=\"https://www.fake.com/patient_search/\" storage-instance=\"patients\">"
                + "<title>"
                + "<text>"
                + "<locale id=\"locale_id\"/>"
                + "</text>"
                + "</title>"
                + "<data key=\"device_id\" ref=\"instance('session')/session/context/deviceid\"/>"
                + "</query>";
        SessionDatumParser parser = ParserTestUtils.buildParser(query, SessionDatumParser.class);
        RemoteQueryDatum datum = (RemoteQueryDatum) parser.parse();
        String title = datum.getTitleText().getArgument();
        Hashtable<String, XPathExpression> hiddenQueryValues = datum.getHiddenQueryValues();

        assertEquals(1, hiddenQueryValues.size());
        assertTrue(hiddenQueryValues.containsKey("device_id"));
        assertEquals("locale_id", title);
    }
}
