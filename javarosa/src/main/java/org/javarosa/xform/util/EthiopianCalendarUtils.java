package org.javarosa.xform.util;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.chrono.EthiopicChronology;
import org.joda.time.chrono.GregorianChronology;

import java.util.Calendar;
import java.util.Date;

public class EthiopianCalendarUtils {

    public static String convertToEthiopianString(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        return ConvertToEthiopian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private static String ConvertToEthiopian(int gregorianYear, int gregorianMonth, int gregorianDay) {
        Chronology chron_eth = EthiopicChronology.getInstance();
        Chronology chron_greg = GregorianChronology.getInstance();
        DateTime jodaDateTime = new DateTime(gregorianYear, gregorianMonth, gregorianDay, 0, 0, 0, chron_greg);
        DateTime dtEthiopic = jodaDateTime.withChronology(chron_eth);
        String[] monthsArray = CalendarUtils.getMonthsArray("ethiopian_months");
        return dtEthiopic.getDayOfMonth() + " "
                + monthsArray[dtEthiopic.getMonthOfYear() - 1] + " "
                + dtEthiopic.getYear();
    }

}
