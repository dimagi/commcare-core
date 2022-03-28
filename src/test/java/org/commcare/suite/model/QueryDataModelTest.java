package org.commcare.suite.model;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;

import org.commcare.xml.QueryDataParser;
import org.javarosa.core.model.utils.test.PersistableSandbox;
import org.javarosa.test_utils.ReflectionUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Serialization tests for QueryData classes
 */
public class QueryDataModelTest {

    private PersistableSandbox mSandbox;


    @Before
    public void setUp() {
        mSandbox = new PersistableSandbox();
    }

    @Test
    public void testSerializeValueQueryData() throws InvalidStructureException, IllegalAccessException {
        QueryData value = QueryDataParser.buildQueryData("key", "ref", null, null);
        checkValueData(value);
    }

    @Test
    public void testSerializeValueQueryData_exclude() throws InvalidStructureException, IllegalAccessException {
        QueryData value = QueryDataParser.buildQueryData("key", "ref", "true()", null);
        checkValueData(value);
    }

    @Test
    public void testSerializeListQueryData() throws InvalidStructureException, IllegalAccessException {
        QueryData value = QueryDataParser.buildQueryData(
                "key", "ref", "true()", "instance('selected-cases')/session-data/value");
        checkListQueryData(value);
    }

    @Test
    public void testSerializeListQueryData_nullExclude() throws InvalidStructureException, IllegalAccessException {
        QueryData value = QueryDataParser.buildQueryData(
                "key", "ref", null, "instance('selected-cases')/session-data/value");
        checkListQueryData(value);
    }

    public <T extends QueryData> void checkValueData(QueryData value) throws IllegalAccessException {
        checkSerialization(ValueQueryData.class, value, ImmutableList.of("ref", "excludeExpr"));
    }

    public <T extends QueryData> void checkListQueryData(QueryData value) throws IllegalAccessException {
        checkSerialization(ListQueryData.class, value, ImmutableList.of("ref", "excludeExpr", "nodeset"));
    }

    public <T extends QueryData> void checkSerialization(Class<T> clazz, QueryData value, List<String> fields)
            throws IllegalAccessException {
        byte[] bytes = mSandbox.serialize(value);
        T deserialized = mSandbox.deserialize(bytes, clazz);
        assertEquals(value.getKey(), deserialized.getKey());
        for (String fieldName : fields){
            assertEquals(
                    ReflectionUtils.getField(value, fieldName),
                    ReflectionUtils.getField(deserialized, fieldName)
            );
        }
    }
}
