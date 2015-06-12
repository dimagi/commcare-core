package org.javarosa.core.model.data.test;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.javarosa.core.model.data.IntegerData;

public class IntegerDataTests extends TestCase {
    Integer one;
    Integer two;

    private static int NUM_TESTS = 3;

    /* (non-Javadoc)
     * @see j2meunit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        one = new Integer(1);
        two = new Integer(2);
    }

    public IntegerDataTests(String name) {
        super(name);
    }

    public IntegerDataTests() {
        super();
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(new IntegerDataTests("testGetData"));
        suite.addTest(new IntegerDataTests("testSetData"));
        suite.addTest(new IntegerDataTests("testNullData"));

        return suite;
    }

    public void testGetData() {
        IntegerData data = new IntegerData(one);
        assertEquals("IntegerData's getValue returned an incorrect integer", data.getValue(), one);
    }

    public void testSetData() {
        IntegerData data = new IntegerData(one);
        data.setValue(two);

        assertTrue("IntegerData did not set value properly. Maintained old value.", !(data.getValue().equals(one)));
        assertEquals("IntegerData did not properly set value ", data.getValue(), two);

        data.setValue(one);
        assertTrue("IntegerData did not set value properly. Maintained old value.", !(data.getValue().equals(two)));
        assertEquals("IntegerData did not properly reset value ", data.getValue(), one);

    }

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
