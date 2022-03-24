package org.commcare.xml;

import static org.junit.Assert.assertEquals;

import org.commcare.suite.model.QueryData;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Test;

/**
 * Low level tests for query data parsing
 */
public class QueryDataParserTest {

    @Test
    public void testQueryDataParser() throws InvalidStructureException {
        String query = "<data key=\"device_id\" ref=\"instance('session')/session/context/deviceid\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("device_id", queryData.getKey());
    }
}
