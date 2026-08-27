package org.commcare.util;

import junit.framework.TestCase;

import org.junit.Test;

import java.text.ParseException;
import java.util.TimeZone;

public class DateRangeUtilsTest extends TestCase {

    @Test
    public void testDateConversion() throws ParseException {
        // The conversion passes through UTC, so the day it lands on depends on which side of
        // Greenwich you are. Pin the zone rather than inheriting the runner's.
        assertRoundTrip("UTC");
        assertRoundTrip("America/Chicago");
        assertRoundTrip("Asia/Kolkata");
    }

    private void assertRoundTrip(String timeZoneId) throws ParseException {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
        try {
            String dateRange = "2020-02-15 to 2021-03-18";
            String formattedDateRange = DateRangeUtils.formatDateRangeAnswer(dateRange);
            assertEquals(timeZoneId, "__range__2020-02-15__2021-03-18", formattedDateRange);
            assertEquals(timeZoneId, dateRange, DateRangeUtils.getHumanReadableDateRange(formattedDateRange));
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }
}
