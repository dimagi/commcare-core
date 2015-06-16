package org.javarosa.xform.util.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.util.Date;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.util.XFormAnswerDataSerializer;

/**
 * Note that this is just a start and doesn't cover direct comparisons
 * for most values.
 *
 * @author Clayton Sims
 */
public class XFormAnswerDataSerializerTest {
    final String stringDataValue = "String Data Value";
    final Integer integerDataValue = new Integer(5);
    final Date dateDataValue = new Date();
    final Date timeDataValue = new Date();

    StringData stringData;
    IntegerData integerData;
    DateData dateData;
    SelectOneData selectData;
    TimeData timeData;

    TreeElement stringElement = new TreeElement();
    TreeElement intElement = new TreeElement();
    TreeElement dateElement = new TreeElement();
    TreeElement selectElement = new TreeElement();
    TreeElement timeElement = new TreeElement();

    XFormAnswerDataSerializer serializer;

    @BeforeClass
    public static void setUp() {
        stringData = new StringData(stringDataValue);
        stringElement.setValue(stringData);

        integerData = new IntegerData(integerDataValue);
        intElement.setValue(integerData);

        dateData = new DateData(dateDataValue);
        dateElement.setValue(dateData);

        timeData = new TimeData(timeDataValue);
        timeElement.setValue(timeData);

        serializer = new XFormAnswerDataSerializer();
    }

    @Test
    public void testString() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer String", serializer.canSerialize(stringElement.getValue()));
        Object answerData = serializer.serializeAnswerData(stringData);
        assertNotNull("Serializer returns Null for valid String Data", answerData);
        assertEquals("Serializer returns incorrect string serialization", answerData, stringDataValue);
    }

    @Test
    public void testInteger() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Integer", serializer.canSerialize(intElement.getValue()));
        Object answerData = serializer.serializeAnswerData(integerData);
        assertNotNull("Serializer returns Null for valid Integer Data", answerData);
        //assertEquals("Serializer returns incorrect Integer serialization", answerData, integerDataValue);
    }

    @Test
    public void testDate() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Date", serializer.canSerialize(dateElement.getValue()));
        Object answerData = serializer.serializeAnswerData(dateData);
        assertNotNull("Serializer returns Null for valid Date Data", answerData);
    }

    @Test
    public void testTime() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Time", serializer.canSerialize(timeElement.getValue()));
        Object answerData = serializer.serializeAnswerData(timeData);
        assertNotNull("Serializer returns Null for valid Time Data", answerData);
    }

    @Test
    public void testSelect() {
        //No select tests yet.
    }
}
