package org.javarosa.core.util;

/**
 * @author Clayton Sims
 */
public class NoLocalizedTextException extends RuntimeException {
    private final String keynames;
    private final String locale;

    public NoLocalizedTextException(String message, String keynames, String locale) {
        super(message);
        this.keynames = keynames;
        this.locale = locale;
    }

    public String getMissingKeyNames() {
        return keynames;
    }

    public String getLocaleMissingKey() {
        return locale;
    }
}
