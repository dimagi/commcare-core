package org.javarosa.xform.util.test;

import org.javarosa.xform.util.NepaliCalendarUtils;
import org.javarosa.xform.util.UniversalDate;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CalendarTests {
    @Test
    public void testTimesFallOnSameDate() {
        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));

        Calendar nepaliMiddleOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliMiddleOfDayDate.set(2007, Calendar.JULY, 7, 18, 46);

        Calendar nepaliBeginningOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliBeginningOfDayDate.set(2007, Calendar.JULY, 7, 0, 0);

        UniversalDate middleOfDay = NepaliCalendarUtils.fromMillis(nepaliMiddleOfDayDate.getTimeInMillis(), nepaliTimeZone);
        UniversalDate beginningOfDay = NepaliCalendarUtils.fromMillis(nepaliBeginningOfDayDate.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(middleOfDay, beginningOfDay);

        Calendar nepaliEndOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliEndOfDayDate.set(2007, Calendar.JULY, 7, 23, 59, 59);
        UniversalDate endOfDay = NepaliCalendarUtils.fromMillis(nepaliEndOfDayDate.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(endOfDay, beginningOfDay);
    }

    private static void assertSameDate(UniversalDate a, UniversalDate b) {
        assertEquals(a.day, b.day);
        assertEquals(a.month, b.month);
        assertEquals(a.year, b.year);
    }

    @Test
    public void testDateCalcsAreSensitiveToCurrentTimezone() {
        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        DateTimeZone mexicanTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        Calendar nepalCal = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepalCal.set(2007, Calendar.JULY, 7, 18, 46);
        Calendar mexicoCal = Calendar.getInstance(mexicanTimeZone.toTimeZone());
        mexicoCal.set(2007, Calendar.JULY, 7, 18, 46);

        UniversalDate mexicanDate = NepaliCalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        UniversalDate nepaliDate = NepaliCalendarUtils.fromMillis(nepalCal.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(nepaliDate, mexicanDate);
    }

    @Test
    public void testUnpackingDateInDifferentTimezone() {
        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        DateTimeZone mexicanTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        Calendar mexicoCal = Calendar.getInstance(mexicanTimeZone.toTimeZone());
        mexicoCal.set(2007, Calendar.JULY, 7, 18, 46);

        UniversalDate mexicanDate = NepaliCalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        long time = NepaliCalendarUtils.toMillisFromJavaEpoch(mexicanDate.year, mexicanDate.month, mexicanDate.day, mexicanTimeZone);
        UniversalDate rebuiltDateInUsingDifferentTimezone = NepaliCalendarUtils.fromMillis(time, nepaliTimeZone);
        assertSameDate(rebuiltDateInUsingDifferentTimezone, mexicanDate);
    }

    @Test
    public void testBaseDateSerialization() {
        DateTimeZone nycTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"));

        Calendar dayInNewYork = Calendar.getInstance(nycTimeZone.toTimeZone());
        dayInNewYork.set(2007, Calendar.JULY, 7);
        UniversalDate nycTime = NepaliCalendarUtils.fromMillis(dayInNewYork.getTimeInMillis(), nycTimeZone);

        long time = NepaliCalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nycTimeZone);
        UniversalDate unpackedNycTime = NepaliCalendarUtils.fromMillis(time, nycTimeZone);
        assertSameDate(nycTime, unpackedNycTime);

        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        time = NepaliCalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nepaliTimeZone);
        UniversalDate unpackedNepaliTime = NepaliCalendarUtils.fromMillis(time, nepaliTimeZone);
        assertSameDate(nycTime, unpackedNepaliTime);
    }

    @Test
    public void serializeUniversalDateViaMillisTest() {
        // India
        DateTimeZone indiaTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:00"));
        UniversalDate nepaliDate = new UniversalDate(2073, 5, 2, 0);
        long normalizedTime = NepaliCalendarUtils.toMillisFromJavaEpoch(2073, 5, 2, indiaTimeZone);
        Date date = new Date(normalizedTime);
        UniversalDate deserializedNepaliDate = NepaliCalendarUtils.fromMillis(date.getTime(), indiaTimeZone);
        assertSameDate(nepaliDate, deserializedNepaliDate);

        // Boston
        DateTimeZone bostonTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT-04:00"));
        normalizedTime = NepaliCalendarUtils.toMillisFromJavaEpoch(2073, 5, 2, bostonTimeZone);
        date = new Date(normalizedTime);
        deserializedNepaliDate = NepaliCalendarUtils.fromMillis(date.getTime(), bostonTimeZone);
        assertSameDate(nepaliDate, deserializedNepaliDate);
    }
}
