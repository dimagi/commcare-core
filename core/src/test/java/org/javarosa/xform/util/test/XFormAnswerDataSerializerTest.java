package org.javarosa.xform.util.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

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
public class XFormAnswerDataSerializerTest extends TestCase {
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

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
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

    public XFormAnswerDataSerializerTest(String name) {
        super(name);
    }

    public XFormAnswerDataSerializerTest() {
        super();
    }

    public Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new XFormAnswerDataSerializerTest("testString"));
        suite.addTest(new XFormAnswerDataSerializerTest("testInteger"));
        suite.addTest(new XFormAnswerDataSerializerTest("testDate"));
        suite.addTest(new XFormAnswerDataSerializerTest("testTime"));
        suite.addTest(new XFormAnswerDataSerializerTest("testSelect"));
        return suite;
    }


    public void testString() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer String", serializer.canSerialize(stringElement.getValue()));
        Object answerData = serializer.serializeAnswerData(stringData);
        assertNotNull("Serializer returns Null for valid String Data", answerData);
        assertEquals("Serializer returns incorrect string serialization", answerData, stringDataValue);
    }

    public void testInteger() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Integer", serializer.canSerialize(intElement.getValue()));
        Object answerData = serializer.serializeAnswerData(integerData);
        assertNotNull("Serializer returns Null for valid Integer Data", answerData);
        //assertEquals("Serializer returns incorrect Integer serialization", answerData, integerDataValue);
    }

    public void testDate() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Date", serializer.canSerialize(dateElement.getValue()));
        Object answerData = serializer.serializeAnswerData(dateData);
        assertNotNull("Serializer returns Null for valid Date Data", answerData);
    }

    public void testTime() {
        assertTrue("Serializer Incorrectly Reports Inability to Serializer Time", serializer.canSerialize(timeElement.getValue()));
        Object answerData = serializer.serializeAnswerData(timeData);
        assertNotNull("Serializer returns Null for valid Time Data", answerData);
    }

    public void testSelect() {
        //No select tests yet.
    }
}
