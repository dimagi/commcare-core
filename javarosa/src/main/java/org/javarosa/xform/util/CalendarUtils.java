package org.javarosa.xform.util;

import org.commcare.util.ArrayDataSource;
import org.commcare.util.LocaleArrayDataSource;

public class CalendarUtils {
    private static ArrayDataSource arrayDataSource = new LocaleArrayDataSource();

    public static void setArrayDataSource(ArrayDataSource arrayDataSource) {
        CalendarUtils.arrayDataSource = arrayDataSource;
    }

    public static String[] getMonthsArray(String key) {
        return arrayDataSource.getArray(key);
    }
}
