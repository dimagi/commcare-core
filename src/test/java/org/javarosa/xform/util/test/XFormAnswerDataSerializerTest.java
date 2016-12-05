package org.javarosa.xform.util.test;

import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.IntegerData;
import org.javarosa.core.model.data.StringData;
import org.javarosa.core.model.data.TimeData;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.util.XFormAnswerDataSerializer;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Note that this is just a start and doesn't cover direct comparisons
 * for most values.
 *
 * @author Clayton Sims
 */
public class XFormAnswerDataSerializerTest {
    static final String stringDataValue = "String Data Value";
    static final Integer integerDataValue = new Integer(5);
    static final Date dateDataValue = new Date();
    static final Date timeDataValue = new Date();

    static StringData stringData;
    static IntegerData integerData;
    static DateData dateData;
    static TimeData timeData;

    static final TreeElement stringElement = new TreeElement();
    static final TreeElement intElement = new TreeElement();
    static final TreeElement dateElement = new TreeElement();
    static final TreeElement timeElement = new TreeElement();

    static XFormAnswerDataSerializer serializer;

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
}
