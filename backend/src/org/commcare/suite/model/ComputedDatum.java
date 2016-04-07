package org.commcare.suite.model;

/**
 * Piece of required session data that is acquired via an xpath computation
 *
 * @author Phillip Mates (pmates@dimagi.com)
 */
public class ComputedDatum extends SessionDatum {
    public ComputedDatum() {

    }

    /**
     * @param id    Name used to access the computed data in the session instance
     * @param value XPath expression whose evaluation result is used as the data
     */
    public ComputedDatum(String id, String value) {
        super(id, value);
    }
}
