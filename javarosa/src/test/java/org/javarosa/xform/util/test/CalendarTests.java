package org.javarosa.xform.util.test;

import org.javarosa.xform.util.CalendarUtils;
import org.javarosa.xform.util.UniversalDate;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CalendarTests {
    @Test
    public void testTimesFallOnSameDate() {
        TimeZone nepaliTimeZone = TimeZone.getTimeZone("GMT+05:45");

        Calendar nepaliMiddleOfDayDate = Calendar.getInstance(nepaliTimeZone);
        nepaliMiddleOfDayDate.set(2007, Calendar.JULY, 7, 18, 46);

        Calendar nepaliBeginningOfDayDate = Calendar.getInstance(nepaliTimeZone);
        nepaliBeginningOfDayDate.set(2007, Calendar.JULY, 7, 0, 0);

        UniversalDate middleOfDay = CalendarUtils.fromMillis(nepaliMiddleOfDayDate.getTimeInMillis(), nepaliTimeZone);
        UniversalDate beginningOfDay = CalendarUtils.fromMillis(nepaliBeginningOfDayDate.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(middleOfDay, beginningOfDay);

        Calendar nepaliEndOfDayDate = Calendar.getInstance(nepaliTimeZone);
        nepaliEndOfDayDate.set(2007, Calendar.JULY, 7, 23, 59, 59);
        UniversalDate endOfDay = CalendarUtils.fromMillis(nepaliEndOfDayDate.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(endOfDay, beginningOfDay);
    }

    private static void assertSameDate(UniversalDate a, UniversalDate b) {
        assertEquals(a.day, b.day);
        assertEquals(a.month, b.month);
        assertEquals(a.year, b.year);
    }

    @Test
    public void testDateCalcsAreSensitiveToCurrentTimezone() {
        TimeZone nepaliTimeZone = TimeZone.getTimeZone("GMT+05:45");
        TimeZone mexicanTimeZone = TimeZone.getTimeZone("GMT-06:00");
        Calendar nepalCal = Calendar.getInstance(nepaliTimeZone);
        nepalCal.set(2007, Calendar.JULY, 7, 18, 46);
        Calendar mexicoCal = Calendar.getInstance(mexicanTimeZone);
        mexicoCal.set(2007, Calendar.JULY, 7, 18, 46);

        UniversalDate mexicanDate = CalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        UniversalDate nepaliDate = CalendarUtils.fromMillis(nepalCal.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(nepaliDate, mexicanDate);
    }

    @Test
    public void testUnpackingDateInDifferentTimezone() {
        TimeZone nepaliTimeZone = TimeZone.getTimeZone("GMT+05:45");
        TimeZone mexicanTimeZone = TimeZone.getTimeZone("GMT-06:00");
        Calendar mexicoCal = Calendar.getInstance(mexicanTimeZone);
        mexicoCal.set(2007, Calendar.JULY, 7, 18, 46);

        UniversalDate mexicanDate = CalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        long time = CalendarUtils.toMillisFromJavaEpoch(mexicanDate.year, mexicanDate.month, mexicanDate.day, mexicanTimeZone);
        UniversalDate rebuiltDateInUsingDifferentTimezone = CalendarUtils.fromMillis(time, nepaliTimeZone);
        assertSameDate(rebuiltDateInUsingDifferentTimezone, mexicanDate);
    }

    @Test
    public void testBaseDateSerialization() {
        TimeZone nycTimeZone = TimeZone.getTimeZone("America/New_York");

        Calendar dayInNewYork = Calendar.getInstance(nycTimeZone);
        dayInNewYork.set(2007, Calendar.JULY, 7);
        UniversalDate nycTime = CalendarUtils.fromMillis(dayInNewYork.getTimeInMillis(), nycTimeZone);

        long time = CalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nycTimeZone);
        UniversalDate unpackedNycTime = CalendarUtils.fromMillis(time, nycTimeZone);
        assertSameDate(nycTime, unpackedNycTime);

        time = CalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nycTimeZone);
        unpackedNycTime = CalendarUtils.fromMillis(time, nycTimeZone);
        assertSameDate(nycTime, unpackedNycTime);
    }
}
