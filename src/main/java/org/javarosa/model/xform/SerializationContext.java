package org.javarosa.model.xform;

/**
 * Provides contextualizing information needed to properly serialize an XForm
 */
public class SerializationContext {

    private String timezone;

    public void setTimezone(String tz) {
        this.timezone = tz;
    }

    public String getTimezone() {
        return timezone;
    }
}
