package org.javarosa.core.model.data.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import org.javarosa.core.model.data.StringData;

public class StringDataTests {
    static String stringA;
    static String stringB;

    @BeforeClass
    public static void setUp() {
        stringA = "string A";
        stringB = "string B";
    }

    @Test
    public void testGetData() {
        StringData data = new StringData(stringA);
        assertEquals("StringData's getValue returned an incorrect String", data.getValue(), stringA);

    }

    @Test
    public void testSetData() {
        StringData data = new StringData(stringA);
        data.setValue(stringB);

        assertTrue("StringData did not set value properly. Maintained old value.", !(data.getValue().equals(stringA)));
        assertEquals("StringData did not properly set value ", data.getValue(), stringB);

        data.setValue(stringA);
        assertTrue("StringData did not set value properly. Maintained old value.", !(data.getValue().equals(stringB)));
        assertEquals("StringData did not properly reset value ", data.getValue(), stringA);
    }

    @Test
    public void testNullData() {
        boolean exceptionThrown = false;
        StringData data = new StringData();
        data.setValue(stringA);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("StringData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("StringData overwrote existing value on incorrect input", data.getValue().equals(stringA));
    }
}
