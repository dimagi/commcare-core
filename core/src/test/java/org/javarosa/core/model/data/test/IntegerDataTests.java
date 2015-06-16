package org.javarosa.core.model.data.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import org.javarosa.core.model.data.IntegerData;

public class IntegerDataTests {
    static Integer one;
    static Integer two;

    @BeforeClass
    public static void setUp() {
        one = new Integer(1);
        two = new Integer(2);
    }

    @Test
    public void testGetData() {
        IntegerData data = new IntegerData(one);
        assertEquals("IntegerData's getValue returned an incorrect integer", data.getValue(), one);
    }

    @Test
    public void testSetData() {
        IntegerData data = new IntegerData(one);
        data.setValue(two);

        assertTrue("IntegerData did not set value properly. Maintained old value.", !(data.getValue().equals(one)));
        assertEquals("IntegerData did not properly set value ", data.getValue(), two);

        data.setValue(one);
        assertTrue("IntegerData did not set value properly. Maintained old value.", !(data.getValue().equals(two)));
        assertEquals("IntegerData did not properly reset value ", data.getValue(), one);

    }

    @Test
    public void testNullData() {
        boolean exceptionThrown = false;
        IntegerData data = new IntegerData();
        data.setValue(one);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("IntegerData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("IntegerData overwrote existing value on incorrect input", data.getValue().equals(one));
    }
}
