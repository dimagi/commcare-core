package org.javarosa.xform.util.test;

import org.javarosa.core.model.utils.DateUtils;
import org.javarosa.core.model.utils.TimezoneProvider;
import org.javarosa.core.services.locale.Localization;
import org.javarosa.core.services.locale.TableLocaleSource;
import org.javarosa.xform.util.CalendarUtils;
import org.javarosa.xform.util.UniversalDate;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

/**
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class CalendarTests {

    @Before
    public void configureLocaleForCalendar() {
        Localization.getGlobalLocalizerAdvanced().addAvailableLocale("default");
        Localization.setLocale("default");
        TableLocaleSource localeData = new TableLocaleSource();
        localeData.setLocaleMapping("ethiopian_months",
                "Mäskäräm,T’ïk’ïmt,Hïdar,Tahsas,T’ïr,Yäkatit,Mägabit,Miyaziya,Gïnbot,Säne,Hämle,Nähäse,P’agume");
        localeData.setLocaleMapping("nepali_months",
                "Baishakh,Jestha,Ashadh,Shrawan,Bhadra,Ashwin,Kartik,Mangsir,Poush,Magh,Falgun,Chaitra");
        Localization.getGlobalLocalizerAdvanced().registerLocaleResource("default", localeData);
    }

    @Test
    public void testTimesFallOnSameDate() {
        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));

        Calendar nepaliMiddleOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliMiddleOfDayDate.set(2007, Calendar.JULY, 7, 18, 46);

        Calendar nepaliBeginningOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliBeginningOfDayDate.set(2007, Calendar.JULY, 7, 0, 0);

        UniversalDate middleOfDay = CalendarUtils.fromMillis(nepaliMiddleOfDayDate.getTimeInMillis(),
                nepaliTimeZone);
        UniversalDate beginningOfDay = CalendarUtils.fromMillis(nepaliBeginningOfDayDate.getTimeInMillis(),
                nepaliTimeZone);
        assertSameDate(middleOfDay, beginningOfDay);

        Calendar nepaliEndOfDayDate = Calendar.getInstance(nepaliTimeZone.toTimeZone());
        nepaliEndOfDayDate.set(2007, Calendar.JULY, 7, 23, 59, 59);
        UniversalDate endOfDay = CalendarUtils.fromMillis(nepaliEndOfDayDate.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(endOfDay, beginningOfDay);
    }

    // millis <=> date is different in every timezone
    @Test
    public void testConvertToNepaliString() {
        MockTimeZoneProvider mockTimeZoneProvider = new MockTimeZoneProvider(TimeZone.getTimeZone("Europe/Madrid"));
        DateUtils.setTimezoneProvider(mockTimeZoneProvider);
        DateTimeZone timeZone = DateTimeZone.forOffsetMillis(mockTimeZoneProvider.getTimezone().getRawOffset());
        // this is what Nepali widget uses to calculate the millis from date fields
        long millis = CalendarUtils.toMillisFromJavaEpoch(2081, 6, 16, timeZone);
        String nepaliDateStr = CalendarUtils.convertToNepaliString(new Date(millis), null);
        assertEquals( "16 Ashwin 2081", nepaliDateStr);


        mockTimeZoneProvider.setTimezone(TimeZone.getTimeZone("Asia/Kathmandu"));
        timeZone = DateTimeZone.forOffsetMillis(mockTimeZoneProvider.getTimezone().getRawOffset());
        millis = CalendarUtils.toMillisFromJavaEpoch(2081, 6, 16, timeZone);
        nepaliDateStr = CalendarUtils.convertToNepaliString(new Date(millis), null);
        assertEquals( "16 Ashwin 2081", nepaliDateStr);


        mockTimeZoneProvider.setTimezone(TimeZone.getTimeZone("America/Chicago"));
        timeZone = DateTimeZone.forOffsetMillis(mockTimeZoneProvider.getTimezone().getRawOffset());
        millis = CalendarUtils.toMillisFromJavaEpoch(2081, 6, 16, timeZone);
        nepaliDateStr = CalendarUtils.convertToNepaliString(new Date(millis), null);
        assertEquals( "16 Ashwin 2081", nepaliDateStr);
        DateUtils.resetTimezoneProvider();
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

        UniversalDate mexicanDate = CalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        UniversalDate nepaliDate = CalendarUtils.fromMillis(nepalCal.getTimeInMillis(), nepaliTimeZone);
        assertSameDate(nepaliDate, mexicanDate);
    }

    @Test
    public void testUnpackingDateInDifferentTimezone() {
        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        DateTimeZone mexicanTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT-06:00"));
        Calendar mexicoCal = Calendar.getInstance(mexicanTimeZone.toTimeZone());
        mexicoCal.set(2007, Calendar.JULY, 7, 18, 46);

        UniversalDate mexicanDate = CalendarUtils.fromMillis(mexicoCal.getTimeInMillis(), mexicanTimeZone);
        long time = CalendarUtils.toMillisFromJavaEpoch(mexicanDate.year, mexicanDate.month, mexicanDate.day,
                mexicanTimeZone);
        UniversalDate rebuiltDateInUsingDifferentTimezone = CalendarUtils.fromMillis(time, nepaliTimeZone);
        assertSameDate(rebuiltDateInUsingDifferentTimezone, mexicanDate);
    }

    @Test
    public void testBaseDateSerialization() {
        DateTimeZone nycTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"));

        Calendar dayInNewYork = Calendar.getInstance(nycTimeZone.toTimeZone());
        dayInNewYork.set(2007, Calendar.JULY, 7);
        UniversalDate nycTime = CalendarUtils.fromMillis(dayInNewYork.getTimeInMillis(), nycTimeZone);

        long time = CalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nycTimeZone);
        UniversalDate unpackedNycTime = CalendarUtils.fromMillis(time, nycTimeZone);
        assertSameDate(nycTime, unpackedNycTime);

        DateTimeZone nepaliTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:45"));
        time = CalendarUtils.toMillisFromJavaEpoch(nycTime.year, nycTime.month, nycTime.day, nepaliTimeZone);
        UniversalDate unpackedNepaliTime = CalendarUtils.fromMillis(time, nepaliTimeZone);
        assertSameDate(nycTime, unpackedNepaliTime);
    }

    @Test
    public void serializeUniversalDateViaMillisTest() {
        // India
        DateTimeZone indiaTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT+05:00"));
        UniversalDate nepaliDate = new UniversalDate(2073, 5, 2, 0);
        long normalizedTime = CalendarUtils.toMillisFromJavaEpoch(2073, 5, 2, indiaTimeZone);
        Date date = new Date(normalizedTime);
        UniversalDate deserializedNepaliDate = CalendarUtils.fromMillis(date.getTime(), indiaTimeZone);
        assertSameDate(nepaliDate, deserializedNepaliDate);

        // Boston
        DateTimeZone bostonTimeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("GMT-04:00"));
        normalizedTime = CalendarUtils.toMillisFromJavaEpoch(2073, 5, 2, bostonTimeZone);
        date = new Date(normalizedTime);
        deserializedNepaliDate = CalendarUtils.fromMillis(date.getTime(), bostonTimeZone);
        assertSameDate(nepaliDate, deserializedNepaliDate);
    }

    private  class MockTimeZoneProvider extends TimezoneProvider {

        private TimeZone timeZone;

        public MockTimeZoneProvider(TimeZone timeZone) {
            this.timeZone = timeZone;
        }

        public  void  setTimezone(TimeZone timeZone){
            this.timeZone = timeZone;
        }

        @Override
        public TimeZone getTimezone() {
            return timeZone;
        }
    }
}
