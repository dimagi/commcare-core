package org.javarosa.core.util;

/**
 * @author Clayton Sims
 * @date May 27, 2009
 */
public class NoLocalizedTextException extends RuntimeException {
    private String keynames;
    private String locale;

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
