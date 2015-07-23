package org.javarosa.core.model.data.test;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.util.Date;

import org.javarosa.core.model.data.TimeData;

public class TimeDataTests {
    static Date now;
    static Date minusOneHour;

    @BeforeClass
    public static void setUp() {

        now = new Date();
        minusOneHour = new Date(new Date().getTime() - (1000 * 60));
    }

    @Test
    public void testGetData() {
        TimeData data = new TimeData(now);
        assertEquals("TimeData's getValue returned an incorrect Time", data.getValue(), now);
        Date temp = new Date(now.getTime());
        now.setTime(1234);
        assertEquals("TimeData's getValue was mutated incorrectly", data.getValue(), temp);

        Date rep = (Date)data.getValue();
        rep.setTime(rep.getTime() - 1000);

        assertEquals("TimeData's getValue was mutated incorrectly", data.getValue(), temp);

    }

    @Test
    public void testSetData() {
        TimeData data = new TimeData(now);
        data.setValue(minusOneHour);

        assertTrue("TimeData did not set value properly. Maintained old value.", !(data.getValue().equals(now)));
        assertEquals("TimeData did not properly set value ", data.getValue(), minusOneHour);

        data.setValue(now);
        assertTrue("TimeData did not set value properly. Maintained old value.", !(data.getValue().equals(minusOneHour)));
        assertEquals("TimeData did not properly reset value ", data.getValue(), now);

        Date temp = new Date(now.getTime());
        now.setTime(now.getTime() - 1324);

        assertEquals("TimeData's value was mutated incorrectly", data.getValue(), temp);
    }

    @Test
    public void testNullData() {
        boolean exceptionThrown = false;
        TimeData data = new TimeData();
        data.setValue(now);
        try {
            data.setValue(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue("TimeData failed to throw an exception when setting null data", exceptionThrown);
        assertTrue("TimeData overwrote existing value on incorrect input", data.getValue().equals(now));
    }
}
