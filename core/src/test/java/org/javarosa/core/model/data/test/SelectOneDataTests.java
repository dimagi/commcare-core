package org.javarosa.core.model.data.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Vector;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.util.OrderedHashtable;

public class SelectOneDataTests extends TestCase {
    QuestionDef question;

    Selection one;
    Selection two;

    private static int NUM_TESTS = 3;

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        question = new QuestionDef();
        question.setID(57);

        OrderedHashtable oh = new OrderedHashtable();
        Vector v = new Vector();
        for (int i = 0; i < 3; i++) {
            question.addSelectChoice(new SelectChoice("", "Selection" + i, "Selection" + i, false));
        }

        one = new Selection("Selection1");
        one.attachChoice(question);
        two = new Selection("Selection2");
        two.attachChoice(question);
    }

    public SelectOneDataTests(String name) {
        super(name);
    }

    public SelectOneDataTests() {
        super();
    }

    public Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new SelectOneDataTests("testGetData");
        suite.addTest(new SelectOneDataTests("testSetData");
        suite.addTest(new SelectOneDataTests("testNullData");

        return suite;
    }


    public void testGetData() {
        SelectOneData data = new SelectOneData(one);
        assertEquals("SelectOneData's getValue returned an incorrect SelectOne", data.getValue(), one);

    }

    public void testSetData() {
        SelectOneData data = new SelectOneData(one);
        data.setValue(two);

        assertTrue("SelectOneData did not set value properly. Maintained old value.", !(data.getValue().equals(one)));
        assertEquals("SelectOneData did not properly set value ", data.getValue(), two);

        data.setValue(one);
        assertTrue("SelectOneData did not set value properly. Maintained old value.", !(data.getValue().equals(two)));
        assertEquals("SelectOneData did not properly reset value ", data.getValue(), one);

    }

    public void testNullData() {
        boolean exceptionThrown = false;
        SelectOneData data = new SelectOneData();
        data.setValue(one);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("SelectOneData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("SelectOneData overwrote existing value on incorrect input", data.getValue().equals(one));
    }
}
