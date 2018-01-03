package org.javarosa.core.model.data.helper;

/**
 * Provides contextualizing information needed to properly cast an UncastData value
 *
 * Created by amstone326 on 1/3/18.
 */
public class CastingContext {

    private int timezoneOffset = -1;

    public CastingContext(int timezoneOffset) {
        this.timezoneOffset = timezoneOffset;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }
}
