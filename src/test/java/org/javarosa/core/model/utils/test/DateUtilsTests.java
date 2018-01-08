package org.javarosa.core.model.utils.test;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.model.utils.DateUtils.DateFields;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DateUtilsTests {
    private static Date currentTime;

    @BeforeClass
    public static void setUp() {
        currentTime = new Date();
    }

    /**
     * This test ensures that the Strings returned
     * by the getXMLStringValue function are in
     * the proper XML compliant format.
     */
    @Test
    public void testGetXMLStringValueFormat() {
        String currentDate = DateUtils.getXMLStringValue(currentTime);
        assertEquals("The date string was not of the proper length", currentDate.length(), "YYYY-MM-DD".length());
        assertEquals("The date string does not have proper year formatting", currentDate.indexOf("-"), "YYYY-".indexOf("-"));

        try {
            Integer.parseInt(currentDate.substring(0, 4));
        } catch (NumberFormatException e) {
            fail("The Year value was not a valid integer");
        }

        try {
            Integer.parseInt(currentDate.substring(5, 7));
        } catch (NumberFormatException e) {
            fail("The Month value was not a valid integer");
        }

        try {
            Integer.parseInt(currentDate.substring(8, 10));
        } catch (NumberFormatException e) {
            fail("The Day value was not a valid integer");
        }
    }

    @Test
    public void testTimeParses() {
        // This is all kind of tricky. We need to assume J2ME level compliance, so
        // dates won't ever be assumed to have an intrinsic timezone, they'll be
        // assumed to be in the phone's default timezone

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        testTime("10:00", 1000 * 60 * 60 * 10);
        testTime("10:00Z", 1000 * 60 * 60 * 10);

        testTime("10:00+02", 1000 * 60 * 60 * 8);
        testTime("10:00-02", 1000 * 60 * 60 * 12);

        testTime("10:00+02:30", 1000 * 60 * 6 * 75);
        testTime("10:00-02:30", 1000 * 60 * 6 * 125);

        TimeZone offsetTwoHours = TimeZone.getTimeZone("GMT+02");

        TimeZone.setDefault(offsetTwoHours);

        testTime("10:00", 1000 * 60 * 60 * 10);
        testTime("10:00Z", 1000 * 60 * 60 * 12);

        testTime("10:00+02", 1000 * 60 * 60 * 10);
        testTime("10:00-02", 1000 * 60 * 60 * 14);

        testTime("10:00+02:30", 1000 * 60 * 6 * 95);
        testTime("10:00-02:30", 1000 * 60 * 6 * 145);

        TimeZone offsetMinusTwoHours = TimeZone.getTimeZone("GMT-02");

        TimeZone.setDefault(offsetMinusTwoHours);

        testTime("14:00", 1000 * 60 * 60 * 14);
        testTime("14:00Z", 1000 * 60 * 60 * 12);

        testTime("14:00+02", 1000 * 60 * 60 * 10);
        testTime("14:00-02", 1000 * 60 * 60 * 14);

        testTime("14:00+02:30", 1000 * 60 * 6 * 95);
        testTime("14:00-02:30", 1000 * 60 * 6 * 145);


        TimeZone offsetPlusHalf = TimeZone.getTimeZone("GMT+0230");

        TimeZone.setDefault(offsetPlusHalf);

        testTime("14:00", 1000 * 60 * 6 * 140);
        testTime("14:00Z", 1000 * 60 * 6 * 165);

        testTime("14:00+02", 1000 * 60 * 6 * 145);
        testTime("14:00-02", 1000 * 60 * 6 * 185);

        testTime("14:00+02:30", 1000 * 60 * 6 * 140);
        testTime("14:00-02:30", 1000 * 60 * 6 * 190);

        testTime("14:00+04:00", 1000 * 60 * 6 * 125);

        TimeZone.setDefault(null);
    }

    private void testTime(String in, long test) {
        try {
            Date d = DateUtils.parseTime(in);

            // getTime here should always assume that it's in the UTC context, since that's the
            // only available mode for j2me 1.3 (IE: Dates will always come out flat). We'll
            // simulate that here by offsetting.
            long offset = getOffset();

            long value = d.getTime() + offset;

            assertEquals("Fail: " + in + "(" + TimeZone.getDefault().getDisplayName() + ")", test, value);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error: " + in + e.getMessage());
        }
    }

    private long getOffset() {
        DateFields df = new DateFields();
        Date d = DateUtils.getDate(df);

        return -d.getTime();
    }

    @Test
    public void testParity() {
        testCycle(new Date(1300139579000L));
        testCycle(new Date(0));

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        testCycle(new Date(1300139579000L));
        testCycle(new Date(0));

        TimeZone offsetTwoHours = TimeZone.getTimeZone("GMT+02");

        TimeZone.setDefault(offsetTwoHours);

        testCycle(new Date(1300139579000L));
        testCycle(new Date(0));


        TimeZone offTwoHalf = TimeZone.getTimeZone("GMT+0230");

        TimeZone.setDefault(offTwoHalf);

        testCycle(new Date(1300139579000L));
        testCycle(new Date(0));

        TimeZone offMinTwoHalf = TimeZone.getTimeZone("GMT-0230");

        TimeZone.setDefault(offMinTwoHalf);

        testCycle(new Date(1300139579000L));
        testCycle(new Date(0));
    }

    private void testCycle(Date in) {
        try {
            String formatted = DateUtils.formatDateTime(in, DateUtils.FORMAT_ISO8601);
            Date out = DateUtils.parseDateTime(formatted);
            assertEquals("Fail:", in.getTime(), out.getTime());

        } catch (Exception e) {
            e.printStackTrace();
            fail("Error: " + in + e.getMessage());
        }
    }

    @Test
    public void testFormat() {
        Calendar novFifth2016 = Calendar.getInstance();
        novFifth2016.set(2016, Calendar.NOVEMBER, 5);
        Date novFifthDate = novFifth2016.getTime();
        DateFields novFifth2016Fields = DateUtils.getFields(novFifthDate, null);
        HashMap<String, String> escapesResults = new HashMap<>();
        escapesResults.put("%a", "Sat");
        escapesResults.put("%A", "Saturday");
        escapesResults.put("%b", "Nov");
        escapesResults.put("%B", "November");
        escapesResults.put("%d", "05");
        escapesResults.put("%e", "5");
        escapesResults.put("%w", "6");

        for (String escape : escapesResults.keySet()) {
            String result = escapesResults.get(escape);
            String formatted = DateUtils.format(novFifth2016Fields, escape);
            assertEquals("Fail: '" + escape + "' rendered unexpectedly", result, formatted);
        }


        boolean didFail = false;
        try {
            DateUtils.format(novFifth2016Fields, "%c");
        } catch (RuntimeException e) {
            didFail = true;
        }
        assertTrue(didFail);
    }
    
}
