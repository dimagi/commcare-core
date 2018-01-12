package org.commcare.util.screen;

/**
 * Wrapper for exceptions or errors that occur during CLI usage
 *
 * Created by ctsims on 8/5/2015.
 */
public class CommCareSessionException extends Exception {
    public CommCareSessionException(String message) {
        super(message);
    }

    public CommCareSessionException(String message, Exception e) {
        super(message);
        this.initCause(e);
    }
}
