package org.commcare.util;

import org.commcare.modern.util.Pair;
import org.junit.After;
import org.junit.Test;

import java.text.ParseException;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class DateRangeUtilsTest {

    private static final long FEB_15_2020_UTC_MIDNIGHT = 1581724800000L;

    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    public void testDateConversion() throws ParseException {
        for (String timeZoneId : new String[]{"UTC", "America/New_York", "America/Los_Angeles", "Asia/Kolkata", "Pacific/Kiritimati"}) {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
            String dateRange = "2020-02-15 to 2021-03-18";
            String formattedDateRange = DateRangeUtils.formatDateRangeAnswer(dateRange);
            assertEquals(timeZoneId, "__range__2020-02-15__2021-03-18", formattedDateRange);
            assertEquals(timeZoneId, dateRange, DateRangeUtils.getHumanReadableDateRange(formattedDateRange));
        }
    }

    @Test
    public void testPickerTimesAreUtcMidnight() throws ParseException {
        for (String timeZoneId : new String[]{"UTC", "America/New_York", "Asia/Kolkata"}) {
            TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
            Pair<Long, Long> selection = DateRangeUtils.parseHumanReadableDate("2020-02-15 to 2020-02-15");
            assertEquals(timeZoneId, FEB_15_2020_UTC_MIDNIGHT, (long)selection.first);
            assertEquals(timeZoneId, "2020-02-15", DateRangeUtils.getDateFromTime(FEB_15_2020_UTC_MIDNIGHT));
        }
    }
}
