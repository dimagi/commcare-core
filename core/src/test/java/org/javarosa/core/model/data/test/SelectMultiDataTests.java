package org.javarosa.core.model.data.test;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectMultiData;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SelectMultiDataTests {
    static QuestionDef question;

    static Selection one;
    static Selection two;
    static Selection three;

    static Vector firstTwo;
    static Vector lastTwo;
    static Vector invalid;

    @BeforeClass
    public static void setUp() {

        question = new QuestionDef();

        for (int i = 0; i < 4; i++) {
            question.addSelectChoice(new SelectChoice("", "Selection" + i, "Selection " + i, false));
        }

        one = new Selection("Selection 1");
        one.attachChoice(question);
        two = new Selection("Selection 2");
        two.attachChoice(question);
        three = new Selection("Selection 3");
        three.attachChoice(question);

        firstTwo = new Vector();
        firstTwo.addElement(one);
        firstTwo.addElement(two);

        lastTwo = new Vector();
        lastTwo.addElement(two);
        lastTwo.addElement(three);

        invalid = new Vector();
        invalid.addElement(three);
        invalid.addElement(new Integer(12));
        invalid.addElement(one);
    }

    @Test
    public void testGetData() {
        SelectOneData data = new SelectOneData(one);
        assertEquals("SelectOneData's getValue returned an incorrect SelectOne", data.getValue(), one);
    }

    @Test
    public void testSetData() {
        SelectMultiData data = new SelectMultiData(firstTwo);
        data.setValue(lastTwo);

        assertTrue("SelectMultiData did not set value properly. Maintained old value.", !(data.getValue().equals(firstTwo)));
        assertEquals("SelectMultiData did not properly set value ", data.getValue(), lastTwo);

        data.setValue(firstTwo);
        assertTrue("SelectMultiData did not set value properly. Maintained old value.", !(data.getValue().equals(lastTwo)));
        assertEquals("SelectMultiData did not properly reset value ", data.getValue(), firstTwo);

    }

    @Test
    public void testNullData() {
        boolean exceptionThrown = false;
        SelectMultiData data = new SelectMultiData();
        data.setValue(firstTwo);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("SelectMultiData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("SelectMultiData overwrote existing value on incorrect input", data.getValue().equals(firstTwo));
    }

    @Test
    public void testVectorImmutability() {
        SelectMultiData data = new SelectMultiData(firstTwo);
        Selection[] copy = new Selection[firstTwo.size()];
        firstTwo.copyInto(copy);
        firstTwo.setElementAt(two, 0);
        firstTwo.removeElementAt(1);

        Vector internal = (Vector)data.getValue();

        assertVectorIdentity("External Reference: ", internal, copy);

        data.setValue(lastTwo);
        Vector start = (Vector)data.getValue();

        Selection[] external = new Selection[start.size()];
        start.copyInto(external);

        start.removeElementAt(1);
        start.setElementAt(one, 0);

        assertVectorIdentity("Internal Reference: ", (Vector)data.getValue(), external);
    }

    private void assertVectorIdentity(String messageHeader, Vector v, Selection[] a) {

        assertEquals(messageHeader + "SelectMultiData's internal representation was violated. Vector size changed.", v.size(), a.length);

        for (int i = 0; i < v.size(); ++i) {
            Selection internalValue = (Selection)v.elementAt(i);
            Selection copyValue = a[i];

            assertEquals(messageHeader + "SelectMultiData's internal representation was violated. Element " + i + "changed.", internalValue, copyValue);
        }
    }

    @Test
    public void testBadDataTypes() {
        boolean failure = false;
        SelectMultiData data = new SelectMultiData(firstTwo);
        try {
            data.setValue(invalid);
            data = new SelectMultiData(invalid);
        } catch (Exception e) {
            failure = true;
        }
        assertTrue("SelectMultiData did not throw a proper exception while being set to invalid data.", failure);

        Selection[] values = new Selection[firstTwo.size()];
        firstTwo.copyInto(values);
        assertVectorIdentity("Ensure not overwritten: ", (Vector)data.getValue(), values);
    }
}
