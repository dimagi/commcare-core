package org.commcare.xml;

import static org.junit.Assert.assertEquals;

import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteQueryDatum;
import org.commcare.suite.model.Text;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * Low level tests for session parsing
 */
public class SessionDatumParserTest {

    @Test
    public void testSessionDatumParser()
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
        String title = datum.getTitleLocaleId().getArgument();
        List<QueryData> hiddenQueryValues = datum.getHiddenQueryValues();

        assertEquals(1, hiddenQueryValues.size());
        QueryData queryData = hiddenQueryValues.get(0);
        assertEquals("device_id", queryData.getKey());
        assertEquals("locale_id", title);
    }
}
