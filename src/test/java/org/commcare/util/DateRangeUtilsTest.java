package org.commcare.util;

import junit.framework.TestCase;

import org.junit.Test;

import java.text.ParseException;
import java.util.TimeZone;

public class DateRangeUtilsTest extends TestCase {

    @Test
    public void testDateConversionInMultipleTimezones() throws ParseException {
        String[] timezones = {"UTC", "America/Los_Angeles", "Asia/Kolkata", "Europe/London", "America/New_York"};

        for (String tzId : timezones) {
            TimeZone originalTz = TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(tzId));

                String dateRange = "2020-02-15 to 2021-03-18";
                String formattedDateRange = DateRangeUtils.formatDateRangeAnswer(dateRange);

                System.out.println("Timezone: " + tzId);
                System.out.println("  Expected: __range__2020-02-15__2021-03-18");
                System.out.println("  Actual:   " + formattedDateRange);
                System.out.println("  Match: " + formattedDateRange.equals("__range__2020-02-15__2021-03-18"));

                assertEquals("Failed in timezone " + tzId,
                    "__range__2020-02-15__2021-03-18",
                    formattedDateRange);
                assertEquals("Failed reverse conversion in timezone " + tzId,
                    dateRange,
                    DateRangeUtils.getHumanReadableDateRange(formattedDateRange));
            } finally {
                TimeZone.setDefault(originalTz);
            }
        }
    }
}
