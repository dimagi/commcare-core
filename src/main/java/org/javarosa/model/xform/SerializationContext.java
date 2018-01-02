package org.javarosa.model.xform;

/**
 * Provides contextualizing information needed to properly serialize an XForm
 */
public class SerializationContext {

    private int timezoneOffset;

    public void setTimezoneOffset(int offsetInMillis) {
        this.timezoneOffset = offsetInMillis;
    }

    public int getTimezoneOffset() {
        return timezoneOffset;
    }
}
