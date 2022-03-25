package org.commcare.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.commcare.suite.model.QueryData;
import org.javarosa.core.model.condition.EvaluationContext;
import org.javarosa.core.model.instance.DataInstance;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

/**
 * Low level tests for query data parsing
 */
public class QueryDataParserTest {

    @Test
    public void testParseValueData() throws InvalidStructureException, XmlPullParserException, IOException {
        String query = "<data key=\"device_id\" ref=\"instance('session')/session/case_id\"/>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("device_id", queryData.getKey());

        EvaluationContext evalContext = new EvaluationContext(null, TestInstances.getInstances());
        assertEquals(Collections.singletonList("bang"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseListData() throws InvalidStructureException, XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\">"
                + "<list nodeset=\"instance('selected-cases')/session-data/value\" exclude=\"count(instance"
                + "('casedb')/casedb/case[@case_id = current()/.]) = 1\" ref=\".\"/>"
                + "</data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("case_id_list", queryData.getKey());

        Hashtable<String, DataInstance> instances = TestInstances.getInstances();
        EvaluationContext evalContext = new EvaluationContext(null, instances);
        assertEquals(Arrays.asList("456", "789"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseListData_noExclude() throws InvalidStructureException, XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\">"
                + "<list nodeset=\"instance('selected-cases')/session-data/value\" ref=\".\"/>"
                + "</data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        QueryData queryData = parser.parse();
        assertEquals("case_id_list", queryData.getKey());

        Hashtable<String, DataInstance> instances = TestInstances.getInstances();
        EvaluationContext evalContext = new EvaluationContext(null, instances);
        assertEquals(Arrays.asList("123", "456", "789"), queryData.getValues(evalContext));
    }

    @Test
    public void testParseQueryData_doubleList() throws XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\">"
                + "<list nodeset=\"instance('selected-cases')/session-data/value\" ref=\".\"/>"
                + "<list nodeset=\"instance('selected-cases')/session-data/value\" ref=\".\"/>"
                + "</data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        try {
            parser.parse();
            fail("Expected InvalidStructureException");
        } catch (InvalidStructureException ignored) {}
    }

    @Test
    public void testParseQueryData_noRef() throws XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\"></data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        try {
            parser.parse();
            fail("Expected InvalidStructureException");
        } catch (InvalidStructureException ignored) {}
    }

    @Test
    public void testParseQueryData_badNesting() throws XmlPullParserException, IOException {
        String query = "<data key=\"case_id_list\">"
                + "<data key=\"device_id\" ref=\"instance('session')/session/case_id\"/>"
                + "</data>";
        QueryDataParser parser = ParserTestUtils.buildParser(query, QueryDataParser.class);
        try {
            parser.parse();
            fail("Expected InvalidStructureException");
        } catch (InvalidStructureException ignored) {}
    }
}
