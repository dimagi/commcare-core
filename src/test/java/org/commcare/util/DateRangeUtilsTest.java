//package org.commcare.util;
//
//import junit.framework.TestCase;
//
//import org.junit.Test;
//
//import java.text.ParseException;
//
//public class DateRangeUtilsTest extends TestCase {
//
//    @Test
//    public void testDateConversion() throws ParseException {
//        String dateRange = "2020-02-15 to 2021-03-18";
//        String formattedDateRange = DateRangeUtils.formatDateRangeAnswer(dateRange);
//        assertEquals("__range__2020-02-15__2021-03-18", formattedDateRange);
//        assertEquals(dateRange, DateRangeUtils.getHumanReadableDateRange(formattedDateRange));
//    }
//}
