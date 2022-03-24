package org.commcare.xml;

import static org.junit.Assert.assertEquals;

import org.commcare.suite.model.QueryData;
import org.commcare.suite.model.RemoteQueryDatum;
import org.javarosa.xml.ElementParser;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.junit.Test;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

/**
 * Low level tests for session parsing
 */
public class SessionDatumParserTest {

    @Test
    public void testSessionDatumParser()
            throws IOException, UnfullfilledRequirementsException, InvalidStructureException,
            XmlPullParserException {
        String query = "<query url=\"https://www.fake.com/patient_search/\" storage-instance=\"patients\">"
                + "<data key=\"device_id\" ref=\"instance('session')/session/context/deviceid\"/>"
                + "</query>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(query.getBytes("UTF-8"));
        KXmlParser parser = ElementParser.instantiateParser(inputStream);
        SessionDatumParser sessionDatumParser = new SessionDatumParser(parser);
        RemoteQueryDatum datum = (RemoteQueryDatum) sessionDatumParser.parse();
        List<QueryData> hiddenQueryValues = datum.getHiddenQueryValues();
        assertEquals(1, hiddenQueryValues.size());
        QueryData queryData = hiddenQueryValues.get(0);
        assertEquals("device_id", queryData.getKey());
    }
}
