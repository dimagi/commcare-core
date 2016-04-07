package org.commcare.suite.model;

/**
 * Represents ?? data requirement in the current session.
 */
public class FormIdDatum extends SessionDatum {
    public FormIdDatum() {
    }

    public FormIdDatum(String value) {
        super("", value);
    }
}
