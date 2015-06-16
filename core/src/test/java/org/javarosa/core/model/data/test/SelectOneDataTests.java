package org.javarosa.core.model.data.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.util.Vector;

import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.SelectOneData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.core.util.OrderedHashtable;

public class SelectOneDataTests {
    static QuestionDef question;

    static Selection one;
    static Selection two;

    @BeforeClass
    public static void setUp() {
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

    @Test
    public void testGetData() {
        SelectOneData data = new SelectOneData(one);
        assertEquals("SelectOneData's getValue returned an incorrect SelectOne", data.getValue(), one);

    }

    @Test
    public void testSetData() {
        SelectOneData data = new SelectOneData(one);
        data.setValue(two);

        assertTrue("SelectOneData did not set value properly. Maintained old value.", !(data.getValue().equals(one)));
        assertEquals("SelectOneData did not properly set value ", data.getValue(), two);

        data.setValue(one);
        assertTrue("SelectOneData did not set value properly. Maintained old value.", !(data.getValue().equals(two)));
        assertEquals("SelectOneData did not properly reset value ", data.getValue(), one);

    }

    @Test
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
