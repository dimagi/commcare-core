package org.javarosa.core.model.utils;

import java.util.Calendar;
import java.util.TimeZone;

public class DefaultTimeZoneProvider extends TimezoneProvider {
    @Override
    public int getTimezoneOffsetMillis() {
        return Calendar.getInstance().getTimeZone().getRawOffset();
    }

    @Override
    public TimeZone getTimezone() {
        return Calendar.getInstance().getTimeZone();
    }
}
