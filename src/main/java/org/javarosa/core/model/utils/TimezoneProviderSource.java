package org.javarosa.core.model.utils;

/**
 * Created by amstone326 on 1/8/18.
 */
public class TimezoneProviderSource {

    protected TimezoneProvider getProvider() {
        return new TimezoneProvider();
    }

    public int getTimezoneOffsetMillis() {
        return getProvider().getTimezoneOffsetMillis();
    }

}
