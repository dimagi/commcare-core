package org.javarosa.core.model.data.helper;

/**
 * Provides contextualizing information needed to properly resolve or write out a final value (such as in operations
 * like serialization or casting)
 */
public class ValueResolutionContext {

    private int timezoneOffsetMillis = -1;

    public ValueResolutionContext(int offsetInMillis) {
        this.timezoneOffsetMillis = offsetInMillis;
    }

    public int getTimezoneOffset() {
        return timezoneOffsetMillis;
    }
}
